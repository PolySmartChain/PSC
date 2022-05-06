package org.tdf.common.store;

import org.iq80.leveldb.impl.Iq80DBFactory;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class LevelDBTests extends DBTests {
    @Override
    DatabaseStore getDB() {
        return new LevelDb(Iq80DBFactory.factory, "local/leveldb");
    }
}
