package org.tdf.common.trie;

import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.tdf.common.serialize.Codec;
import org.tdf.common.serialize.Codecs;

import static org.tdf.common.TrieUtil.newInstance;

@RunWith(JUnit4.class)
public class TrieImplTest extends AbstractTrieTest {
    Trie<String, String> newStringTrie() {
        return newInstance(new NoDoubleDeleteStore(), Codecs.string, Codecs.string);
    }

    Trie<byte[], byte[]> newBytesTrie() {
        return newInstance(new NoDoubleDeleteStore(), Codec.identity(), Codec.identity());
    }

}
