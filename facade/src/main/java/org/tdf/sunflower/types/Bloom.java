package org.tdf.sunflower.types;

import org.tdf.common.util.ByteUtil;
import org.tdf.common.util.HexBytes;

import java.util.Arrays;

public class Bloom {
    public static final HexBytes EMPTY = HexBytes.fromBytes(new byte[256]);
    public static final long MEM_SIZE = 256 + 16;

    final static int _8STEPS = 8;
    final static int _3LOW_BITS = 7;
    final static int ENSURE_BYTE = 255;

    private final byte[] data;


    public Bloom() {
        this(new byte[256]);
    }

    public Bloom(byte[] data) {
        this.data = data;
    }

    public static Bloom create(byte[] toBloom) {

        int mov1 = (((toBloom[0] & ENSURE_BYTE) & (_3LOW_BITS)) << _8STEPS) + ((toBloom[1]) & ENSURE_BYTE);
        int mov2 = (((toBloom[2] & ENSURE_BYTE) & (_3LOW_BITS)) << _8STEPS) + ((toBloom[3]) & ENSURE_BYTE);
        int mov3 = (((toBloom[4] & ENSURE_BYTE) & (_3LOW_BITS)) << _8STEPS) + ((toBloom[5]) & ENSURE_BYTE);

        byte[] data = new byte[256];
        Bloom bloom = new Bloom(data);

        ByteUtil.setBit(data, mov1, 1);
        ByteUtil.setBit(data, mov2, 1);
        ByteUtil.setBit(data, mov3, 1);

        return bloom;
    }

    public void or(Bloom bloom) {
        for (int i = 0; i < data.length; ++i) {
            data[i] |= bloom.data[i];
        }
    }

    // this or other = this <=> other <= this
    public boolean matches(Bloom topicBloom) {
        Bloom copied = copy();
        copied.or(topicBloom);
        return this.equals(copied);
    }

    // x | y = y => y contains x
    public boolean belongsTo(Bloom other) {
        for (int i = 0; i < data.length; i++) {
            int x = data[i] & 0xff;
            int y = other.data[i] & 0xff;
            if ((x | y) != y)
                return false;
        }
        return true;
    }

    public byte[] getData() {
        return data;
    }

    public Bloom copy() {
        return new Bloom(Arrays.copyOf(getData(), getData().length));
    }

    @Override
    public String toString() {
        return ByteUtil.toHexString(data);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Bloom bloom = (Bloom) o;

        return Arrays.equals(data, bloom.data);

    }

    @Override
    public int hashCode() {
        return data != null ? Arrays.hashCode(data) : 0;
    }
}
