package org.tdf.common.util;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.HashMap;
import java.util.Map;

@RunWith(JUnit4.class)
public class ByteArrayMapTests {
    protected Map<byte[], String> map;

    @Before
    public void before() {
        map = new ByteArrayMap<>();
        map.put("1".getBytes(), "1");
        map.put("2".getBytes(), "2");
        map.put("3".getBytes(), "3");
    }

    @Test
    public void test() {
        map.entrySet().toArray(new HashMap.SimpleEntry[0]);
        map.entrySet().forEach(entry -> {
            System.out.println(new String(entry.getKey()));
            System.out.println(entry.getValue());
        });
    }
}
