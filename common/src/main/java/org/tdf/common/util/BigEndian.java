package org.tdf.common.util;

/**
 * util for big-endian encoding and decoding
 */
public class BigEndian {

    public static short decodeInt16(byte[] data, int offset) {
        return (short) (((data[offset] & 0xff) << 8) | (data[offset + 1] & 0xff));
    }

    public static void encodeInt16(short num, byte[] data, int offset) {
        data[offset] = (byte) ((num >>> 8) & 0xff);
        data[offset + 1] = (byte) (num & 0xff);
    }

    public static int decodeInt32(byte[] data, int offset) {
        return ((data[offset] & 0xff) << 24) | ((data[offset + 1] & 0xff) << 16) | ((data[offset + 2] & 0xff) << 8) | (data[offset + 3] & 0xff);
    }

    public static void encodeInt32(int val, byte[] data, int offset) {
        data[offset] = (byte) ((val >>> 24) & 0xff);
        data[offset + 1] = (byte) ((val >>> 16) & 0xff);
        data[offset + 2] = (byte) ((val >>> 8) & 0xff);
        data[offset + 3] = (byte) (val & 0xff);
    }

    public static byte[] encodeInt32(int val) {
        byte[] r = new byte[4];
        encodeInt32(val, r, 0);
        return r;
    }

    public static byte[] encodeInt64(long val) {
        byte[] r = new byte[8];
        encodeInt64(val, r, 0);
        return r;
    }

    // big-endian encoding
    public static void encodeInt64(long n, byte[] data, int offset) {
        data[offset] = (byte) ((n >> 56) & 0xffL);
        data[offset + 1] = (byte) ((n >>> 48) & 0xffL);
        data[offset + 2] = (byte) ((n >>> 40) & 0xffL);
        data[offset + 3] = (byte) ((n >>> 32) & 0xffL);
        data[offset + 4] = (byte) ((n >>> 24) & 0xffL);
        data[offset + 5] = (byte) ((n >>> 16) & 0xffL);
        data[offset + 6] = (byte) ((n >>> 8) & 0xffL);
        data[offset + 7] = (byte) (n & 0xffL);
    }

    public static long decodeInt64(byte[] data, int offset) {
        return ((((long) data[offset]) & 0xffL) << 56) |
            (((long) data[offset + 1]) & 0xffL) << 48 |
            (((long) data[offset + 2]) & 0xffL) << 40 |
            (((long) data[offset + 3]) & 0xffL) << 32 |
            (((long) data[offset + 4]) & 0xffL) << 24 |
            (((long) data[offset + 5]) & 0xffL) << 16 |
            (((long) data[offset + 6]) & 0xffL) << 8 |
            (((long) data[offset + 7]) & 0xffL)
            ;
    }


}
