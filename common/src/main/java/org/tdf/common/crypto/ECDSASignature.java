package org.tdf.common.crypto;

import org.spongycastle.asn1.ASN1InputStream;
import org.spongycastle.asn1.ASN1Integer;
import org.spongycastle.asn1.DLSequence;
import org.spongycastle.util.encoders.Base64;
import org.spongycastle.util.encoders.Hex;
import org.tdf.common.util.ByteUtil;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.Charset;

import static org.tdf.common.util.BIUtil.isLessThan;
import static org.tdf.common.util.ByteUtil.bigIntegerToBytes;

/**
 * Groups the two components that make up a signature, and provides a way to encode to Base64 form, which is
 * how ECDSA signatures are represented when embedded in other data structures in the Ethereum protocol. The raw
 * components can be useful for doing further EC maths on them.
 */
public class ECDSASignature {
    /**
     * The two components of the signature.
     */
    public final BigInteger r, s;
    public byte v;

    /**
     * Constructs a signature with the given components. Does NOT automatically canonicalise the signature.
     *
     * @param r -
     * @param s -
     */
    public ECDSASignature(BigInteger r, BigInteger s) {
        this.r = r;
        this.s = s;
    }

    /**
     *t
     * @param r
     * @param s
     * @return -
     */
    private static ECDSASignature fromComponents(byte[] r, byte[] s) {
        return new ECDSASignature(new BigInteger(1, r), new BigInteger(1, s));
    }

    /**
     *
     * @param r -
     * @param s -
     * @param v -
     * @return -
     */
    public static ECDSASignature fromComponents(byte[] r, byte[] s, byte v) {
        ECDSASignature signature = fromComponents(r, s);
        signature.v = v;
        return signature;
    }

    public boolean validateComponents() {
        return validateComponents(r, s, v);
    }

    public static boolean validateComponents(BigInteger r, BigInteger s, byte v) {

        if (v != 27 && v != 28) return false;

        if (isLessThan(r, BigInteger.ONE)) return false;
        if (isLessThan(s, BigInteger.ONE)) return false;

        if (!isLessThan(r, ECKey.SECP256K1N)) return false;
        if (!isLessThan(s, ECKey.SECP256K1N)) return false;

        return true;
    }

    public static ECDSASignature decodeFromDER(byte[] bytes) {
        ASN1InputStream decoder = null;
        try {
            decoder = new ASN1InputStream(bytes);
            DLSequence seq = (DLSequence) decoder.readObject();
            if (seq == null)
                throw new RuntimeException("Reached past end of ASN.1 stream.");
            ASN1Integer r, s;
            try {
                r = (ASN1Integer) seq.getObjectAt(0);
                s = (ASN1Integer) seq.getObjectAt(1);
            } catch (ClassCastException e) {
                throw new IllegalArgumentException(e);
            }
            // OpenSSL deviates from the DER spec by interpreting these values as unsigned, though they should not be
            // Thus, we always use the positive versions. See: http://r6.ca/blog/20111119T211504Z.html
            return new ECDSASignature(r.getPositiveValue(), s.getPositiveValue());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (decoder != null)
                try { decoder.close(); } catch (IOException x) {}
        }
    }

    /**
     * Will automatically adjust the S component to be less than or equal to half the curve order, if necessary.
     * This is required because for every signature (r,s) the signature (r, -s (mod N)) is a valid signature of
     * the same message. However, we dislike the ability to modify the bits of a Ethereum transaction after it's
     * been signed, as that violates various assumed invariants. Thus in future only one of those forms will be
     * considered legal and the other will be banned.
     *
     * @return  -
     */
    public ECDSASignature toCanonicalised() {
        if (s.compareTo(ECKey.HALF_CURVE_ORDER) > 0) {
            // The order of the curve is the number of valid points that exist on that curve. If S is in the upper
            // half of the number of valid points, then bring it back to the lower half. Otherwise, imagine that
            //    N = 10
            //    s = 8, so (-8 % 10 == 2) thus both (r, 8) and (r, 2) are valid solutions.
            //    10 - 8 == 2, giving us always the latter solution, which is canonical.
            return new ECDSASignature(r, ECKey.CURVE.getN().subtract(s));
        } else {
            return this;
        }
    }

    /**
     *
     * @return -
     */
    public String toBase64() {
        byte[] sigData = new byte[65];  // 1 header + 32 bytes for R + 32 bytes for S
        sigData[0] = v;
        System.arraycopy(bigIntegerToBytes(this.r, 32), 0, sigData, 1, 32);
        System.arraycopy(bigIntegerToBytes(this.s, 32), 0, sigData, 33, 32);
        return new String(Base64.encode(sigData), Charset.forName("UTF-8"));
    }

    public byte[] toByteArray() {
        final byte fixedV = this.v >= 27
            ? (byte) (this.v - 27)
            :this.v;

        return ByteUtil.merge(
            ByteUtil.bigIntegerToBytes(this.r, 32),
            ByteUtil.bigIntegerToBytes(this.s, 32),
            new byte[]{fixedV});
    }

    public String toHex() {
        return Hex.toHexString(toByteArray());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ECDSASignature signature = (ECDSASignature) o;

        if (!r.equals(signature.r)) return false;
        if (!s.equals(signature.s)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = r.hashCode();
        result = 31 * result + s.hashCode();
        return result;
    }
}
