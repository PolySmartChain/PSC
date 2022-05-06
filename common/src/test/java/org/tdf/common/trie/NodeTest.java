package org.tdf.common.trie;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.tdf.common.store.ByteArrayMapStore;
import org.tdf.common.store.Store;
import org.tdf.common.util.BigEndian;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;

@RunWith(JUnit4.class)
public class NodeTest {

    @Test
    public void test1() {
        Node n = Node.newLeaf(TrieKey.fromNormal("test".getBytes()), "test".getBytes());
        Arrays.asList("toaster", "toasting", "slow", "slowly")
            .forEach(x -> n.insert(TrieKey.fromNormal(x.getBytes()), x.getBytes()));

        for (String s : Arrays.asList("toaster", "toasting", "slow", "slowly")
        ) {
            assert Arrays.equals(n.get(TrieKey.fromNormal(s.getBytes())), s.getBytes());
        }
    }

    @Test
    public void test2() {
        Node n = Node.newLeaf(TrieKey.fromNormal("do".getBytes()), "verb".getBytes());
        List<String> li = Arrays.asList("dog", "puppy", "doge", "coin", "horse", "stallion");
        for (int i = 0; i < li.size(); i += 2) {
            String key = li.get(i);
            String val = li.get(i + 1);
            n.insert(TrieKey.fromNormal(key.getBytes(StandardCharsets.US_ASCII)), val.getBytes(StandardCharsets.US_ASCII));
        }
        for (int i = 0; i < li.size(); i += 2) {
            String key = li.get(i);
            String val = li.get(i + 1);
            assert Arrays.equals(n.get(TrieKey.fromNormal(key.getBytes(StandardCharsets.US_ASCII))), val.getBytes(StandardCharsets.US_ASCII));
        }
    }

    @Test
    public void test3() {
        Node n = Node.newLeaf(TrieKey.fromNormal("do".getBytes()), "verb".getBytes());
        List<String> li = Arrays.asList("dog", "puppy", "doge", "coin", "horse", "stallion");
        for (int i = 0; i < li.size(); i += 2) {
            String key = li.get(i);
            String val = li.get(i + 1);
            n.insert(TrieKey.fromNormal(key.getBytes()), val.getBytes());
        }
        n = n.delete(TrieKey.fromNormal("doge".getBytes()));
        assert Arrays.equals(n.get(TrieKey.fromNormal("dog".getBytes())), "puppy".getBytes());
        assert Arrays.equals(n.get(TrieKey.fromNormal("do".getBytes())), "verb".getBytes());
        assert Arrays.equals(n.get(TrieKey.fromNormal("horse".getBytes())), "stallion".getBytes());
        assert n.get(TrieKey.fromNormal("doge".getBytes())) == null;
    }

    @Test
    public void test4() {
        Node n = Node.newLeaf(TrieKey.fromNormal("do".getBytes()), "verb".getBytes());
        List<String> li = Arrays.asList("dog", "puppy", "doge", "coin", "horse", "stallion");
        for (int i = 0; i < li.size(); i += 2) {
            String key = li.get(i);
            String val = li.get(i + 1);
            n.insert(TrieKey.fromNormal(key.getBytes()), val.getBytes());
        }
        n = n.delete(TrieKey.fromNormal("do".getBytes()));
        assert Arrays.equals(n.get(TrieKey.fromNormal("dog".getBytes())), "puppy".getBytes());
        assert Arrays.equals(n.get(TrieKey.fromNormal("doge".getBytes())), "coin".getBytes());
        assert Arrays.equals(n.get(TrieKey.fromNormal("horse".getBytes())), "stallion".getBytes());
        assert n.get(TrieKey.fromNormal("do".getBytes())) == null;
    }

    @Test
    public void test5() {
        Node n = Node.newLeaf(TrieKey.fromNormal("do".getBytes()), "verb".getBytes());
        List<String> li = Arrays.asList("dog", "puppy", "doge", "coin", "horse", "stallion");
        for (int i = 0; i < li.size(); i += 2) {
            String key = li.get(i);
            String val = li.get(i + 1);
            n.insert(TrieKey.fromNormal(key.getBytes(StandardCharsets.US_ASCII)), val.getBytes(StandardCharsets.US_ASCII));
        }
        for (int i = 0; i < li.size(); i += 2) {
            String key = li.get(i);
            String val = li.get(i + 1);
            n = n.delete(TrieKey.fromNormal(key.getBytes()));
        }
        n = n.delete(TrieKey.fromNormal("do".getBytes()));
        assert n == null;
    }

    @Test
    public void test6() {
        Node n = Node.newLeaf(TrieKey.single(3).concat(TrieKey.single(15)).concat(TrieKey.single(0x0e)), "abc".getBytes());
        n.insert(TrieKey.fromNormal(new byte[]{0x3f, 0x3d, 0x4a}), "dog".getBytes());
        n.insert(TrieKey.fromNormal(new byte[]{0x3f, 0x3d, 0x4b}), "dog1".getBytes());
        assert Arrays.equals(n.get(TrieKey.fromNormal(new byte[]{0x3f, 0x3d, 0x4a})), "dog".getBytes());
        assert Arrays.equals(n.get(TrieKey.fromNormal(new byte[]{0x3f, 0x3d, 0x4b})), "dog1".getBytes());

        n = n.delete(TrieKey.fromNormal(new byte[]{0x3f, 0x3d, 0x4b}));
        assert n.get(TrieKey.fromNormal(new byte[]{0x3f, 0x3d, 0x4b})) == null;
        n = n.delete(TrieKey.single(3).concat(TrieKey.single(15)).concat(TrieKey.single(0x0e)));
    }

    @Test
    public void testRadix() {
        RadixNode n = new RadixNode();
        n.insert(TrieKey.single(3).concat(TrieKey.single(15)).concat(TrieKey.single(0x0e)), "abc".getBytes());
        n.insert(TrieKey.fromNormal(new byte[]{0x3f, 0x3d, 0x4a}), "dog".getBytes());
        n.insert(TrieKey.fromNormal(new byte[]{0x3f, 0x3d, 0x4b}), "dog1".getBytes());
        assert Arrays.equals(n.get(TrieKey.fromNormal(new byte[]{0x3f, 0x3d, 0x4a})), "dog".getBytes());
        assert Arrays.equals(n.get(TrieKey.fromNormal(new byte[]{0x3f, 0x3d, 0x4b})), "dog1".getBytes());

        n.delete(TrieKey.fromNormal(new byte[]{0x3f, 0x3d, 0x4b}));
        assert n.get(TrieKey.fromNormal(new byte[]{0x3f, 0x3d, 0x4b})) == null;
        n.delete(TrieKey.single(3).concat(TrieKey.single(15)).concat(TrieKey.single(0x0e)));
    }

    @Test
//    insert n = 1000000 random 32 bytes to trie consume 557 ms
//    delete n = 1000000 random 32 bytes to trie consume 635 ms
    public void test7() {
        boolean performance = false;
        if (!performance) return;
        Node n = Node.newBranch();
        byte[] empty = new byte[0];
        SecureRandom sr = new SecureRandom();
        Set<byte[]> set = new HashSet<>();
        for (int i = 0; i < 1_000_000; i++) {
            byte[] bytes = new byte[32];
            set.add(bytes);
            sr.nextBytes(bytes);
        }
        long start = System.currentTimeMillis();
        set.forEach(x -> n.insert(TrieKey.fromNormal(x), empty));
        long end = System.currentTimeMillis();
        System.out.println("insert 1000,000 random 32 bytes to trie consume " + (end - start) + " ms");
        start = System.currentTimeMillis();
        set.forEach(x -> n.delete(TrieKey.fromNormal(x)));
        end = System.currentTimeMillis();
        System.out.println("delete 1000,000 random 32 bytes to trie consume " + (end - start) + " ms");
    }

    @Test
//    insert n = 50000 random 32 bytes to trie consume 157 ms
//    delete n = 50000 random 32 bytes to trie consume 970 ms
//    gc overflow when n = 100000
    public void test8() {
        boolean performance = false;
        if (!performance) return;
        RadixNode n = new RadixNode();
        byte[] empty = new byte[0];
        SecureRandom sr = new SecureRandom();
        Set<byte[]> set = new HashSet<>();
        for (int i = 0; i < 50_000; i++) {
            byte[] bytes = new byte[32];
            set.add(bytes);
            sr.nextBytes(bytes);
        }
        long start = System.currentTimeMillis();
        set.forEach(x -> n.insert(TrieKey.fromNormal(x), empty));
        long end = System.currentTimeMillis();
        System.out.println("insert 50,000 random 32 bytes to trie consume " + (end - start) + " ms");
        start = System.currentTimeMillis();
        set.forEach(x -> n.delete(TrieKey.fromNormal(x)));
        end = System.currentTimeMillis();
        System.out.println("delete 50,000 random 32 bytes to trie consume " + (end - start) + " ms");
    }

    // https://ethereum.stackexchange.com/questions/268/ethereum-block-architecture
    @Test
    public void test9() {
        Node n = Node.newLeaf(TrieKey.fromNormal(
            Arrays.copyOfRange(BigEndian.encodeInt64(0x0a711355), 4, 8)
        ).shift(), "45.0ETH".getBytes());
        n.insert(TrieKey.fromNormal(
            Arrays.copyOfRange(BigEndian.encodeInt64(0x0a77d337), 4, 8)
        ).shift(), "1.00WEI".getBytes());
        n.insert(TrieKey.fromNormal(
            Arrays.copyOfRange(BigEndian.encodeInt64(0x0a7f9365), 4, 8)
        ).shift(), "1.00WEI".getBytes());
        n.insert(TrieKey.fromNormal(
            Arrays.copyOfRange(BigEndian.encodeInt64(0x0a77d397), 4, 8)
        ).shift(), "1.00WEI".getBytes());
    }

    @Test
    public void test10() {
        Node n = Node.newLeaf(TrieKey.fromNormal("abc".getBytes()), "aaa".getBytes());
        n.insert(TrieKey.fromNormal("abc".getBytes()), "ccc".getBytes());
        assert Arrays.equals(n.get(TrieKey.fromNormal("abc".getBytes())), "ccc".getBytes());
        n.insert(TrieKey.fromNormal("a".getBytes()), "ddd".getBytes());
        assert Arrays.equals(n.get(TrieKey.fromNormal("a".getBytes())), "ddd".getBytes());
        n.insert(TrieKey.fromNormal("a".getBytes()), "eee".getBytes());
        assert Arrays.equals(n.get(TrieKey.fromNormal("a".getBytes())), "eee".getBytes());
        n.delete(TrieKey.fromNormal("abcd".getBytes()));
        assert n.get(TrieKey.fromNormal("abc".getBytes())) != null;
    }

    @Test
    public void test11() {
        Node n = Node.newLeaf(TrieKey.fromNormal("test".getBytes()), "test".getBytes());
        Arrays.asList("toaster", "toasting", "slow", "slowly")
            .forEach(x -> n.insert(TrieKey.fromNormal(x.getBytes()), x.getBytes()));
        Store<byte[], byte[]> s = new ByteArrayMapStore<>();
        byte[] element = n.commit(s, true);
        Node n2 = Node.fromEncoded(element, s);
        for (String s2 : Arrays.asList("toaster", "toasting", "slow", "slowly")
        ) {
            assert Arrays.equals(n2.get(TrieKey.fromNormal(s2.getBytes())), s2.getBytes());
        }
    }

    // basic radix Trie
    public static class RadixNode {
        RadixNode[] children = new RadixNode[16];

        byte[] value;

        void insert(TrieKey key, byte[] value) {
            if (key.isEmpty()) {
                this.value = value;
                return;
            }
            RadixNode child = children[key.get(0)];
            if (child != null) {
                child.insert(key.shift(), value);
                return;
            }
            child = new RadixNode();
            children[key.get(0)] = child;
            child.insert(key.shift(), value);
        }

        RadixNode delete(TrieKey key) {
            if (key.isEmpty()) {
                value = null;
                if (isNull()) return null;
                return this;
            }
            RadixNode child = children[key.get(0)];
            if (child == null) return this;
            children[key.get(0)] = child.delete(key.shift());
            if (isNull()) return null;
            return this;
        }

        boolean isNull() {
            return Arrays.stream(children).allMatch(Objects::isNull) && value == null;
        }

        byte[] get(TrieKey key) {
            if (key.isEmpty()) return value;
            RadixNode child = children[key.get(0)];
            if (child == null) return null;
            return child.get(key.shift());
        }
    }
}
