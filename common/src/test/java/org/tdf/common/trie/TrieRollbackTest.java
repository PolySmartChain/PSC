package org.tdf.common.trie;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.tdf.common.TrieUtil;
import org.tdf.common.serialize.Codecs;
import org.tdf.common.store.ByteArrayMapStore;
import org.tdf.common.store.NoDeleteStore;
import org.tdf.common.store.NoDoubleDeleteStore;
import org.tdf.common.store.Store;
import org.tdf.common.util.ByteArrayMap;
import org.tdf.common.util.ByteUtil;
import org.tdf.common.util.HexBytes;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(JUnit4.class)
public class TrieRollbackTest {
    protected Store<byte[], byte[]> removed;
    protected Store<byte[], byte[]> delegate;
    protected Store<byte[], byte[]> database;
    protected Trie<String, String> trie;
    protected List<HexBytes> roots;
    protected Map<String, Map<String, String>> dumps;
    protected List<Map<HexBytes, HexBytes>> nodes;
    private NoDeleteStore<byte[], byte[]> noDelete;


    @Before
    public void before() throws Exception {
        removed = new ByteArrayMapStore<>();

        delegate = new ByteArrayMapStore<>();

        noDelete = new NoDeleteStore<>(delegate, ByteUtil::isNullOrZeroArray);
        database = new NoDoubleDeleteStore<>(noDelete, ByteUtil::isNullOrZeroArray);

        trie = TrieUtil.<String, String>builder()
            .store(database)
            .keyCodec(Codecs.string)
            .valueCodec(Codecs.string)
            .build();

        roots = new ArrayList<>();

        dumps = new HashMap<>();

        nodes = new ArrayList<>();

        URL massiveUpload_1 = ClassLoader
            .getSystemResource("trie/massive-upload.dmp");

        File file = new File(massiveUpload_1.toURI());
        List<String> strData = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);


        for (int i = 0; i < 100; ++i) {

            String[] keyVal = strData.get(i).split("=");

            if (keyVal[0].equals("*"))
                trie.remove(keyVal[1].trim());
            else
                trie.set(keyVal[0].trim(), keyVal[1].trim());

            HexBytes rootHash = trie.commit();

            // skip when trie is not modified
            if (roots.stream().anyMatch(x -> x.equals(rootHash))) continue;

            trie.flush();
            roots.add(rootHash);
            nodes.add(trie.dump());
            dumps.put(rootHash.getHex(), dump(trie));
        }
    }

    @Test
    public void empty() {
    }

    // rollback successful
    @Test
    public void test1() {
        for (HexBytes rootHash : roots) {
            trie = trie.revert(rootHash, database);
            assert equals(dump(trie), dumps.get(rootHash.getHex()));
        }
        for (int i = 0; i < roots.size() - 1; i++) {
            trie = trie.revert(roots.get(i), database);
        }
    }

    @Test
    // get a tree from dumped nodes success
    public void test3() {
        for (int i = 0; i < roots.size(); i++) {
            Map<byte[], byte[]> m = new ByteArrayMap<>();

            nodes.get(i).forEach((k, v) -> m.put(k.getBytes(), v.getBytes()));

            Store<byte[], byte[]> db = new ByteArrayMapStore<>(m);
            Trie<String, String> trie1 = trie.revert(roots.get(i), db);
            assert equals(dumps.get(roots.get(i).getHex()), dump(trie1));
        }
    }

    private Map<String, String> dump(Trie<String, String> trie) {
        Map<String, String> m = new HashMap<>();
        trie.traverse((k, v) -> {
            m.put(k, v);
            return true;
        });
        return m;
    }

    private boolean equals(Map<String, String> m1, Map<String, String> m2) {
        if (m1.size() != m2.size()) return false;
        for (String k : m1.keySet()) {
            String v1 = m1.get(k);
            if (!m2.containsKey(k)) return false;
            if (!v1.equals(m2.get(k))) return false;
        }
        for (String k : m2.keySet()) {
            String v2 = m2.get(k);
            if (!m1.containsKey(k)) return false;
            if (!v2.equals(m1.get(k))) return false;
        }
        return true;
    }
}
