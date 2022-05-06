package org.tdf.common.store;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.tdf.common.util.FastByteComparisons;

import java.util.Objects;

@Ignore
public abstract class NoDeleteStoreTest {
    protected Store<byte[], byte[]> store;


    abstract protected Store<byte[], byte[]> supplyNoDelete();

    @Before
    public void before() {
        store = supplyNoDelete();
        store.set("a".getBytes(), "1".getBytes());
        store.set("b".getBytes(), "2".getBytes());
        store.set("c".getBytes(), "3".getBytes());
        store.flush();
    }


    @Test
    public void test1() {
        store.remove("a".getBytes());
        store.flush();
        assert Objects.requireNonNull(store.get("a".getBytes())).length != 0;

        assert FastByteComparisons.equal(
            Objects.requireNonNull(store.get("a".getBytes())),
            "1".getBytes()
        );

        store.flush();
        assert Objects.requireNonNull(store.get("a".getBytes())).length != 0;
    }

    @Test
    public void test2() {
        store.remove("a".getBytes());
        store.set("a".getBytes(), "11".getBytes());
        assert FastByteComparisons.equal(
            Objects.requireNonNull(store.get("a".getBytes())),
            "11".getBytes()
        );
    }
}
