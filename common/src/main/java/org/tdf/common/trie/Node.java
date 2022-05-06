package org.tdf.common.trie;

import com.github.salpadding.rlpstream.Constants;
import com.github.salpadding.rlpstream.Rlp;
import com.github.salpadding.rlpstream.RlpList;
import com.github.salpadding.rlpstream.StreamId;
import org.tdf.common.store.Store;
import org.tdf.common.util.FastByteComparisons;
import org.tdf.common.util.HashUtil;
import org.tdf.common.util.HexBytes;

import java.util.ArrayList;
import java.util.List;

import static org.tdf.common.trie.TrieKey.EMPTY;

/**
 * patricia tree's node inspired by:
 * https://ethfans.org/toya/articles/588
 * https://medium.com/shyft-network-media/understanding-trie-databases-in-ethereum-9f03d2c3325d
 * https://github.com/ethereum/wiki/wiki/Patricia-Tree#optimization
 */
public class Node {
    static final int BRANCH_SIZE = 17;
    // rlp encoded of this node, for serialization
    byte[] rlp;
    private boolean dirty;
    // if hash is not null, resolve rlp encoded from db
    private byte[] hash;
    // for lazy load, read only
    private final Store<byte[], byte[]> readOnlyCache;
    // if node is branch node, the length of children is 17
    // the first 16 element is children, and the 17th element is value
    // if node is extension node or leaf node, the length of children is 2
    // the first element is trie key and the second element is value(leaf node) or child node(extension node)
    private Object[] children;

    private Node(byte[] rlp, boolean dirty, byte[] hash, Store<byte[], byte[]> readOnlyCache, Object[] children) {
        this.rlp = rlp;
        this.dirty = dirty;
        this.hash = hash;
        this.readOnlyCache = readOnlyCache;
        this.children = children;
    }

    static Node fromRootHash(byte[] hash, Store<byte[], byte[]> readOnlyCache) {
        return new Node(null, false, hash, readOnlyCache, null);
    }

    // create root node from database and reference
    static Node fromEncoded(
        byte[] rlp,
        Store<byte[], byte[]> readOnlyCache
    ) {
        long streamId = StreamId.decodeElement(rlp, 0, rlp.length, true);
        if (StreamId.isList(streamId))
            return new Node(rlp, false, null, readOnlyCache, null);
        return new Node(null, false, Rlp.decodeBytes(rlp), readOnlyCache, null);
    }

    static Node fromListIndex(
        RlpList li,
        int idx,
        Store<byte[], byte[]> readOnlyCache
    ) {
        if (li.isListAt(idx)) {
            return new Node(li.rawAt(idx), false, null, readOnlyCache, null);
        }
        return new Node(null, false, li.bytesAt(idx), readOnlyCache, null);
    }

    static Node newBranch() {
        return new Node(null, true, null, null, new Object[BRANCH_SIZE]);
    }

    static Node newLeaf(TrieKey key, byte[] value) {
        return new Node(null, true, null, null, new Object[]{key, value});
    }

    static Node newExtension(TrieKey key, Node child) {
        return new Node(null, true, null, null, new Object[]{key, child});
    }

    private void setDirty() {
        dirty = true;
    }

    // encode and commit root node to store
    // return rlp encoded
    // if commit is call at root node, force hash is set to true
    byte[] commit(
        Store<byte[], byte[]> cache,
        boolean forceHash
    ) {
        // if child node is dirty, the parent node must be dirty also
        if (!dirty) return hash != null ? Rlp.encodeBytes(hash) : rlp;
        Type type = getType();
        switch (type) {
            case LEAF: {
                this.rlp = Rlp.encodeElements(
                    Rlp.encodeBytes(getKey().toPacked(true)),
                    Rlp.encodeBytes(getValue())
                );
                break;
            }
            case EXTENSION: {
                this.rlp = Rlp.encodeElements(
                    Rlp.encodeBytes(getKey().toPacked(false)),
                    getExtension().commit(cache, false)
                );
                break;
            }
            default: {
                List<byte[]> elements = new ArrayList<>(BRANCH_SIZE);
                for (int i = 0; i < BRANCH_SIZE - 1; i++) {
                    Node child = (Node) children[i];
                    if (child == null) {
                        elements.add(Constants.NULL);
                        continue;
                    }
                    elements.add(child.commit(cache, false));
                }
                elements.add(Rlp.encodeBytes(getValue()));
                this.rlp = Rlp.encodeElements(elements);
            }
        }
        dispose(cache);
        dirty = false;
        byte[] raw = rlp;

        // if encoded size is great than or equals to 32, store node to db and return a hash reference
        if (raw.length >= 32 || forceHash) {
            hash = HashUtil.sha3(raw);
            cache.set(hash, raw);
            return Rlp.encodeBytes(hash);
        }
        // clean hash
        return rlp;
    }

    // get actual rlp encoding in the cache
    private void resolve() {
        if (rlp != null || hash == null) return;

        byte[] v = readOnlyCache
            .get(hash);
        if (v == null || !FastByteComparisons.equal(hash, HashUtil.sha3(v))) {
            throw new RuntimeException("rlp encoding not found in cache");
        }
        rlp = v;
    }


    // parse encoded from cache
    private void parse() {
        // has parsed
        if (children != null) return;
        resolve();
        long streamId = StreamId.decodeElement(this.rlp, 0, this.rlp.length, true);
        RlpList rlp = StreamId.asList(this.rlp, streamId, BRANCH_SIZE);
        if (rlp.size() == 2) {
            children = new Object[2];
            byte[] packed = rlp.bytesAt(0);
            TrieKey key = TrieKey.fromPacked(packed);
            children[0] = key;
            boolean terminal = TrieKey.isTerminal(packed);
            if (terminal) {
                children[1] = rlp.bytesAt(1);
                return;
            }
            children[1] = fromListIndex(rlp, 1, readOnlyCache);
            return;
        }
        children = new Object[BRANCH_SIZE];
        for (int i = 0; i < BRANCH_SIZE - 1; i++) {
            if (rlp.isNullAt(i)) continue;
            children[i] = fromListIndex(rlp, i, readOnlyCache);
        }
        byte[] item = rlp.bytesAt(BRANCH_SIZE - 1);
        if (item.length == 0) return;
        children[BRANCH_SIZE - 1] = item;
    }

    // clean key-value in database
    private void dispose(Store<byte[], byte[]> cache) {
        if (cache == null || hash == null) return;
        cache.remove(hash);
        hash = null;
    }

    // wrap o to an extension or leaf node
    private Node newShort(TrieKey key, Object o) {
        // if size of key is zero, no need to wrap child
        if (key.size() == 0 && o instanceof Node) {
            return (Node) o;
        }
        return new Node(null, true, null, null, new Object[]{key, o});
    }

    public Type getType() {
        parse();
        if (children.length == BRANCH_SIZE) return Type.BRANCH;
        return children[1] instanceof Node ? Type.EXTENSION : Type.LEAF;
    }

    // try to set value
    // if old value is override, set as dirty
    private boolean setValue(byte[] value) {
        assertBranchOrLeaf();
        byte[] val = getValue();
        if (val != null && FastByteComparisons.equal(val, value)) {
            return dirty;
        }
        setDirty();
        if (getType() == Type.BRANCH) {
            children[BRANCH_SIZE - 1] = value;
            return dirty;
        }
        children[1] = value;
        return dirty;
    }

    public byte[] getValue() {
        parse();
        assertBranchOrLeaf();
        if (getType() == Type.BRANCH) return (byte[]) children[BRANCH_SIZE - 1];
        return (byte[]) children[1];
    }

    public byte[] get(TrieKey key) {
        parse();
        Type type = getType();
        if (type == Type.BRANCH) {
            if (key.isEmpty()) return getValue();
            Node child = getChild(key.get(0));
            return child == null ? null : child.get(key.shift());
        }
        TrieKey k1 = key.matchAndShift(getKey());
        if (k1 == null) return null;
        if (type == Type.LEAF) return k1.isEmpty() ? getValue() : null;
        return getExtension().get(k1);
    }

    // deep-first scanning
    void traverse(TrieKey init, ScannerAction action) {
        parse();
        Type type = getType();
        if (type == Type.BRANCH) {
            if (!action.scan(init, this)) return;
            for (int i = 0; i < BRANCH_SIZE - 1; i++) {
                if (children[i] == null) continue;
                ((Node) children[i]).traverse(init.concat(TrieKey.single(i)), action);
            }
            return;
        }
        if (type == Type.EXTENSION) {
            TrieKey path = init.concat(getKey());
            if (!action.scan(path, this)) return;
            getExtension().traverse(path, action);
            return;
        }
        // leaf node
        action.scan(init.concat(getKey()), this);
    }

    // for test only
    void insert(TrieKey key, byte[] value) {
        insert(key, value, null);
    }

    // insert a new value, deprecated node will be removed from the cache if cache is not null
    // return true when dirty
    boolean insert(TrieKey key, byte[] value,
                   // cache to dispose converted node
                   Store<byte[], byte[]> cache
    ) {
        parse();
        Type type = getType();
        if (type == Type.BRANCH) {
            return branchInsert(key, value, cache);
        }

        TrieKey current = getKey();
        // by definition, common prefix <= current and common prefix <= key ( <= represents subset of here )
        TrieKey commonPrefix = key.getCommonPrefix(current);

        // current is leaf and current equals to key
        if (type == Type.LEAF && commonPrefix.size() == current.size() && commonPrefix.size() == key.size()) {
            return setValue(value);
        }

        // space is not enough, convert to branch node
        if (commonPrefix.isEmpty()) {
            dispose(cache);
            toBranch();
            branchInsert(key, value, cache);
            setDirty();
            return dirty;
        }

        // convert self to extension node
        if ((type == Type.LEAF && commonPrefix.size() == current.size())) {
            dispose(cache);
            byte[] val = getValue();
            Node newBranch = newBranch();
            children[1] = newBranch;
            newBranch.setValue(val);
            newBranch.branchInsert(key.shift(commonPrefix.size()), value, cache);
            setDirty();
            return dirty;
        }

        // current is extension and common prefix equals to current
        if (type == Type.EXTENSION && commonPrefix.size() == current.size()) {
            // TODO: remove this assertion for the extension must be branch
            getExtension().assertBranch();
            this.dirty |= getExtension().branchInsert(key.shift(commonPrefix.size()), value, cache);
            return dirty;
        }

        setDirty();
        dispose(cache);
        // common prefix is a strict subset of current here
        // common prefix < current => tmp couldn't be empty
        TrieKey tmp = current.shift(commonPrefix.size());

        Object o = children[1];
        Node newBranch = newBranch();
        children[1] = newBranch;
        // reset to common prefix
        children[0] = commonPrefix;

        newBranch.children[tmp.get(0)] = newShort(tmp.shift(), o);

        tmp = key.shift(commonPrefix.size());
        if (tmp.isEmpty()) {
            // tmp is empty => common prefix = key => key < current
            newBranch.children[BRANCH_SIZE - 1] = value;
            return dirty;
        }
        newBranch.children[tmp.get(0)] = newLeaf(tmp.shift(), value);
        return dirty;
    }

    Node delete(TrieKey key) {
        return delete(key, null);
    }

    // delete a key
    // return true when dirty
    // if cache is non-null, deprecated node will be removed from cache
    Node delete(TrieKey key, Store<byte[], byte[]> cache) {
        parse();
        Type type = getType();
        if (type == Type.BRANCH) {
            return branchDelete(key, cache);
        }
        TrieKey k1 = key.matchAndShift(getKey());
        // delete failed
        if (k1 == null) return this;
        if (type == Type.LEAF) {
            if (k1.isEmpty()) {
                dispose(cache);
                children[1] = null;
                // delete value success, notify parent to remove this child
                return null;
            }
            // delete failed, no need to compact
            return this;
        }
        Node child = (Node) children[1];
        child = child.delete(k1, cache);
        children[1] = child;
        if (child == null) {
            dispose(cache);
            // delete child success, notify parent to remove this child
            return null;
        }
        this.dirty |= child.dirty;
        tryCompact();
        return this;
    }

    // insert a new value, deprecated node will be removed from the cache if cache is not null
    // return true when dirty
    private boolean branchInsert(TrieKey key, byte[] value, Store<byte[], byte[]> cache) {
        if (key.isEmpty()) {
            return setValue(value);
        }
        Node child = getChild(key.get(0));
        if (child != null) {
            this.dirty |= child.insert(key.shift(), value, cache);
            return dirty;
        }
        child = newLeaf(key.shift(), value);
        children[key.get(0)] = child;
        setDirty();
        return dirty;
    }

    // delete a key
    // return true when dirty
    // if cache is non-null, deprecated node will be removed from cache
    private Node branchDelete(TrieKey key, Store<byte[], byte[]> cache) {
        if (key.isEmpty()) {
            // delete failed
            if (getValue() == null) return this;
            children[BRANCH_SIZE - 1] = null;
            tryCompact();
            setDirty();
            return this;
        }
        int idx = key.get(0);
        Node child = (Node) children[idx];
        // delete failed, no need to compact
        if (child == null) return this;
        child = child.delete(key.shift(), cache);
        children[idx] = child;
        this.dirty |= child == null || child.dirty;
        tryCompact();
        return this;
    }

    private void tryCompact() {
        Type type = getType();
        if (type == Type.LEAF) return;
        if (type == Type.EXTENSION) {
            extensionCompact();
            return;
        }
        int index = getCompactIndex();
        if (index < 0) return;
        branchCompact(index);
    }

    private Node getChild(int index) {
        assertBranch();
        return (Node) children[index];
    }

    private Node getExtension() {
        assertExtension();
        return (Node) children[1];
    }

    public TrieKey getKey() {
        parse();
        assertNotBranch();
        return (TrieKey) children[0];
    }

    private void setKey(TrieKey key) {
        assertNotBranch();
        children[0] = key;
    }

    private void assertBranchOrLeaf() {
        if (getType() != Type.BRANCH && getType() != Type.LEAF) throw new RuntimeException("not a branch or leaf node");
    }

    private void assertNotBranch() {
        if (getType() == Type.BRANCH) throw new RuntimeException("not a extension or leaf node");
    }

    private void assertBranch() {
        if (getType() != Type.BRANCH) throw new RuntimeException("not a branch node");
    }

    private void assertExtension() {
        if (getType() != Type.EXTENSION) throw new RuntimeException("not an extension node");
    }

    private void assertLeaf() {
        if (getType() != Type.LEAF) throw new RuntimeException("not a leaf node");
    }

    // convert extension or leaf node to branch
    // if node is extension, the key couldn't be empty
    // if node is leaf and key is empty, just move value to new branch
    private void toBranch() {
        TrieKey key = getKey();
        Object o = children[1];
        children = new Object[BRANCH_SIZE];
        if (key.isEmpty() && o instanceof byte[]) {
            children[BRANCH_SIZE - 1] = o;
            return;
        }
        children[key.get(0)] = newShort(key.shift(), o);
    }

    // check the branch node could be compacted
    private int getCompactIndex() {
        int cnt = 0;
        // last non-null children
        int idx = -1;
        for (int i = 0; i < BRANCH_SIZE; i++) {
            if (children[i] == null) continue;
            cnt++;
            if (cnt > 1) return -1;
            idx = i;
        }
        return idx;
    }

    // join extension node and its non-branch child as a compact extension node
    private void extensionCompact() {
        Node n = getExtension();
        if (n.getType() == Type.BRANCH) return;
        children[0] = getKey().concat(n.getKey());
        children[1] = n.children[1];
    }

    // compact single child or single value branch node to a extension or leaf node
    private void branchCompact(int index) {
        Object o = children[index];
        children = new Object[2];
        if (o instanceof byte[]) {
            children[0] = EMPTY;
            children[1] = o;
            return;
        }
        Node n = (Node) o;
        if (n.getType() != Type.BRANCH) {
            // non-branch child could be compressed
            children[0] = TrieKey.single(index).concat((TrieKey) n.children[0]);
            children[1] = n.children[1];
            return;
        }
        children[0] = TrieKey.single(index);
        children[1] = n;
    }

    @Override
    public String toString() {
        if (hash == null) return getType().toString();
        return getType().toString() + " " + HexBytes.encode(hash);
    }

    boolean isDirty() {
        return this.dirty;
    }

    byte[] getHash() {
        return this.hash;
    }

    enum Type {
        BRANCH,
        EXTENSION,
        LEAF
    }
}
