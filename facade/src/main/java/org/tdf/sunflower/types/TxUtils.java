package org.tdf.sunflower.types;

import com.github.salpadding.rlpstream.Rlp;
import org.tdf.common.crypto.ECDSASignature;
import org.tdf.common.util.BigIntegers;

import java.math.BigInteger;

import static org.tdf.common.util.ByteUtil.EMPTY_BYTE_ARRAY;
import static org.tdf.sunflower.types.Transaction.CHAIN_ID_INC;
import static org.tdf.sunflower.types.Transaction.LOWER_REAL_V;

public final class TxUtils {
    private TxUtils() {}

    static byte getRealV(BigInteger bv) {
        if (bv.bitLength() > 31) return 0; // chainId is limited to 31 bits, longer are not valid for now
        long v = bv.longValue();
        if (v == LOWER_REAL_V || v == (LOWER_REAL_V + 1)) return (byte) v;
        byte realV = LOWER_REAL_V;
        int inc = 0;
        if ((int) v % 2 == 0) inc = 1;
        return (byte) (realV + inc);
    }

    static Integer extractChainIdFromRawSignature(BigInteger bv, byte[] r, byte[] s) {
        if (r == null && s == null) return bv.intValue();  // EIP 86
        if (bv.bitLength() > 31)
            return Integer.MAX_VALUE; // chainId is limited to 31 bits, longer are not valid for now
        long v = bv.longValue();
        if (v == LOWER_REAL_V || v == (LOWER_REAL_V + 1)) return null;
        return (int) ((v - CHAIN_ID_INC) / 2);
    }

    static int getV(Integer chainId, ECDSASignature signature) {
        if (signature != null) {
            int encodeV;
            if (chainId == null) {
                encodeV = signature.v;
            } else {
                encodeV = signature.v - LOWER_REAL_V;
                encodeV += chainId * 2 + CHAIN_ID_INC;
            }
            return encodeV;
        } else {
            // Since EIP-155 use chainId for v
            return chainId == null ? 0 : chainId;
        }
    }
}
