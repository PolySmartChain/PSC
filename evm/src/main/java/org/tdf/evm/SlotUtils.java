package org.tdf.evm;

import java.math.BigInteger;

/**
 * slot is a 256bit arithmetic unit
 */
public class SlotUtils {
    public static final int SLOT_SIZE = 8;
    public static final int SLOT_MAX_INDEX = SLOT_SIZE - 1;
    public static final long LONG_MASK = 0xffffffffL;
    public static final int[] ONE = {0, 0, 0, 0, 0, 0, 0, 1};
    public static final int SIGN_BIT_MASK = 0x80000000;
    public static final int SLOT_BYTE_ARRAY_SIZE = 32;
    public static final int INT_SIZE = 4;
    public static final int INT_BITS = 32;
    public static final int SLOT_BITS = SLOT_SIZE * INT_BITS;
    public static final int ADDRESS_SIZE = 20;

    // slot - 1 = slot + negative_one
    public static final int[] NEGATIVE_ONE = {0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff};

    private SlotUtils() {
    }

    public static byte int2byte(int i) {
        return (byte) (i & 0xff);
    }

    /**
     * converts data[offset:offset+4] to 32bit integer in big endian
     */
    public static int decodeInt32(byte[] data, int offset) {
        return ((data[offset] & 0xff) << 24) | ((data[offset + 1] & 0xff) << 16) | ((data[offset + 2] & 0xff) << 8) | (data[offset + 3] & 0xff);
    }

    /**
     * write integer
     */
    public static void encodeInt32(int val, byte[] data, int offset) {
        data[offset] = (byte) ((val >>> 24) & 0xff);
        data[offset + 1] = (byte) ((val >>> 16) & 0xff);
        data[offset + 2] = (byte) ((val >>> 8) & 0xff);
        data[offset + 3] = (byte) (val & 0xff);
    }

    /**
     * dst = left + right, return 1 if overflow
     */
    public static long add(int[] left, int leftOffset, int[] right, int rightOffset, int[] dst, int dstOffset) {
        long carry = 0;

        for (int i = SLOT_MAX_INDEX; i >= 0; i--) {
            long added = (left[leftOffset + i] & LONG_MASK) + (right[rightOffset + i] & LONG_MASK) + carry;
            dst[dstOffset + i] = (int) added;
            carry = added >>> INT_BITS;
        }
        return carry;
    }


    /**
     * write dst as left - right
     */
    public static void sub(int[] left, int leftOffset, int[] right, int rightOffset, int[] dst, int dstOffset) {
        for (int i = 0; i < SLOT_SIZE; i++) {
            dst[dstOffset + i] = ~right[rightOffset + i];
        }
        add(dst, dstOffset, ONE, 0, dst, dstOffset);
        add(left, leftOffset, dst, dstOffset, dst, dstOffset);
    }

    /**
     * encode slot as big endian
     */
    public static void encodeBE(int[] left, int leftOffset, byte[] out, int outOffset) {
        for (int i = 0; i < SLOT_SIZE; i++) {
            encodeInt32(left[i + leftOffset], out, outOffset + i * INT_SIZE);
        }
    }

    /**
     * decode byte array into slot, require data.length - dataOffset >= 32
     */
    public static void decodeBE(byte[] data, int dataOffset, int[] out, int outOffset) {
        for (int i = 0; i < SLOT_SIZE; i++) {
            out[outOffset + i] = decodeInt32(data, dataOffset + i * INT_SIZE);
        }
    }

    /**
     * decode byte array into slot, require data.length - dataOffset <= 32
     */
    public static void copyFrom(int[] slot, int slotOffset, byte[] bytes) {
        if (bytes.length > SLOT_BYTE_ARRAY_SIZE)
            throw new RuntimeException("byte array size overflow > " + SLOT_BYTE_ARRAY_SIZE);

        if (bytes.length == SLOT_BYTE_ARRAY_SIZE) {
            decodeBE(bytes, 0, slot, slotOffset);
            return;
        }

        byte[] data = new byte[SLOT_BYTE_ARRAY_SIZE];
        System.arraycopy(bytes, 0, data, SLOT_BYTE_ARRAY_SIZE - bytes.length, bytes.length);
        decodeBE(data, 0, slot, slotOffset);
    }


    /**
     * copy from biginteger into 256bit byte array
     */
    public static void copyFrom(byte[] dst, int offset, BigInteger value) {
        if (value.signum() < 0)
            throw new RuntimeException("unexpected negative bignumber");

        byte[] bytes = value.toByteArray();

        if (bytes[0] == 0) {
            if (bytes.length - 1 > SLOT_BYTE_ARRAY_SIZE)
                throw new ArithmeticException("bigint overflow");
            System.arraycopy(bytes, 1, dst, offset + SLOT_BYTE_ARRAY_SIZE - bytes.length + 1, bytes.length - 1);
            return;
        }
        if (bytes.length > SLOT_BYTE_ARRAY_SIZE)
            throw new ArithmeticException("bigint overflow");
        System.arraycopy(bytes, 0, dst, offset + SLOT_BYTE_ARRAY_SIZE - bytes.length, bytes.length);
    }

    /**
     * convert unsigned big integer to byte array
     */
    public static byte[] asUnsignedByteArray(
        BigInteger value) {
        byte[] bytes = value.toByteArray();

        if (bytes[0] == 0) {
            byte[] tmp = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, tmp, 0, tmp.length);
            return tmp;
        }
        return bytes;
    }


    /**
     * copy int into slot
     */
    public static void copyFrom(int[] slot, int slotOffset, int n) {
        slot[slotOffset + SLOT_SIZE - 1] = n;
    }

    /**
     * copy long into slot
     */
    public static void copyFrom(int[] slot, int slotOffset, long n) {
        slot[slotOffset + SLOT_SIZE - 1] = (int) n;
        slot[slotOffset + SLOT_SIZE - 2] = (int) (n >>> 32);
    }

    /**
     * copy big integer into slot
     */
    public static void copyFrom(int[] slot, int slotOffset, BigInteger bi) {
        copyFrom(slot, slotOffset, asUnsignedByteArray(bi));
    }

    /**
     * convert slot to big integer
     */
    public static BigInteger toBigInt(int[] slot, int slotOffset) {
        return toBigInt(slot, slotOffset, SLOT_SIZE);
    }

    /**
     * convert slot to big integer
     */
    public static BigInteger toBigInt(int[] slot, int slotOffset, int size) {
        byte[] bytes = new byte[size * INT_SIZE];
        for (int i = 0; i < size; i++)
            encodeInt32(slot[slotOffset + i], bytes, i * INT_SIZE);
        return new BigInteger(1, bytes);
    }

    /**
     * check if slot is zero
     */
    public static boolean isZero(int[] slot, int slotOffset) {
        for (int i = slotOffset; i < slotOffset + SLOT_SIZE; i++) {
            if (slot[i] != 0)
                return false;
        }
        return true;
    }

    /**
     * compare left slot to right slot
     */
    public static int compareTo(int[] left, int leftOffset, int[] right, int rightOffset, int size) {
        for (int i = 0; i < size; i++) {
            int compared = Integer.compareUnsigned(left[leftOffset + i], right[rightOffset + i]);
            if (compared != 0)
                return compared;
        }
        return 0;
    }

    /**
     * keep the least significant 256 bit, write into out
     */
    public static void trim256(int[] data, int dataOffset, int size, int[] out, int outOffset) {
        if (size <= SLOT_SIZE) {
            System.arraycopy(data, dataOffset, out, SLOT_SIZE - size + outOffset, size);
            return;
        }
        System.arraycopy(data, dataOffset + size - SLOT_SIZE, out, outOffset, SLOT_SIZE);
    }

    public static int signOf(int[] dst, int dstOffset) {
        if (dst[dstOffset] < 0)
            return -1;
        return isZero(dst, dstOffset) ? 0 : 1;
    }

    /**
     * return dst[dstOffset:dstOffset+8] == ONE
     */
    public static boolean isOne(int[] dst, int dstOffset) {
        for (int i = 0; i < SLOT_SIZE; i++) {
            if (ONE[i] != dst[dstOffset + i])
                return false;
        }
        return true;
    }

    /**
     * dst = complement(dst)
     */
    public static void complement(int[] dst, int dstOffset) {
        for (int i = dstOffset; i < dstOffset + SLOT_SIZE; i++) {
            dst[i] = ~dst[i];
        }
        add(dst, dstOffset, ONE, 0, dst, dstOffset);
    }

    /**
     * slot = slot << (words * 32 + rems)
     */
    public static void leftShift(int[] val, int offset, int words, int rems) {
        for (int i = 0; i < SLOT_SIZE && words != 0; i++) {
            val[offset + i] = i + words < SLOT_SIZE ? val[offset + i + words] : 0;
        }
        if (rems == 0)
            return;
        int n2 = 32 - rems;
        for (int i = offset, c = val[i], m = i + SLOT_SIZE - 1; i < m; i++) {
            int b = c;
            c = val[i + 1];
            val[i] = (b << rems) | (c >>> n2);
        }
        val[offset + SLOT_SIZE - 1] <<= rems;
    }

    /**
     * logical right shift
     * slot = slot >>> (words * 32 + rems)
     */
    public static void rightShift(int[] val, int offset, int words, int rems) {
        for (int i = SLOT_MAX_INDEX; i >= 0 && words != 0; i--) {
            val[offset + i] = i - words >= 0 ? val[offset + i - words] : 0;
        }
        if (rems == 0)
            return;
        int n2 = 32 - rems;
        for (int i = offset + SLOT_SIZE - 1, c = val[i]; i > offset; i--) {
            int b = c;
            c = val[i - 1];
            val[i] = (c << n2) | (b >>> rems);
        }
        val[offset] >>>= rems;
    }

    /**
     * arithmetic right shift
     * slot = slot >> (words * 32 + rems)
     */
    public static void signedRightShift(int[] val, int offset, int words, int rems) {
        int sig = val[offset] < 0 ? -1 : 0;
        for (int i = SLOT_MAX_INDEX; i >= 0 && words != 0; i--) {
            val[offset + i] = i - words >= 0 ? val[offset + i - words] : sig;
        }
        if (rems == 0)
            return;
        int n2 = 32 - rems;
        for (int i = offset + SLOT_SIZE - 1, c = val[i]; i > offset; i--) {
            int b = c;
            c = val[i - 1];
            val[i] = (c << n2) | (b >>> rems);
        }
        val[offset] >>= rems;
    }

}
