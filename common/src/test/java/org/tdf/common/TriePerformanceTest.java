package org.tdf.common;

import org.tdf.common.serialize.Codec;
import org.tdf.common.store.ByteArrayMapStore;
import org.tdf.common.trie.Trie;

import java.security.SecureRandom;

// insert 10000000 23382 ms
public class TriePerformanceTest {
    public static void main(String[] args) {
        int n = 10_000_00;
        Trie<byte[], byte[]> trie = TrieUtil.<byte[], byte[]>builder()
            .store(new ByteArrayMapStore<>())
            .keyCodec(Codec.identity())
            .valueCodec(Codec.identity())
            .build();
        byte[] dummy = new byte[]{1};
        SecureRandom sr = new SecureRandom();
        long start = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            byte[] bytes = new byte[32];
            sr.nextBytes(bytes);
            trie.set(bytes, dummy);
            if (i % 100000 == 0) {
//                System.out.println(i * 1.0 / n);
            }
        }
        trie.commit();
        trie.flush();
        long end = System.currentTimeMillis();
        System.out.println("insert " + n + " use " + (end - start) + " ms");
        start = System.currentTimeMillis();
        int size = trie.getSize();
        end = System.currentTimeMillis();
        System.out.println("count " + size + " use " + (end - start) + " ms");
    }
}
