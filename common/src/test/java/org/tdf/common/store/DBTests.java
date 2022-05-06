package org.tdf.common.store;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.tdf.common.serialize.Codecs;
import org.tdf.common.util.ByteArrayMap;
import org.tdf.common.util.FastByteComparisons;

import java.util.Map;
import java.util.Objects;

@Ignore
public abstract class DBTests {

    protected DatabaseStore databaseStore;

    protected Store<String, String> wrapped;

    abstract DatabaseStore getDB();

    @Before
    public void before() {
        databaseStore = getDB();
        databaseStore.init(DBSettings.DEFAULT);
        wrapped = new StoreWrapper<>(databaseStore, Codecs.string, Codecs.string);
    }

    @After
    public void after() {
        databaseStore.clear();
        databaseStore.close();
    }

    @Test
    public void test() {
        assert databaseStore.getAlive();
        wrapped.set("1", "1");
        assert wrapped.get("1").equals("1");
    }


    @Test
    public void testPutAll() {
        Map<byte[], byte[]> rows = new ByteArrayMap<>();
        rows.put("1".getBytes(), "1".getBytes());
        rows.put("2".getBytes(), "2".getBytes());
        databaseStore.putAll(rows.entrySet());
        assert FastByteComparisons.equal(
            Objects.requireNonNull(databaseStore.get("1".getBytes())),
            "1".getBytes()
        );


        rows = new ByteArrayMap<>();
        rows.put("1".getBytes(), new byte[0]);
        rows.put("2".getBytes(), new byte[0]);
        databaseStore.putAll(rows.entrySet());
        assert databaseStore.get("1".getBytes()) == null || databaseStore.get("1".getBytes()).length == 0;
    }
}
