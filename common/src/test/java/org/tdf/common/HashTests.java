package org.tdf.common;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.tdf.common.util.HashUtil;

import java.security.SecureRandom;

import static org.junit.Assert.assertArrayEquals;

@RunWith(JUnit4.class)
public class HashTests {

    @Test
    public void test0() {
        SecureRandom sr = new SecureRandom();
        byte[] dst = new byte[32];
        byte[] rand = new byte[100];

        for (int i = 0; i < 100; i++) {
            sr.nextBytes(rand);
            HashUtil.sha3(rand, 0, rand.length / 2, dst, 0);
            assertArrayEquals(dst, HashUtil.sha3(rand, 0, rand.length / 2));
        }
    }
}
