package org.tdf.evm;

import java.lang.reflect.Constructor;
import java.math.BigInteger;
import java.util.Arrays;

import static org.tdf.evm.SlotUtils.SLOT_SIZE;

public class MutableBigInteger {
    static final long INFLATED = Long.MIN_VALUE;

    static final long LONG_MASK = 0xffffffffL;
    /**
     * MutableBigInteger with one element value array with the value 1. Used by
     * BigDecimal divideAndRound to increment the quotient. Use this constant
     * only when the method is not going to modify this object.
     */
    static final MutableBigInteger ONE = new MutableBigInteger(1);
    /**
     * The minimum {@code intLen} for cancelling powers of two before
     * dividing.
     * If the number of ints is less than this threshold,
     * {@code divideKnuth} does not eliminate common powers of two from
     * the dividend and divisor.
     */
    static final int KNUTH_POW2_THRESH_LEN = 6;
    /**
     * The minimum number of trailing zero ints for cancelling powers of two
     * before dividing.
     * If the dividend and divisor don't share at least this many zero ints
     * at the end, {@code divideKnuth} does not eliminate common powers
     * of two from the dividend and divisor.
     */
    static final int KNUTH_POW2_THRESH_ZEROS = 3;
    private static Constructor<BigInteger> BIG_INTEGER_CONSTRUCTOR = null;
    /**
     * Holds the magnitude of this MutableBigInteger in big endian order.
     * The magnitude may start at an offset into the value array, and it may
     * end before the length of the value array.
     */
    int[] value;
    /**
     * The number of ints of the value array that are currently used
     * to hold the magnitude of this MutableBigInteger. The magnitude starts
     * at an offset and offset + intLen may be less than value.length.
     */
    int intLen;
    /**
     * The offset into the value array where the magnitude of this
     * MutableBigInteger begins.
     */
    int offset = 0;

    /**
     * The default constructor. An empty MutableBigInteger is created with
     * a one word capacity.
     */
    public MutableBigInteger() {
        value = new int[1];
        intLen = 0;
    }

    /**
     * Construct a new MutableBigInteger with a magnitude specified by
     * the int val.
     */
    public MutableBigInteger(int val) {
        value = new int[1];
        intLen = 1;
        value[0] = val;
    }

    /**
     * Construct a new MutableBigInteger with the specified value array
     * up to the length of the array supplied.
     */
    public MutableBigInteger(int[] val) {
        value = val;
        intLen = val.length;
    }

    // Constants

    /**
     * Construct a new MutableBigInteger with a magnitude equal to the
     * specified MutableBigInteger.
     */
    MutableBigInteger(MutableBigInteger val) {
        intLen = val.intLen;
        value = Arrays.copyOfRange(val.value, val.offset, val.offset + intLen);
    }

    static int bitLengthForInt(int n) {
        return 32 - Integer.numberOfLeadingZeros(n);
    }

    private static Constructor<BigInteger> getBigIntegerConstructor() {
        if (BIG_INTEGER_CONSTRUCTOR != null)
            return BIG_INTEGER_CONSTRUCTOR;

        try {
            Constructor<BigInteger> constructor = BigInteger.class.getDeclaredConstructor(int[].class, int.class);
            constructor.setAccessible(true);

            BIG_INTEGER_CONSTRUCTOR = constructor;
            return constructor;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Constructors

    private static void copyAndShift(int[] src, int srcFrom, int srcLen, int[] dst, int dstFrom, int shift) {
        int n2 = 32 - shift;
        int c = src[srcFrom];
        for (int i = 0; i < srcLen - 1; i++) {
            int b = c;
            c = src[++srcFrom];
            dst[dstFrom + i] = (b << shift) | (c >>> n2);
        }
        dst[dstFrom + srcLen - 1] = c << shift;
    }

    /**
     * This method divides a long quantity by an int to estimate
     * qhat for two multi precision numbers. It is used when
     * the signed value of n is less than zero.
     * Returns long value where high 32 bits contain remainder value and
     * low 32 bits contain quotient value.
     */
    static long divWord(long n, int d) {
        long dLong = d & LONG_MASK;
        long r;
        long q;
        if (dLong == 1) {
            q = (int) n;
            r = 0;
            return (r << 32) | (q & LONG_MASK);
        }

        // Approximate the quotient and remainder
        q = (n >>> 1) / (dLong >>> 1);
        r = n - q * dLong;

        // Correct the approximation
        while (r < 0) {
            r += dLong;
            q--;
        }
        while (r >= dLong) {
            r -= dLong;
            q++;
        }
        // n - q*dlong == r && 0 <= r <dLong, hence we're done.
        return (r << 32) | (q & LONG_MASK);
    }

    /**
     * Calculate GCD of a and b interpreted as unsigned integers.
     */
    static int binaryGcd(int a, int b) {
        if (b == 0)
            return a;
        if (a == 0)
            return b;

        // Right shift a & b till their last bits equal to 1.
        int aZeros = Integer.numberOfTrailingZeros(a);
        int bZeros = Integer.numberOfTrailingZeros(b);
        a >>>= aZeros;
        b >>>= bZeros;

        int t = (aZeros < bZeros ? aZeros : bZeros);

        while (a != b) {
            if ((a + 0x80000000) > (b + 0x80000000)) {  // a > b as unsigned
                a -= b;
                a >>>= Integer.numberOfTrailingZeros(a);
            } else {
                b -= a;
                b >>>= Integer.numberOfTrailingZeros(b);
            }
        }
        return a << t;
    }

    /**
     * Returns the multiplicative inverse of val mod 2^32.  Assumes val is odd.
     */
    static int inverseMod32(int val) {
        // Newton's iteration!
        int t = val;
        t *= 2 - val * t;
        t *= 2 - val * t;
        t *= 2 - val * t;
        t *= 2 - val * t;
        return t;
    }

    /**
     * Returns the multiplicative inverse of val mod 2^64.  Assumes val is odd.
     */
    static long inverseMod64(long val) {
        // Newton's iteration!
        long t = val;
        t *= 2 - val * t;
        t *= 2 - val * t;
        t *= 2 - val * t;
        t *= 2 - val * t;
        t *= 2 - val * t;
        assert (t * val == 1);
        return t;
    }

    public int[] getValue() {
        return value;
    }

    public void setValue(int[] value) {
        this.value = value;
    }

    public int getIntLen() {
        return intLen;
    }

    public void setIntLen(int intLen) {
        this.intLen = intLen;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public BigInteger toBigInt() {
        normalize();
        return toBigInt(isZero() ? 0 : 1);
    }

    public BigInteger toBigInt(int sign) {
        if (intLen == 0 || sign == 0)
            return BigInteger.ZERO;
        try {
            return getBigIntegerConstructor().newInstance(getMagnitudeArray(), sign);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Makes this number an {@code n}-int number all of whose bits are ones.
     * Used by Burnikel-Ziegler division.
     *
     * @param n number of ints in the {@code value} array
     * @return a number equal to {@code ((1<<(32*n)))-1}
     */
    private void ones(int n) {
        if (n > value.length)
            value = new int[n];
        Arrays.fill(value, -1);
        offset = 0;
        intLen = n;
    }

    /**
     * Internal helper method to return the magnitude array. The caller is not
     * supposed to modify the returned array.
     */
    private int[] getMagnitudeArray() {
        return Arrays.copyOfRange(value, offset, offset + intLen);
    }

    /**
     * Convert this MutableBigInteger to a long value. The caller has to make
     * sure this MutableBigInteger can be fit into long.
     */
    private long toLong() {
        assert (intLen <= 2) : "this MutableBigInteger exceeds the range of long";
        if (intLen == 0)
            return 0;
        long d = value[offset] & LONG_MASK;
        return (intLen == 2) ? d << 32 | (value[offset + 1] & LONG_MASK) : d;
    }

    /**
     * Clear out a MutableBigInteger for reuse.
     */
    public void clear() {
        offset = intLen = 0;
        for (int index = 0, n = value.length; index < n; index++)
            value[index] = 0;
    }

    /**
     * Set a MutableBigInteger to zero, removing its offset.
     */
    public void reset() {
        offset = intLen = 0;
    }

    /**
     * Compare the magnitude of two MutableBigIntegers. Returns -1, 0 or 1
     * as this MutableBigInteger is numerically less than, equal to, or
     * greater than {@code b}.
     */
    final int compare(MutableBigInteger b) {
        int blen = b.intLen;
        if (intLen < blen)
            return -1;
        if (intLen > blen)
            return 1;

        // Add Integer.MIN_VALUE to make the comparison act as unsigned integer
        // comparison.
        int[] bval = b.value;
        for (int i = offset, j = b.offset; i < intLen + offset; i++, j++) {
            int b1 = value[i] + 0x80000000;
            int b2 = bval[j] + 0x80000000;
            if (b1 < b2)
                return -1;
            if (b1 > b2)
                return 1;
        }
        return 0;
    }

    /**
     * Returns a value equal to what {@code b.leftShift(32*ints); return compare(b);}
     * would return, but doesn't change the value of {@code b}.
     */
    private int compareShifted(MutableBigInteger b, int ints) {
        int blen = b.intLen;
        int alen = intLen - ints;
        if (alen < blen)
            return -1;
        if (alen > blen)
            return 1;

        // Add Integer.MIN_VALUE to make the comparison act as unsigned integer
        // comparison.
        int[] bval = b.value;
        for (int i = offset, j = b.offset; i < alen + offset; i++, j++) {
            int b1 = value[i] + 0x80000000;
            int b2 = bval[j] + 0x80000000;
            if (b1 < b2)
                return -1;
            if (b1 > b2)
                return 1;
        }
        return 0;
    }

    /**
     * Compare this against half of a MutableBigInteger object (Needed for
     * remainder tests).
     * Assumes no leading unnecessary zeros, which holds for results
     * from divide().
     */
    final int compareHalf(MutableBigInteger b) {
        int blen = b.intLen;
        int len = intLen;
        if (len <= 0)
            return blen <= 0 ? 0 : -1;
        if (len > blen)
            return 1;
        if (len < blen - 1)
            return -1;
        int[] bval = b.value;
        int bstart = 0;
        int carry = 0;
        // Only 2 cases left:len == blen or len == blen - 1
        if (len != blen) { // len == blen - 1
            if (bval[bstart] == 1) {
                ++bstart;
                carry = 0x80000000;
            } else
                return -1;
        }
        // compare values with right-shifted values of b,
        // carrying shifted-out bits across words
        int[] val = value;
        for (int i = offset, j = bstart; i < len + offset; ) {
            int bv = bval[j++];
            long hb = ((bv >>> 1) + carry) & LONG_MASK;
            long v = val[i++] & LONG_MASK;
            if (v != hb)
                return v < hb ? -1 : 1;
            carry = (bv & 1) << 31; // carray will be either 0x80000000 or 0
        }
        return carry == 0 ? 0 : -1;
    }

    /**
     * Return the index of the lowest set bit in this MutableBigInteger. If the
     * magnitude of this MutableBigInteger is zero, -1 is returned.
     */
    private final int getLowestSetBit() {
        if (intLen == 0)
            return -1;
        int j, b;
        for (j = intLen - 1; (j > 0) && (value[j + offset] == 0); j--)
            ;
        b = value[j + offset];
        if (b == 0)
            return -1;
        return ((intLen - 1 - j) << 5) + Integer.numberOfTrailingZeros(b);
    }

    /**
     * Return the int in use in this MutableBigInteger at the specified
     * index. This method is not used because it is not inlined on all
     * platforms.
     */
    private final int getInt(int index) {
        return value[offset + index];
    }

    /**
     * Return a long which is equal to the unsigned value of the int in
     * use in this MutableBigInteger at the specified index. This method is
     * not used because it is not inlined on all platforms.
     */
    private final long getLong(int index) {
        return value[offset + index] & LONG_MASK;
    }

    /**
     * Ensure that the MutableBigInteger is in normal form, specifically
     * making sure that there are no leading zeros, and that if the
     * magnitude is zero, then intLen is zero.
     */
    public final void normalize() {
        if (intLen == 0) {
            offset = 0;
            return;
        }

        int index = offset;
        if (value[index] != 0)
            return;

        int indexBound = index + intLen;
        do {
            index++;
        } while (index < indexBound && value[index] == 0);

        int numZeros = index - offset;
        intLen -= numZeros;
        offset = (intLen == 0 ? 0 : offset + numZeros);
    }

    /**
     * If this MutableBigInteger cannot hold len words, increase the size
     * of the value array to len words.
     */
    private final void ensureCapacity(int len) {
        if (value.length < len) {
            value = new int[len];
            offset = 0;
            intLen = len;
        }
    }

    /**
     * Convert this MutableBigInteger into an int array with no leading
     * zeros, of a length that is equal to this MutableBigInteger's intLen.
     */
    int[] toIntArray() {
        int[] result = new int[intLen];
        for (int i = 0; i < intLen; i++)
            result[i] = value[offset + i];
        return result;
    }

    /**
     * Sets the int at index+offset in this MutableBigInteger to val.
     * This does not get inlined on all platforms so it is not used
     * as often as originally intended.
     */
    void setInt(int index, int val) {
        value[offset + index] = val;
    }

    /**
     * Sets this MutableBigInteger's value array to the specified array.
     * The intLen is set to the specified length.
     */
    public void setValue(int[] val, int length) {
        value = val;
        intLen = length;
        offset = 0;
    }

    /**
     * Sets this MutableBigInteger's value array to a copy of the specified
     * array. The intLen is set to the length of the new array.
     */
    public void copyValue(MutableBigInteger src) {
        int len = src.intLen;
        if (value.length < len)
            value = new int[len];
        System.arraycopy(src.value, src.offset, value, 0, len);
        intLen = len;
        offset = 0;
    }

    /**
     * Sets this MutableBigInteger's value array to a copy of the specified
     * array. The intLen is set to the length of the specified array.
     */
    public void copyValue(int[] val) {
        int len = val.length;
        if (value.length < len)
            value = new int[len];
        System.arraycopy(val, 0, value, 0, len);
        intLen = len;
        offset = 0;
    }

    /**
     * Returns true iff this MutableBigInteger has a value of one.
     */
    public boolean isOne() {
        return (intLen == 1) && (value[offset] == 1);
    }

    /**
     * Returns true iff this MutableBigInteger has a value of zero.
     */
    public boolean isZero() {
        return (intLen == 0);
    }

    /**
     * Returns true iff this MutableBigInteger is even.
     */
    public boolean isEven() {
        return (intLen == 0) || ((value[offset + intLen - 1] & 1) == 0);
    }

    /**
     * Returns true iff this MutableBigInteger is odd.
     */
    public boolean isOdd() {
        return isZero() ? false : ((value[offset + intLen - 1] & 1) == 1);
    }

    /**
     * Returns true iff this MutableBigInteger is in normal form. A
     * MutableBigInteger is in normal form if it has no leading zeros
     * after the offset, and intLen + offset <= value.length.
     */
    boolean isNormal() {
        if (intLen + offset > value.length)
            return false;
        if (intLen == 0)
            return true;
        return (value[offset] != 0);
    }

    /**
     * Like {@link #rightShift(int)} but {@code n} can be greater than the length of the number.
     */
    void safeRightShift(int n) {
        if (n / 32 >= intLen) {
            reset();
        } else {
            rightShift(n);
        }
    }

    /**
     * Right shift this MutableBigInteger n bits. The MutableBigInteger is left
     * in normal form.
     */
    void rightShift(int n) {
        if (intLen == 0)
            return;
        int nInts = n >>> 5;
        int nBits = n & 0x1F;
        this.intLen -= nInts;
        if (nBits == 0)
            return;
        int bitsInHighWord = bitLengthForInt(value[offset]);
        if (nBits >= bitsInHighWord) {
            this.primitiveLeftShift(32 - nBits);
            this.intLen--;
        } else {
            primitiveRightShift(nBits);
        }
    }

    /**
     * Like {@link #leftShift(int)} but {@code n} can be zero.
     */
    void safeLeftShift(int n) {
        if (n > 0) {
            leftShift(n);
        }
    }

    /**
     * Left shift this MutableBigInteger n bits.
     */
    void leftShift(int n) {
        /*
         * If there is enough storage space in this MutableBigInteger already
         * the available space will be used. Space to the right of the used
         * ints in the value array is faster to utilize, so the extra space
         * will be taken from the right if possible.
         */
        if (intLen == 0)
            return;
        int nInts = n >>> 5;
        int nBits = n & 0x1F;
        int bitsInHighWord = bitLengthForInt(value[offset]);

        // If shift can be done without moving words, do so
        if (n <= (32 - bitsInHighWord)) {
            primitiveLeftShift(nBits);
            return;
        }

        int newLen = intLen + nInts + 1;
        if (nBits <= (32 - bitsInHighWord))
            newLen--;
        if (value.length < newLen) {
            // The array must grow
            int[] result = new int[newLen];
            for (int i = 0; i < intLen; i++)
                result[i] = value[offset + i];
            setValue(result, newLen);
        } else if (value.length - offset >= newLen) {
            // Use space on right
            for (int i = 0; i < newLen - intLen; i++)
                value[offset + intLen + i] = 0;
        } else {
            // Must use space on left
            for (int i = 0; i < intLen; i++)
                value[i] = value[offset + i];
            for (int i = intLen; i < newLen; i++)
                value[i] = 0;
            offset = 0;
        }
        intLen = newLen;
        if (nBits == 0)
            return;
        if (nBits <= (32 - bitsInHighWord))
            primitiveLeftShift(nBits);
        else
            primitiveRightShift(32 - nBits);
    }

    /**
     * A primitive used for division. This method adds in one multiple of the
     * divisor a back to the dividend result at a specified offset. It is used
     * when qhat was estimated too large, and must be adjusted.
     */
    private int divadd(int[] a, int aLen, int[] result, int offset) {
        long carry = 0;

        for (int j = aLen - 1; j >= 0; j--) {
            long sum = (a[j] & LONG_MASK) +
                (result[j + offset] & LONG_MASK) + carry;
            result[j + offset] = (int) sum;
            carry = sum >>> 32;
        }
        return (int) carry;
    }

    /**
     * This method is used for division. It multiplies an n word input a by one
     * word input x, and subtracts the n word product from q. This is needed
     * when subtracting qhat*divisor from dividend.
     */
    private int mulsub(int[] q, int[] a, int x, int len, int offset) {
        long xLong = x & LONG_MASK;
        long carry = 0;
        offset += len;

        for (int j = len - 1; j >= 0; j--) {
            long product = (a[j] & LONG_MASK) * xLong + carry;
            long difference = q[offset] - product;
            q[offset--] = (int) difference;
            carry = (product >>> 32)
                + (((difference & LONG_MASK) >
                (((~(int) product) & LONG_MASK))) ? 1 : 0);
        }
        return (int) carry;
    }

    /**
     * The method is the same as mulsun, except the fact that q array is not
     * updated, the only result of the method is borrow flag.
     */
    private int mulsubBorrow(int[] q, int[] a, int x, int len, int offset) {
        long xLong = x & LONG_MASK;
        long carry = 0;
        offset += len;
        for (int j = len - 1; j >= 0; j--) {
            long product = (a[j] & LONG_MASK) * xLong + carry;
            long difference = q[offset--] - product;
            carry = (product >>> 32)
                + (((difference & LONG_MASK) >
                (((~(int) product) & LONG_MASK))) ? 1 : 0);
        }
        return (int) carry;
    }

    /**
     * Right shift this MutableBigInteger n bits, where n is
     * less than 32.
     * Assumes that intLen > 0, n > 0 for speed
     */
    private final void primitiveRightShift(int n) {
        int[] val = value;
        int n2 = 32 - n;
        for (int i = offset + intLen - 1, c = val[i]; i > offset; i--) {
            int b = c;
            c = val[i - 1];
            val[i] = (c << n2) | (b >>> n);
        }
        val[offset] >>>= n;
    }

    /**
     * Left shift this MutableBigInteger n bits, where n is
     * less than 32.
     * Assumes that intLen > 0, n > 0 for speed
     */
    private final void primitiveLeftShift(int n) {
        int[] val = value;
        int n2 = 32 - n;
        for (int i = offset, c = val[i], m = i + intLen - 1; i < m; i++) {
            int b = c;
            c = val[i + 1];
            val[i] = (b << n) | (c >>> n2);
        }
        val[offset + intLen - 1] <<= n;
    }

    /**
     * Discards all ints whose index is greater than {@code n}.
     */
    private void keepLower(int n) {
        if (intLen >= n) {
            offset += intLen - n;
            intLen = n;
        }
    }

    /**
     * Adds the contents of two MutableBigInteger objects.The result
     * is placed within this MutableBigInteger.
     * The contents of the addend are not changed.
     */
    public void add(MutableBigInteger addend) {
        int x = intLen;
        int y = addend.intLen;
        int resultLen = (intLen > addend.intLen ? intLen : addend.intLen);
        int[] result = (value.length < resultLen ? new int[resultLen] : value);

        int rstart = result.length - 1;
        long sum;
        long carry = 0;

        // Add common parts of both numbers
        while (x > 0 && y > 0) {
            x--;
            y--;
            sum = (value[x + offset] & LONG_MASK) +
                (addend.value[y + addend.offset] & LONG_MASK) + carry;
            result[rstart--] = (int) sum;
            carry = sum >>> 32;
        }

        // Add remainder of the longer number
        while (x > 0) {
            x--;
            if (carry == 0 && result == value && rstart == (x + offset))
                return;
            sum = (value[x + offset] & LONG_MASK) + carry;
            result[rstart--] = (int) sum;
            carry = sum >>> 32;
        }
        while (y > 0) {
            y--;
            sum = (addend.value[y + addend.offset] & LONG_MASK) + carry;
            result[rstart--] = (int) sum;
            carry = sum >>> 32;
        }

        if (carry > 0) { // Result must grow in length
            resultLen++;
            if (result.length < resultLen) {
                int[] temp = new int[resultLen];
                // Result one word longer from carry-out; copy low-order
                // bits into new result.
                System.arraycopy(result, 0, temp, 1, result.length);
                temp[0] = 1;
                result = temp;
            } else {
                result[rstart--] = 1;
            }
        }

        value = result;
        intLen = resultLen;
        offset = result.length - resultLen;
    }

    /**
     * Adds the value of {@code addend} shifted {@code n} ints to the left.
     * Has the same effect as {@code addend.leftShift(32*ints); add(addend);}
     * but doesn't change the value of {@code addend}.
     */
    void addShifted(MutableBigInteger addend, int n) {
        if (addend.isZero()) {
            return;
        }

        int x = intLen;
        int y = addend.intLen + n;
        int resultLen = (intLen > y ? intLen : y);
        int[] result = (value.length < resultLen ? new int[resultLen] : value);

        int rstart = result.length - 1;
        long sum;
        long carry = 0;

        // Add common parts of both numbers
        while (x > 0 && y > 0) {
            x--;
            y--;
            int bval = y + addend.offset < addend.value.length ? addend.value[y + addend.offset] : 0;
            sum = (value[x + offset] & LONG_MASK) +
                (bval & LONG_MASK) + carry;
            result[rstart--] = (int) sum;
            carry = sum >>> 32;
        }

        // Add remainder of the longer number
        while (x > 0) {
            x--;
            if (carry == 0 && result == value && rstart == (x + offset)) {
                return;
            }
            sum = (value[x + offset] & LONG_MASK) + carry;
            result[rstart--] = (int) sum;
            carry = sum >>> 32;
        }
        while (y > 0) {
            y--;
            int bval = y + addend.offset < addend.value.length ? addend.value[y + addend.offset] : 0;
            sum = (bval & LONG_MASK) + carry;
            result[rstart--] = (int) sum;
            carry = sum >>> 32;
        }

        if (carry > 0) { // Result must grow in length
            resultLen++;
            if (result.length < resultLen) {
                int[] temp = new int[resultLen];
                // Result one word longer from carry-out; copy low-order
                // bits into new result.
                System.arraycopy(result, 0, temp, 1, result.length);
                temp[0] = 1;
                result = temp;
            } else {
                result[rstart--] = 1;
            }
        }

        value = result;
        intLen = resultLen;
        offset = result.length - resultLen;
    }

    /**
     * Like {@link #addShifted(MutableBigInteger, int)} but {@code this.intLen} must
     * not be greater than {@code n}. In other words, concatenates {@code this}
     * and {@code addend}.
     */
    void addDisjoint(MutableBigInteger addend, int n) {
        if (addend.isZero())
            return;

        int x = intLen;
        int y = addend.intLen + n;
        int resultLen = (intLen > y ? intLen : y);
        int[] result;
        if (value.length < resultLen)
            result = new int[resultLen];
        else {
            result = value;
            Arrays.fill(value, offset + intLen, value.length, 0);
        }

        int rstart = result.length - 1;

        // copy from this if needed
        System.arraycopy(value, offset, result, rstart + 1 - x, x);
        y -= x;
        rstart -= x;

        int len = Math.min(y, addend.value.length - addend.offset);
        System.arraycopy(addend.value, addend.offset, result, rstart + 1 - y, len);

        // zero the gap
        for (int i = rstart + 1 - y + len; i < rstart + 1; i++)
            result[i] = 0;

        value = result;
        intLen = resultLen;
        offset = result.length - resultLen;
    }

    /**
     * Adds the low {@code n} ints of {@code addend}.
     */
    void addLower(MutableBigInteger addend, int n) {
        MutableBigInteger a = new MutableBigInteger(addend);
        if (a.offset + a.intLen >= n) {
            a.offset = a.offset + a.intLen - n;
            a.intLen = n;
        }
        a.normalize();
        add(a);
    }

    /**
     * Subtracts the smaller of this and b from the larger and places the
     * result into this MutableBigInteger.
     */
    int subtract(MutableBigInteger b) {
        MutableBigInteger a = this;

        int[] result = value;
        int sign = a.compare(b);

        if (sign == 0) {
            reset();
            return 0;
        }
        if (sign < 0) {
            MutableBigInteger tmp = a;
            a = b;
            b = tmp;
        }

        int resultLen = a.intLen;
        if (result.length < resultLen)
            result = new int[resultLen];

        long diff = 0;
        int x = a.intLen;
        int y = b.intLen;
        int rstart = result.length - 1;

        // Subtract common parts of both numbers
        while (y > 0) {
            x--;
            y--;

            diff = (a.value[x + a.offset] & LONG_MASK) -
                (b.value[y + b.offset] & LONG_MASK) - ((int) -(diff >> 32));
            result[rstart--] = (int) diff;
        }
        // Subtract remainder of longer number
        while (x > 0) {
            x--;
            diff = (a.value[x + a.offset] & LONG_MASK) - ((int) -(diff >> 32));
            result[rstart--] = (int) diff;
        }

        value = result;
        intLen = resultLen;
        offset = value.length - resultLen;
        normalize();
        return sign;
    }

    /**
     * Subtracts the smaller of a and b from the larger and places the result
     * into the larger. Returns 1 if the answer is in a, -1 if in b, 0 if no
     * operation was performed.
     */
    private int difference(MutableBigInteger b) {
        MutableBigInteger a = this;
        int sign = a.compare(b);
        if (sign == 0)
            return 0;
        if (sign < 0) {
            MutableBigInteger tmp = a;
            a = b;
            b = tmp;
        }

        long diff = 0;
        int x = a.intLen;
        int y = b.intLen;

        // Subtract common parts of both numbers
        while (y > 0) {
            x--;
            y--;
            diff = (a.value[a.offset + x] & LONG_MASK) -
                (b.value[b.offset + y] & LONG_MASK) - ((int) -(diff >> 32));
            a.value[a.offset + x] = (int) diff;
        }
        // Subtract remainder of longer number
        while (x > 0) {
            x--;
            diff = (a.value[a.offset + x] & LONG_MASK) - ((int) -(diff >> 32));
            a.value[a.offset + x] = (int) diff;
        }

        a.normalize();
        return sign;
    }

    /**
     * Multiply the contents of two MutableBigInteger objects. The result is
     * placed into MutableBigInteger z. The contents of y are not changed.
     */
    public void multiply(MutableBigInteger y, MutableBigInteger z) {
        int xLen = intLen;
        int yLen = y.intLen;
        int newLen = xLen + yLen;

        // Put z into an appropriate state to receive product
        if (z.value.length < newLen)
            z.value = new int[newLen];
        z.offset = 0;
        z.intLen = newLen;

        // The first iteration is hoisted out of the loop to avoid extra add
        long carry = 0;
        for (int j = yLen - 1, k = yLen + xLen - 1; j >= 0; j--, k--) {
            long product = (y.value[j + y.offset] & LONG_MASK) *
                (value[xLen - 1 + offset] & LONG_MASK) + carry;
            z.value[k] = (int) product;
            carry = product >>> 32;
        }
        z.value[xLen - 1] = (int) carry;

        // Perform the multiplication word by word
        for (int i = xLen - 2; i >= 0; i--) {
            carry = 0;
            for (int j = yLen - 1, k = yLen + i; j >= 0; j--, k--) {
                long product = (y.value[j + y.offset] & LONG_MASK) *
                    (value[i + offset] & LONG_MASK) +
                    (z.value[k] & LONG_MASK) + carry;
                z.value[k] = (int) product;
                carry = product >>> 32;
            }
            z.value[i] = (int) carry;
        }

        // Remove leading zeros from product
        z.normalize();
    }

    /**
     * Multiply the contents of this MutableBigInteger by the word y. The
     * result is placed into z.
     */
    void mul(int y, MutableBigInteger z) {
        if (y == 1) {
            z.copyValue(this);
            return;
        }

        if (y == 0) {
            z.clear();
            return;
        }

        // Perform the multiplication word by word
        long ylong = y & LONG_MASK;
        int[] zval = (z.value.length < intLen + 1 ? new int[intLen + 1]
            : z.value);
        long carry = 0;
        for (int i = intLen - 1; i >= 0; i--) {
            long product = ylong * (value[i + offset] & LONG_MASK) + carry;
            zval[i + 1] = (int) product;
            carry = product >>> 32;
        }

        if (carry == 0) {
            z.offset = 1;
            z.intLen = intLen;
        } else {
            z.offset = 0;
            z.intLen = intLen + 1;
            zval[0] = (int) carry;
        }
        z.value = zval;
    }

    /**
     * This method is used for division of an n word dividend by a one word
     * divisor. The quotient is placed into quotient. The one word divisor is
     * specified by divisor.
     *
     * @return the remainder of the division is returned.
     */
    int divideOneWord(int divisor, MutableBigInteger quotient) {
        long divisorLong = divisor & LONG_MASK;

        // Special case of one word dividend
        if (intLen == 1) {
            long dividendValue = value[offset] & LONG_MASK;
            int q = (int) (dividendValue / divisorLong);
            int r = (int) (dividendValue - q * divisorLong);
            quotient.value[0] = q;
            quotient.intLen = (q == 0) ? 0 : 1;
            quotient.offset = 0;
            return r;
        }

        if (quotient.value.length < intLen)
            quotient.value = new int[intLen];
        quotient.offset = 0;
        quotient.intLen = intLen;

        // Normalize the divisor
        int shift = Integer.numberOfLeadingZeros(divisor);

        int rem = value[offset];
        long remLong = rem & LONG_MASK;
        if (remLong < divisorLong) {
            quotient.value[0] = 0;
        } else {
            quotient.value[0] = (int) (remLong / divisorLong);
            rem = (int) (remLong - (quotient.value[0] * divisorLong));
            remLong = rem & LONG_MASK;
        }
        int xlen = intLen;
        while (--xlen > 0) {
            long dividendEstimate = (remLong << 32) |
                (value[offset + intLen - xlen] & LONG_MASK);
            int q;
            if (dividendEstimate >= 0) {
                q = (int) (dividendEstimate / divisorLong);
                rem = (int) (dividendEstimate - q * divisorLong);
            } else {
                long tmp = divWord(dividendEstimate, divisor);
                q = (int) (tmp & LONG_MASK);
                rem = (int) (tmp >>> 32);
            }
            quotient.value[intLen - xlen] = q;
            remLong = rem & LONG_MASK;
        }

        quotient.normalize();
        // Unnormalize
        if (shift > 0)
            return rem % divisor;
        else
            return rem;
    }

    /**
     * Calculates the quotient of this div b and places the quotient in the
     * provided MutableBigInteger objects and the remainder object is returned.
     * <p>
     * Uses Algorithm D in Knuth section 4.3.1.
     * Many optimizations to that algorithm have been adapted from the Colin
     * Plumb C library.
     * It special cases one word divisors for speed. The content of b is not
     * changed.
     */
    public void divideKnuth(MutableBigInteger b, MutableBigInteger quotient, MutableBigInteger rem, int[] temporaryDivisor, boolean needRem) {
        if (b.intLen == 0)
            throw new ArithmeticException("BigInteger divide by zero");

        // Dividend is zero
        if (intLen == 0) {
            quotient.intLen = quotient.offset = 0;
            if (needRem)
                rem.reset();
            return;
        }

        int cmp = compare(b);
        // Dividend less than divisor
        if (cmp < 0) {
            quotient.intLen = quotient.offset = 0;
            if (needRem) {
                System.arraycopy(this.value, offset, rem.value, 0, intLen);
                rem.offset = 0;
                rem.intLen = intLen;
            }
            return;
        }
        // Dividend equal to divisor
        if (cmp == 0) {
            quotient.value[0] = quotient.intLen = 1;
            quotient.offset = 0;
            if (needRem)
                rem.reset();
            return;
        }

        quotient.clear();
        // Special case one word divisor
        if (b.intLen == 1) {
            int r = divideOneWord(b.value[b.offset], quotient);
            if (needRem) {
                if (r == 0) {
                    rem.reset();
                } else {
                    rem.value[0] = r;
                    rem.intLen = 1;
                    rem.offset = 0;
                }
            }
            return;
        }

        // Cancel common powers of two if we're above the KNUTH_POW2_* thresholds
        if (intLen >= KNUTH_POW2_THRESH_LEN) {
            int trailingZeroBits = Math.min(getLowestSetBit(), b.getLowestSetBit());
            if (trailingZeroBits >= KNUTH_POW2_THRESH_ZEROS * 32) {
                rightShift(trailingZeroBits);
                b.rightShift(trailingZeroBits);
                divideKnuth(b, quotient, rem, temporaryDivisor, needRem);
                rem.leftShift(trailingZeroBits);
                return;
            }
        }

        divideMagnitude(b, quotient, rem, temporaryDivisor, needRem);
    }

    /**
     * Returns a {@code MutableBigInteger} containing {@code blockLength} ints from
     * {@code this} number, starting at {@code index*blockLength}.<br/>
     * Used by Burnikel-Ziegler division.
     *
     * @param index       the block index
     * @param numBlocks   the total number of blocks in {@code this} number
     * @param blockLength length of one block in units of 32 bits
     * @return
     */
    private MutableBigInteger getBlock(int index, int numBlocks, int blockLength) {
        int blockStart = index * blockLength;
        if (blockStart >= intLen) {
            return new MutableBigInteger();
        }

        int blockEnd;
        if (index == numBlocks - 1) {
            blockEnd = intLen;
        } else {
            blockEnd = (index + 1) * blockLength;
        }
        if (blockEnd > intLen) {
            return new MutableBigInteger();
        }

        int[] newVal = Arrays.copyOfRange(value, offset + intLen - blockEnd, offset + intLen - blockStart);
        return new MutableBigInteger(newVal);
    }

    /**
     * @see BigInteger#bitLength()
     */
    long bitLength() {
        if (intLen == 0)
            return 0;
        return intLen * 32L - Integer.numberOfLeadingZeros(value[offset]);
    }

    /**
     * Internally used  to calculate the quotient of this div v and places the
     * quotient in the provided MutableBigInteger object and the remainder is
     * returned.
     *
     * @return the remainder of the division will be returned.
     */
    long divide(long v, MutableBigInteger quotient) {
        if (v == 0)
            throw new ArithmeticException("BigInteger divide by zero");

        // Dividend is zero
        if (intLen == 0) {
            quotient.intLen = quotient.offset = 0;
            return 0;
        }
        if (v < 0)
            v = -v;

        int d = (int) (v >>> 32);
        quotient.clear();
        // Special case on word divisor
        if (d == 0)
            return divideOneWord((int) v, quotient) & LONG_MASK;
        else {
            return divideLongMagnitude(v, quotient).toLong();
        }
    }

    /**
     * Divide this MutableBigInteger by the divisor.
     * The quotient will be placed into the provided quotient object &
     * the remainder object is returned.
     */
    private void divideMagnitude(MutableBigInteger div,
                                 MutableBigInteger quotient,
                                 final MutableBigInteger rem,
                                 int[] divisor,
                                 boolean needRem) {
        // assert div.intLen > 1
        // D1 normalize the divisor
        int shift = Integer.numberOfLeadingZeros(div.value[div.offset]);
        // Copy divisor value to protect divisor
        final int dlen = div.intLen;
        if (shift > 0) {
            copyAndShift(div.value, div.offset, dlen, divisor, 0, shift);
            if (Integer.numberOfLeadingZeros(value[offset]) >= shift) {
                int[] remarr = rem.value;
                rem.intLen = intLen;
                rem.offset = 1;
                copyAndShift(value, offset, intLen, remarr, 1, shift);
            } else {
                int[] remarr = rem.value;
                rem.intLen = intLen + 1;
                rem.offset = 1;
                int rFrom = offset;
                int c = 0;
                int n2 = 32 - shift;
                for (int i = 1; i < intLen + 1; i++, rFrom++) {
                    int b = c;
                    c = value[rFrom];
                    remarr[i] = (b << shift) | (c >>> n2);
                }
                remarr[intLen + 1] = c << shift;
            }
        } else {
            System.arraycopy(div.value, div.offset, divisor, 0, div.intLen);
            System.arraycopy(value, offset, rem.value, 1, intLen);
            rem.intLen = intLen;
            rem.offset = 1;
        }

        int nlen = rem.intLen;

        // Set the quotient size
        final int limit = nlen - dlen + 1;
        if (quotient.value.length < limit) {
            quotient.value = new int[limit];
            quotient.offset = 0;
        }
        quotient.intLen = limit;
        int[] q = quotient.value;


        // Must insert leading 0 in rem if its length did not change
        if (rem.intLen == nlen) {
            rem.offset = 0;
            rem.value[0] = 0;
            rem.intLen++;
        }

        int dh = divisor[0];
        long dhLong = dh & LONG_MASK;
        int dl = divisor[1];

        // D2 Initialize j
        for (int j = 0; j < limit - 1; j++) {
            // D3 Calculate qhat
            // estimate qhat
            int qhat = 0;
            int qrem = 0;
            boolean skipCorrection = false;
            int nh = rem.value[j + rem.offset];
            int nh2 = nh + 0x80000000;
            int nm = rem.value[j + 1 + rem.offset];

            if (nh == dh) {
                qhat = ~0;
                qrem = nh + nm;
                skipCorrection = qrem + 0x80000000 < nh2;
            } else {
                long nChunk = (((long) nh) << 32) | (nm & LONG_MASK);
                if (nChunk >= 0) {
                    qhat = (int) (nChunk / dhLong);
                    qrem = (int) (nChunk - (qhat * dhLong));
                } else {
                    long tmp = divWord(nChunk, dh);
                    qhat = (int) (tmp & LONG_MASK);
                    qrem = (int) (tmp >>> 32);
                }
            }

            if (qhat == 0)
                continue;

            if (!skipCorrection) { // Correct qhat
                long nl = rem.value[j + 2 + rem.offset] & LONG_MASK;
                long rs = ((qrem & LONG_MASK) << 32) | nl;
                long estProduct = (dl & LONG_MASK) * (qhat & LONG_MASK);

                if (unsignedLongCompare(estProduct, rs)) {
                    qhat--;
                    qrem = (int) ((qrem & LONG_MASK) + dhLong);
                    if ((qrem & LONG_MASK) >= dhLong) {
                        estProduct -= (dl & LONG_MASK);
                        rs = ((qrem & LONG_MASK) << 32) | nl;
                        if (unsignedLongCompare(estProduct, rs))
                            qhat--;
                    }
                }
            }

            // D4 Multiply and subtract
            rem.value[j + rem.offset] = 0;
            int borrow = mulsub(rem.value, divisor, qhat, dlen, j + rem.offset);

            // D5 Test remainder
            if (borrow + 0x80000000 > nh2) {
                // D6 Add back
                divadd(divisor, dlen, rem.value, j + 1 + rem.offset);
                qhat--;
            }

            // Store the quotient digit
            q[j] = qhat;
        } // D7 loop on j
        // D3 Calculate qhat
        // estimate qhat
        int qhat = 0;
        int qrem = 0;
        boolean skipCorrection = false;
        int nh = rem.value[limit - 1 + rem.offset];
        int nh2 = nh + 0x80000000;
        int nm = rem.value[limit + rem.offset];

        if (nh == dh) {
            qhat = ~0;
            qrem = nh + nm;
            skipCorrection = qrem + 0x80000000 < nh2;
        } else {
            long nChunk = (((long) nh) << 32) | (nm & LONG_MASK);
            if (nChunk >= 0) {
                qhat = (int) (nChunk / dhLong);
                qrem = (int) (nChunk - (qhat * dhLong));
            } else {
                long tmp = divWord(nChunk, dh);
                qhat = (int) (tmp & LONG_MASK);
                qrem = (int) (tmp >>> 32);
            }
        }
        if (qhat != 0) {
            if (!skipCorrection) { // Correct qhat
                long nl = rem.value[limit + 1 + rem.offset] & LONG_MASK;
                long rs = ((qrem & LONG_MASK) << 32) | nl;
                long estProduct = (dl & LONG_MASK) * (qhat & LONG_MASK);

                if (unsignedLongCompare(estProduct, rs)) {
                    qhat--;
                    qrem = (int) ((qrem & LONG_MASK) + dhLong);
                    if ((qrem & LONG_MASK) >= dhLong) {
                        estProduct -= (dl & LONG_MASK);
                        rs = ((qrem & LONG_MASK) << 32) | nl;
                        if (unsignedLongCompare(estProduct, rs))
                            qhat--;
                    }
                }
            }


            // D4 Multiply and subtract
            int borrow;
            rem.value[limit - 1 + rem.offset] = 0;
            if (needRem)
                borrow = mulsub(rem.value, divisor, qhat, dlen, limit - 1 + rem.offset);
            else
                borrow = mulsubBorrow(rem.value, divisor, qhat, dlen, limit - 1 + rem.offset);

            // D5 Test remainder
            if (borrow + 0x80000000 > nh2) {
                // D6 Add back
                if (needRem)
                    divadd(divisor, dlen, rem.value, limit - 1 + 1 + rem.offset);
                qhat--;
            }

            // Store the quotient digit
            q[(limit - 1)] = qhat;
        }


        if (needRem) {
            // D8 Unnormalize
            if (shift > 0)
                rem.rightShift(shift);
            rem.normalize();
        }
        quotient.normalize();
    }

    /**
     * Divide this MutableBigInteger by the divisor represented by positive long
     * value. The quotient will be placed into the provided quotient object &
     * the remainder object is returned.
     */
    private MutableBigInteger divideLongMagnitude(long ldivisor, MutableBigInteger quotient) {
        // Remainder starts as dividend with space for a leading zero
        MutableBigInteger rem = new MutableBigInteger(new int[intLen + 1]);
        System.arraycopy(value, offset, rem.value, 1, intLen);
        rem.intLen = intLen;
        rem.offset = 1;

        int nlen = rem.intLen;

        int limit = nlen - 2 + 1;
        if (quotient.value.length < limit) {
            quotient.value = new int[limit];
            quotient.offset = 0;
        }
        quotient.intLen = limit;
        int[] q = quotient.value;

        // D1 normalize the divisor
        int shift = Long.numberOfLeadingZeros(ldivisor);
        if (shift > 0) {
            ldivisor <<= shift;
            rem.leftShift(shift);
        }

        // Must insert leading 0 in rem if its length did not change
        if (rem.intLen == nlen) {
            rem.offset = 0;
            rem.value[0] = 0;
            rem.intLen++;
        }

        int dh = (int) (ldivisor >>> 32);
        long dhLong = dh & LONG_MASK;
        int dl = (int) (ldivisor & LONG_MASK);

        // D2 Initialize j
        for (int j = 0; j < limit; j++) {
            // D3 Calculate qhat
            // estimate qhat
            int qhat = 0;
            int qrem = 0;
            boolean skipCorrection = false;
            int nh = rem.value[j + rem.offset];
            int nh2 = nh + 0x80000000;
            int nm = rem.value[j + 1 + rem.offset];

            if (nh == dh) {
                qhat = ~0;
                qrem = nh + nm;
                skipCorrection = qrem + 0x80000000 < nh2;
            } else {
                long nChunk = (((long) nh) << 32) | (nm & LONG_MASK);
                if (nChunk >= 0) {
                    qhat = (int) (nChunk / dhLong);
                    qrem = (int) (nChunk - (qhat * dhLong));
                } else {
                    long tmp = divWord(nChunk, dh);
                    qhat = (int) (tmp & LONG_MASK);
                    qrem = (int) (tmp >>> 32);
                }
            }

            if (qhat == 0)
                continue;

            if (!skipCorrection) { // Correct qhat
                long nl = rem.value[j + 2 + rem.offset] & LONG_MASK;
                long rs = ((qrem & LONG_MASK) << 32) | nl;
                long estProduct = (dl & LONG_MASK) * (qhat & LONG_MASK);

                if (unsignedLongCompare(estProduct, rs)) {
                    qhat--;
                    qrem = (int) ((qrem & LONG_MASK) + dhLong);
                    if ((qrem & LONG_MASK) >= dhLong) {
                        estProduct -= (dl & LONG_MASK);
                        rs = ((qrem & LONG_MASK) << 32) | nl;
                        if (unsignedLongCompare(estProduct, rs))
                            qhat--;
                    }
                }
            }

            // D4 Multiply and subtract
            rem.value[j + rem.offset] = 0;
            int borrow = mulsubLong(rem.value, dh, dl, qhat, j + rem.offset);

            // D5 Test remainder
            if (borrow + 0x80000000 > nh2) {
                // D6 Add back
                divaddLong(dh, dl, rem.value, j + 1 + rem.offset);
                qhat--;
            }

            // Store the quotient digit
            q[j] = qhat;
        } // D7 loop on j

        // D8 Unnormalize
        if (shift > 0)
            rem.rightShift(shift);

        quotient.normalize();
        rem.normalize();
        return rem;
    }

    /**
     * A primitive used for division by long.
     * Specialized version of the method divadd.
     * dh is a high part of the divisor, dl is a low part
     */
    private int divaddLong(int dh, int dl, int[] result, int offset) {
        long carry = 0;

        long sum = (dl & LONG_MASK) + (result[1 + offset] & LONG_MASK);
        result[1 + offset] = (int) sum;

        sum = (dh & LONG_MASK) + (result[offset] & LONG_MASK) + carry;
        result[offset] = (int) sum;
        carry = sum >>> 32;
        return (int) carry;
    }

    /**
     * This method is used for division by long.
     * Specialized version of the method sulsub.
     * dh is a high part of the divisor, dl is a low part
     */
    private int mulsubLong(int[] q, int dh, int dl, int x, int offset) {
        long xLong = x & LONG_MASK;
        offset += 2;
        long product = (dl & LONG_MASK) * xLong;
        long difference = q[offset] - product;
        q[offset--] = (int) difference;
        long carry = (product >>> 32)
            + (((difference & LONG_MASK) >
            (((~(int) product) & LONG_MASK))) ? 1 : 0);
        product = (dh & LONG_MASK) * xLong + carry;
        difference = q[offset] - product;
        q[offset--] = (int) difference;
        carry = (product >>> 32)
            + (((difference & LONG_MASK) >
            (((~(int) product) & LONG_MASK))) ? 1 : 0);
        return (int) carry;
    }

    /**
     * Compare two longs as if they were unsigned.
     * Returns true iff one is bigger than two.
     */
    private boolean unsignedLongCompare(long one, long two) {
        return (one + Long.MIN_VALUE) > (two + Long.MIN_VALUE);
    }

    /**
     * Calculate GCD of this and v.
     * Assumes that this and v are not zero.
     */
    private MutableBigInteger binaryGCD(MutableBigInteger v) {
        // Algorithm B from Knuth section 4.5.2
        MutableBigInteger u = this;
        MutableBigInteger r = new MutableBigInteger();

        // step B1
        int s1 = u.getLowestSetBit();
        int s2 = v.getLowestSetBit();
        int k = (s1 < s2) ? s1 : s2;
        if (k != 0) {
            u.rightShift(k);
            v.rightShift(k);
        }

        // step B2
        boolean uOdd = (k == s1);
        MutableBigInteger t = uOdd ? v : u;
        int tsign = uOdd ? -1 : 1;

        int lb;
        while ((lb = t.getLowestSetBit()) >= 0) {
            // steps B3 and B4
            t.rightShift(lb);
            // step B5
            if (tsign > 0)
                u = t;
            else
                v = t;

            // Special case one word numbers
            if (u.intLen < 2 && v.intLen < 2) {
                int x = u.value[u.offset];
                int y = v.value[v.offset];
                x = binaryGcd(x, y);
                r.value[0] = x;
                r.intLen = 1;
                r.offset = 0;
                if (k > 0)
                    r.leftShift(k);
                return r;
            }

            // step B6
            if ((tsign = u.difference(v)) == 0)
                break;
            t = (tsign >= 0) ? u : v;
        }

        if (k > 0)
            u.leftShift(k);
        return u;
    }

    public void trim256() {
        if (intLen <= SLOT_SIZE)
            return;
        offset += intLen - SLOT_SIZE;
    }

}
