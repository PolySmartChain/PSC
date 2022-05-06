package org.tdf.common.store;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.tdf.common.serialize.Codecs;
import org.tdf.common.util.HexBytes;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class PrefixStoreTest {

    @Test
    public void test0() {
        Store<HexBytes, HexBytes> s = new MapStore<>();
        PrefixStore<String, String> p = new PrefixStore<>(
            s,
            HexBytes.fromBytes("aaa".getBytes(StandardCharsets.US_ASCII)),
            Codecs.INSTANCE.rlp(String.class),
            Codecs.INSTANCE.rlp(String.class)
        );

        p.set("aaa", "bbb");
//        Map<String, String> m = new HashMap<>(p.asMap());
        assertEquals("bbb", p.get("aaa"));
    }
}
