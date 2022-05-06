package org.tdf.common;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.tdf.common.util.LittleEndian;

import java.util.Arrays;

@RunWith(JUnit4.class)
public class LittleEndianTest {

    @Test
    public void testInt16() {
        assert Arrays.equals(
            LittleEndian.encodeInt16((short) 0xabcd),
            new byte[]{(byte) 0xcd, (byte) 0xab}
        );
        assert LittleEndian.decodeInt16(new byte[]{(byte) 0xcd, (byte) 0xab}, 0) == (short) 0xabcd;
    }

    @Test
    public void testInt32() {
        assert Arrays.equals(
            LittleEndian.encodeInt32(0x1234abcd),
            new byte[]{(byte) 0xcd, (byte) 0xab, 0x34, 0x12}
        );

        assert LittleEndian.decodeInt32(new byte[]{(byte) 0xcd, (byte) 0xab, 0x34, 0x12}, 0) == 0x1234abcd;
    }

    @Test
    public void testInt64() {
        assert Arrays.equals(
            LittleEndian.encodeInt64(0x12_34_ab_cd_45_67_89_efL),
            new byte[]{(byte) 0xef, (byte) 0x89, (byte) 0x67, (byte) 0x45, (byte) 0xcd, (byte) 0xab, 0x34, 0x12}
        );

        assert LittleEndian.decodeInt64(new byte[]{(byte) 0xef, (byte) 0x89, (byte) 0x67, (byte) 0x45, (byte) 0xcd, (byte) 0xab, 0x34, 0x12}, 0) == 0x12_34_ab_cd_45_67_89_efL;
    }
}
