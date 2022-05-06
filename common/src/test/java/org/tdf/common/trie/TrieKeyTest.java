package org.tdf.common.trie;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;

@RunWith(JUnit4.class)
public class TrieKeyTest {

    @Test
    public void test0() {
        TrieKey k = TrieKey.single(1).concat(TrieKey.single(2)).concat(TrieKey.single(3));
        assert k.get(0) == 1;
        assert k.get(1) == 2;
        assert k.get(2) == 3;
        assert Arrays.equals(k.toPacked(false), new byte[]{0x11, 0x23});
    }
}
