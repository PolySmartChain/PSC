package org.tdf.common.util;

import com.github.salpadding.rlpstream.Rlp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.Digest;
import org.spongycastle.crypto.digests.RIPEMD160Digest;
import org.tdf.common.crypto.jce.SpongyCastleProvider;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.util.Collections;

import static java.util.Arrays.copyOfRange;
import static org.tdf.common.util.ByteUtil.EMPTY_BYTE_ARRAY;

public class HashUtil {

    public static final byte[] EMPTY_DATA_HASH;
    public static final byte[] EMPTY_LIST_HASH;
    public static final byte[] EMPTY_TRIE_HASH;
    public static final HexBytes EMPTY_TRIE_HASH_HEX;
    public static final HexBytes EMPTY_DATA_HASH_HEX;


    private static final Logger LOG = LoggerFactory.getLogger(HashUtil.class);
    private static final Provider CRYPTO_PROVIDER;

    private static final String HASH_256_ALGORITHM_NAME;
    private static final String HASH_512_ALGORITHM_NAME;


    static {
        Security.addProvider(SpongyCastleProvider.getInstance());
        CRYPTO_PROVIDER = Security.getProvider("SC");
        HASH_256_ALGORITHM_NAME = "ETH-KECCAK-256";
        HASH_512_ALGORITHM_NAME = "ETH-KECCAK-512";
        EMPTY_DATA_HASH = sha3(EMPTY_BYTE_ARRAY);
        EMPTY_LIST_HASH = sha3(Rlp.encodeElements(Collections.emptyList()));
        EMPTY_TRIE_HASH = sha3(Rlp.encodeBytes(EMPTY_BYTE_ARRAY));
        EMPTY_TRIE_HASH_HEX = HexBytes.fromBytes(EMPTY_TRIE_HASH);
        EMPTY_DATA_HASH_HEX = HexBytes.fromBytes(EMPTY_DATA_HASH);
    }

    /**
     * @param input
     *            - data for hashing
     * @return - sha256 hash of the data
     */
    public static byte[] sha256(byte[] input) {
        try {
            MessageDigest sha256digest = MessageDigest.getInstance("SHA-256");
            return sha256digest.digest(input);
        } catch (NoSuchAlgorithmException e) {
            LOG.error("Can't find such algorithm", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * @param data
     *            - message to hash
     * @return - reipmd160 hash of the message
     */
    public static byte[] ripemd160(byte[] data) {
        Digest digest = new RIPEMD160Digest();
        if (data != null) {
            byte[] resBuf = new byte[digest.getDigestSize()];
            digest.update(data, 0, data.length);
            digest.doFinal(resBuf, 0);
            return resBuf;
        }
        throw new NullPointerException("Can't hash a NULL value");
    }

    public static byte[] sha3(byte[] input) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(HASH_256_ALGORITHM_NAME, CRYPTO_PROVIDER);
            digest.update(input);
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            LOG.error("Can't find such algorithm", e);
            throw new RuntimeException(e);
        }
    }

    public static byte[] sha3(byte[] input1, byte[] input2) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(HASH_256_ALGORITHM_NAME, CRYPTO_PROVIDER);
            digest.update(input1, 0, input1.length);
            digest.update(input2, 0, input2.length);
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            LOG.error("Can't find such algorithm", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * hashing chunk of the data
     *
     * @param input  - data for hash
     * @param start  - start of hashing chunk
     * @param length - length of hashing chunk
     * @return - keccak hash of the chunk
     */
    public static byte[] sha3(byte[] input, int start, int length) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(HASH_256_ALGORITHM_NAME, CRYPTO_PROVIDER);
            digest.update(input, start, length);
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            LOG.error("Can't find such algorithm", e);
            throw new RuntimeException(e);
        }
    }


    public static void sha3(byte[] input, int start, int length, byte[] dst, int dstOff) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(HASH_256_ALGORITHM_NAME, CRYPTO_PROVIDER);
            digest.update(input, start, length);
            digest.digest(dst, dstOff, dst.length);
        } catch (Exception e) {
            LOG.error("Exception: ", e);
            throw new RuntimeException(e);
        }
    }

    public static byte[] sha512(byte[] input) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(HASH_512_ALGORITHM_NAME, CRYPTO_PROVIDER);
            digest.update(input);
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            LOG.error("Can't find such algorithm", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Calculates RIGTMOST160(SHA3(input)). This is used in address
     * calculations. *
     *
     * @param input - data
     * @return - 20 right bytes of the hash keccak of the data
     */
    public static byte[] sha3omit12(byte[] input) {
        byte[] hash = sha3(input);
        return copyOfRange(hash, 12, hash.length);
    }

    /**
     * The way to calculate new address inside ethereum
     *
     * @param addr  - creating address
     * @param nonce - nonce of creating address
     * @return new address
     */
    public static byte[] calcNewAddr(byte[] addr, byte[] nonce) {

        byte[] encSender = Rlp.encodeBytes(addr);
        byte[] encNonce = Rlp.encodeBigInteger(new BigInteger(1, nonce));

        return sha3omit12(Rlp.encodeElements(encSender, encNonce));
    }


    /**
     * The way to calculate new address inside ethereum for {@link org.ethereum.vm.OpCode#CREATE2}
     * sha3(0xff ++ msg.sender ++ salt ++ sha3(init_code)))[12:]
     *
     * @param senderAddr - creating address
     * @param initCode   - contract init code
     * @param salt       - salt to make different result addresses
     * @return new address
     */
    public static byte[] calcSaltAddr(byte[] senderAddr, byte[] initCode, byte[] salt) {
        // 1 - 0xff length, 32 bytes - keccak-256
        byte[] data = new byte[1 + senderAddr.length + salt.length + 32];
        data[0] = (byte) 0xff;
        int currentOffset = 1;
        System.arraycopy(senderAddr, 0, data, currentOffset, senderAddr.length);
        currentOffset += senderAddr.length;
        System.arraycopy(salt, 0, data, currentOffset, salt.length);
        currentOffset += salt.length;
        byte[] sha3InitCode = sha3(initCode);
        System.arraycopy(sha3InitCode, 0, data, currentOffset, sha3InitCode.length);

        return sha3omit12(data);
    }

}
