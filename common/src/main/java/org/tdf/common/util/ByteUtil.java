package org.tdf.common.util;

import java.lang.reflect.Array;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.tdf.common.types.Constants.WORD_SIZE;

public class ByteUtil {

    public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    public static final byte[] ZERO_BYTE_ARRAY = new byte[]{0};
    public static final HexBytes ZEROS_32 = HexBytes.fromBytes(new byte[WORD_SIZE]);


    /**
     * The regular {@link java.math.BigInteger#toByteArray()} method isn't quite what we often need:
     * it appends a leading zero to indicate that the number is positive and may need padding.
     *
     * @param b        the integer to format into a byte array
     * @param numBytes the desired size of the resulting byte array
     * @return numBytes byte long array.
     */
    public static byte[] bigIntegerToBytes(BigInteger b, int numBytes) {
        if (b == null)
            return null;
        byte[] bytes = new byte[numBytes];
        byte[] biBytes = b.toByteArray();
        int start = (biBytes.length == numBytes + 1) ? 1 : 0;
        int length = Math.min(biBytes.length, numBytes);
        System.arraycopy(biBytes, start, bytes, numBytes - length, length);
        return bytes;
    }

    public static byte[] bigIntegerToBytesSigned(BigInteger b, int numBytes) {
        if (b == null)
            return null;
        byte[] bytes = new byte[numBytes];
        Arrays.fill(bytes, b.signum() < 0 ? (byte) 0xFF : 0x00);
        byte[] biBytes = b.toByteArray();
        int start = (biBytes.length == numBytes + 1) ? 1 : 0;
        int length = Math.min(biBytes.length, numBytes);
        System.arraycopy(biBytes, start, bytes, numBytes - length, length);
        return bytes;
    }

    /**
     * Omitting sign indication byte.
     * <br><br>
     * Instead of {@link org.tdf.common.util.BigIntegers#asUnsignedByteArray(BigInteger)}
     * <br>we use this custom method to avoid an empty array in case of BigInteger.ZERO
     *
     * @param value - any big integer number. A <code>null</code>-value will return <code>null</code>
     * @return A byte array without a leading zero byte if present in the signed encoding.
     * BigInteger.ZERO will return an array with length 1 and byte-value 0.
     */
    public static byte[] bigIntegerToBytes(BigInteger value) {
        if (value == null)
            return null;

        byte[] data = value.toByteArray();

        if (data.length != 1 && data[0] == 0) {
            byte[] tmp = new byte[data.length - 1];
            System.arraycopy(data, 1, tmp, 0, tmp.length);
            data = tmp;
        }
        return data;
    }

    /**
     * Cast hex encoded value from byte[] to BigInteger
     * null is parsed like byte[0]
     *
     * @param bb byte array contains the values
     * @return unsigned positive BigInteger value.
     */
    public static BigInteger bytesToBigInteger(byte[] bb) {
        return (bb == null || bb.length == 0) ? BigInteger.ZERO : new BigInteger(1, bb);
    }


    /**
     * Converts a long value into a byte array.
     *
     * @param val - long value to convert
     * @return decimal value with leading byte that are zeroes striped
     */
    public static byte[] longToBytesNoLeadZeroes(long val) {

        // todo: improve performance by while strip numbers until (long >> 8 == 0)
        if (val == 0) return EMPTY_BYTE_ARRAY;

        byte[] data = ByteBuffer.allocate(Long.BYTES).putLong(val).array();

        return stripLeadingZeroes(data);
    }



    public static String toHexString(byte[] data) {
        return data == null ? "" : HexBytes.encode(data);
    }



    /**
     * Cast hex encoded value from byte[] to int
     * null is parsed like byte[0]
     * <p>
     * Limited to Integer.MAX_VALUE: 2^32-1 (4 bytes)
     *
     * @param b array contains the values
     * @return unsigned positive int value.
     */
    public static int byteArrayToInt(byte[] b) {
        if (b == null || b.length == 0)
            return 0;
        return new BigInteger(1, b).intValue();
    }


    public static int firstNonZeroByte(byte[] data) {
        for (int i = 0; i < data.length; ++i) {
            if (data[i] != 0) {
                return i;
            }
        }
        return -1;
    }

    public static byte[] stripLeadingZeroes(byte[] data) {

        if (data == null)
            return null;

        final int firstNonZero = firstNonZeroByte(data);
        switch (firstNonZero) {
            case -1:
                return ZERO_BYTE_ARRAY;

            case 0:
                return data;

            default:
                byte[] result = new byte[data.length - firstNonZero];
                System.arraycopy(data, firstNonZero, result, 0, data.length - firstNonZero);

                return result;
        }
    }



    public static byte[] setBit(byte[] data, int pos, int val) {

        if ((data.length * 8) - 1 < pos)
            throw new Error("outside byte array limit, pos: " + pos);

        int posByte = data.length - 1 - (pos) / 8;
        int posBit = (pos) % 8;
        byte setter = (byte) (1 << (posBit));
        byte toBeSet = data[posByte];
        byte result;
        if (val == 1)
            result = (byte) (toBeSet | setter);
        else
            result = (byte) (toBeSet & ~setter);

        data[posByte] = result;
        return data;
    }




    /**
     * @param arrays - arrays to merge
     * @return - merged array
     */
    public static byte[] merge(byte[]... arrays) {
        int count = 0;

        // avoid iterator creation here
        for(int i = 0; i < arrays.length; i++) {
            count += arrays[i].length;
        }

        // Create new array and copy all array contents
        byte[] mergedArray = new byte[count];
        int start = 0;

        for(int i = 0; i < arrays.length; i++) {
            byte[] array = arrays[i];
            System.arraycopy(array, 0, mergedArray, start, array.length);
            start += array.length;
        }
        return mergedArray;
    }

    public static boolean isNullOrZeroArray(byte[] array) {
        return (array == null) || (array.length == 0);
    }



    public static short bigEndianToShort(byte[] bs, int off) {
        int n = bs[off] << 8;
        ++off;
        n |= bs[off] & 0xFF;
        return (short) n;
    }

    public static byte[] shortToBytes(short n) {
        return ByteBuffer.allocate(2).putShort(n).array();
    }

    /**
     * Converts string hex representation to data bytes
     * Accepts following hex:
     * - with or without 0x prefix
     * - with no leading 0, like 0xabc -> 0x0abc
     *
     * @param data String like '0xa5e..' or just 'a5e..'
     * @return decoded bytes array
     */
    public static byte[] hexStringToBytes(String data) {
        if (data == null) return EMPTY_BYTE_ARRAY;
        if (data.startsWith("0x")) data = data.substring(2);
        if (data.length() % 2 == 1) data = "0" + data;
        return HexBytes.decode(data);
    }

    /**
     * Converts string representation of host/ip to 4-bytes byte[] IPv4
     */
    public static byte[] hostToBytes(String ip) {
        byte[] bytesIp;
        try {
            bytesIp = InetAddress.getByName(ip).getAddress();
        } catch (UnknownHostException e) {
            bytesIp = new byte[4];  // fall back to invalid 0.0.0.0 address
        }

        return bytesIp;
    }

    /**
     * Converts 4 bytes IPv4 IP to String representation
     */
    public static String bytesToIp(byte[] bytesIp) {

        StringBuilder sb = new StringBuilder();
        sb.append(bytesIp[0] & 0xFF);
        sb.append(".");
        sb.append(bytesIp[1] & 0xFF);
        sb.append(".");
        sb.append(bytesIp[2] & 0xFF);
        sb.append(".");
        sb.append(bytesIp[3] & 0xFF);

        return sb.toString();
    }

    /**
     * Returns a number of zero bits preceding the highest-order ("leftmost") one-bit
     * interpreting input array as a big-endian integer value
     */
    public static int numberOfLeadingZeros(byte[] bytes) {

        int i = firstNonZeroByte(bytes);

        if (i == -1) {
            return bytes.length * 8;
        } else {
            int byteLeadingZeros = Integer.numberOfLeadingZeros((int) bytes[i] & 0xff) - 24;
            return i * 8 + byteLeadingZeros;
        }
    }


    public static byte[] subarray(byte[] array, int startIndexInclusive, int endIndexExclusive) {
        if (array == null) {
            return null;
        } else {
            if (startIndexInclusive < 0) {
                startIndexInclusive = 0;
            }

            if (endIndexExclusive > array.length) {
                endIndexExclusive = array.length;
            }

            int newSize = endIndexExclusive - startIndexInclusive;
            if (newSize <= 0) {
                return EMPTY_BYTE_ARRAY;
            } else {
                byte[] subarray = new byte[newSize];
                System.arraycopy(array, startIndexInclusive, subarray, 0, newSize);
                return subarray;
            }
        }
    }

    public static byte[][] subarray(byte[][] array, int startIndexInclusive, int endIndexExclusive) {
        if (array == null) {
            return null;
        } else {
            if (startIndexInclusive < 0) {
                startIndexInclusive = 0;
            }

            if (endIndexExclusive > array.length) {
                endIndexExclusive = array.length;
            }

            int newSize = endIndexExclusive - startIndexInclusive;
            byte[][] subarray;
            if (newSize <= 0) {
                subarray = (byte[][]) Array.newInstance(byte[].class, 0);
                return subarray;
            } else {
                subarray = (byte[][]) Array.newInstance(byte[].class, newSize);
                System.arraycopy(array, startIndexInclusive, subarray, 0, newSize);
                return subarray;
            }
        }
    }
}