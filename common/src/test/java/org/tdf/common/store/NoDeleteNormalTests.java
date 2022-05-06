package org.tdf.common.store;

import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.tdf.common.util.ByteUtil;

@RunWith(JUnit4.class)
public class NoDeleteNormalTests extends NoDeleteStoreTest {
    @Override
    protected NoDeleteStore<byte[], byte[]> supplyNoDelete() {
        return new NoDeleteStore<>(new ByteArrayMapStore<>(), ByteUtil::isNullOrZeroArray);
    }
}
