package org.tdf.sunflower.types;

import com.github.salpadding.rlpstream.Rlp;
import com.github.salpadding.rlpstream.RlpList;
import com.github.salpadding.rlpstream.StreamId;
import com.github.salpadding.rlpstream.annotation.RlpCreator;
import org.tdf.common.trie.Trie;
import org.tdf.common.trie.TrieImpl;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.BigIntegers;
import org.tdf.common.util.ByteUtil;
import org.tdf.common.util.HashUtil;
import org.tdf.common.util.HexBytes;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.tdf.common.util.ByteUtil.EMPTY_BYTE_ARRAY;
import static org.tdf.common.util.ByteUtil.toHexString;

/**
 * The transaction receipt is a tuple of three items
 * comprising the transaction, together with the post-transaction state,
 * and the cumulative gas used in the block containing the transaction receipt
 * as of immediately after the transaction has happened,
 */
public class TransactionReceipt1 {

    public static HexBytes calcReceiptsTrie(List<TransactionReceipt1> receipts) {
        Trie<byte[], byte[]> receiptsTrie = new TrieImpl<>();
        if (receipts == null || receipts.isEmpty())
            return HashUtil.EMPTY_TRIE_HASH_HEX;

        for (int i = 0; i < receipts.size(); i++) {
            receiptsTrie.set(Rlp.encodeInt(i), receipts.get(i).getReceiptTrieEncoded());
        }
        return receiptsTrie.commit();
    }


    private Transaction transaction;
    private byte[] postTxState = EMPTY_BYTE_ARRAY;
    private byte[] cumulativeGas = EMPTY_BYTE_ARRAY;
    private Bloom bloomFilter = new Bloom();
    private List<LogInfo> logInfoList = new ArrayList<>();
    private byte[] gasUsed = EMPTY_BYTE_ARRAY;
    private byte[] executionResult = EMPTY_BYTE_ARRAY;
    private String error = "";
    /* Tx Receipt in encoded form */
    private byte[] rlpEncoded;

    public TransactionReceipt1() {
    }

    @RlpCreator
    public static TransactionReceipt1 fromRlpStream(byte[] bin, long streamId) {
        RlpList li = StreamId.asList(bin, streamId, 7);
        return new TransactionReceipt1(li);
    }

    public TransactionReceipt1(RlpList receipt) {
        RlpList logs = receipt.listAt(3);


        postTxState = receipt.bytesAt(0);
        cumulativeGas = receipt.bytesAt(1);
        bloomFilter = new Bloom(receipt.bytesAt(2));
        gasUsed = receipt.bytesAt(4);

        executionResult = receipt.bytesAt(5);

        if (receipt.size() > 6) {
            byte[] errBytes = receipt.bytesAt(6);
            error = errBytes != null ? new String(errBytes, StandardCharsets.UTF_8) : "";
        }

        for(int i = 0; i < logs.size(); i++) {
            LogInfo logInfo = logs.valueAt(i, LogInfo.class);
            logInfoList.add(logInfo);

        }
        rlpEncoded = receipt.getEncoded();
    }


    public TransactionReceipt1(byte[] rlp) {
        this(Rlp.decodeList(rlp));
    }

    public TransactionReceipt1(byte[] cumulativeGas,
                              Bloom bloomFilter, List<LogInfo> logInfoList) {
        setTxStatus(true);
        this.cumulativeGas = cumulativeGas;
        this.bloomFilter = bloomFilter;
        this.logInfoList = logInfoList;
    }

    public byte[] getPostTxState() {
        return postTxState;
    }

    public void setPostTxState(byte[] postTxState) {
        this.postTxState = postTxState;
        rlpEncoded = null;
    }

    public byte[] getCumulativeGas() {
        return cumulativeGas;
    }

    public void setCumulativeGas(long cumulativeGas) {
        this.cumulativeGas = BigIntegers.asUnsignedByteArray(BigInteger.valueOf(cumulativeGas));
        rlpEncoded = null;
    }

    public void setCumulativeGas(byte[] cumulativeGas) {
        this.cumulativeGas = cumulativeGas;
        rlpEncoded = null;
    }

    public byte[] getGasUsed() {
        return gasUsed;
    }

    public Uint256 getGasUsedAsU256() {
        return Uint256.of(getGasUsed());
    }

    public void setGasUsed(byte[] gasUsed) {
        this.gasUsed = gasUsed;
        rlpEncoded = null;
    }

    public void setGasUsed(long gasUsed) {
        this.gasUsed = BigIntegers.asUnsignedByteArray(BigInteger.valueOf(gasUsed));
        rlpEncoded = null;
    }

    public byte[] getExecutionResult() {
        return executionResult;
    }

    public void setExecutionResult(byte[] executionResult) {
        this.executionResult = executionResult;
        rlpEncoded = null;
    }

    public long getCumulativeGasLong() {
        return new BigInteger(1, cumulativeGas).longValue();
    }

    public Bloom getBloomFilter() {
        return bloomFilter;
    }

    public List<LogInfo> getLogInfoList() {
        return logInfoList;
    }

    public void setLogInfoList(List<LogInfo> logInfoList) {
        if (logInfoList == null) return;
        this.logInfoList = logInfoList;

        for (LogInfo loginfo : logInfoList) {
            bloomFilter.or(loginfo.getBloom());
        }
        rlpEncoded = null;
    }

    public boolean isValid() {
        return byteArrayToLong(gasUsed) > 0;
    }

    public static long byteArrayToLong(byte[] b) {
        if (b == null || b.length == 0)
            return 0;
        return new BigInteger(1, b).longValue();
    }


    public boolean isSuccessful() {
        return error.isEmpty();
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error == null ? "" : error;
    }

    /**
     * Used for Receipt trie hash calculation. Should contain only the following items encoded:
     * [postTxState, cumulativeGas, bloomFilter, logInfoList]
     */
    public byte[] getReceiptTrieEncoded() {
        return getEncoded(true);
    }


    /**
     * Used for serialization, contains all the receipt data encoded
     */
    public byte[] getEncoded() {
        if (rlpEncoded == null) {
            rlpEncoded = getEncoded(false);
        }

        return rlpEncoded;
    }

    public byte[] getEncoded(boolean receiptTrie) {
        byte[] postTxStateRLP = Rlp.encodeBytes(this.postTxState);
        byte[] cumulativeGasRLP = Rlp.encodeBytes(this.cumulativeGas);
        byte[] bloomRLP = Rlp.encodeBytes(this.bloomFilter.getData());

        final byte[] logInfoListRLP;
        if (logInfoList != null) {
            byte[][] logInfoListE = new byte[logInfoList.size()][];

            int i = 0;
            for (LogInfo logInfo : logInfoList) {
                logInfoListE[i] = logInfo.getEncoded();
                ++i;
            }
            logInfoListRLP = Rlp.encodeElements(logInfoListE);
        } else {
            logInfoListRLP = Rlp.encodeElements(Collections.emptyList());
        }

        return receiptTrie ?
            Rlp.encodeElements(postTxStateRLP, cumulativeGasRLP, bloomRLP, logInfoListRLP) :
            Rlp.encodeElements(
                    postTxStateRLP, cumulativeGasRLP, bloomRLP, logInfoListRLP,
                    Rlp.encodeBytes(gasUsed), Rlp.encodeBytes(executionResult),
                    Rlp.encodeBytes(error.getBytes(StandardCharsets.UTF_8))

            );

    }

    public void setTxStatus(boolean success) {
        this.postTxState = success ? new byte[]{1} : new byte[0];
        rlpEncoded = null;
    }

    public boolean hasTxStatus() {
        return postTxState != null && postTxState.length <= 1;
    }

    public boolean isTxStatusOK() {
        return postTxState != null && postTxState.length == 1 && postTxState[0] == 1;
    }

    public Transaction getTransaction() {
        if (transaction == null)
            throw new NullPointerException("Transaction is not initialized. Use TransactionInfo and BlockStore to setup Transaction instance");
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    @Override
    public String toString() {

        // todo: fix that

        return "TransactionReceipt[" +
            "\n  , " + (hasTxStatus() ? ("txStatus=" + (isTxStatusOK() ? "OK" : "FAILED"))
            : ("postTxState=" + toHexString(postTxState))) +
            "\n  , cumulativeGas=" + toHexString(cumulativeGas) +
            "\n  , gasUsed=" + toHexString(gasUsed) +
            "\n  , error=" + error +
            "\n  , executionResult=" + toHexString(executionResult) +
            "\n  , bloom=" + bloomFilter.toString() +
            "\n  , logs=" + logInfoList +
            ']';
    }

}