package org.tdf.sunflower.types;

import com.github.salpadding.rlpstream.*;
import com.github.salpadding.rlpstream.annotation.RlpCreator;
import org.tdf.common.util.HexBytes;

public class TransactionInfo1 implements RlpWritable {
    TransactionReceipt receipt;
    byte[] blockHash;
    // user for pending transaction
    byte[] parentBlockHash;
    int index;

    public Transaction transaction;

    public TransactionInfo1(TransactionReceipt receipt, byte[] blockHash, int index) {
        this.receipt = receipt;
        this.blockHash = blockHash;
        this.index = index;
    }

    /**
     * Creates a pending tx info
     */
    public TransactionInfo1(TransactionReceipt receipt) {
        this.receipt = receipt;
    }

    @RlpCreator
    public static TransactionInfo1 fromRlpStream(byte[] bin, long streamId) {
        return new TransactionInfo1(StreamId.rawOf(bin, streamId));
    }

    public TransactionInfo1(byte[] rlp) {
        RlpList txInfo = Rlp.decodeList(rlp);
        byte[] receiptRLP = txInfo.rawAt(0);

        blockHash = txInfo.bytesAt(1);
        index = Rlp.decodeInt(txInfo.rawAt(2));
        receipt = Rlp .decode(receiptRLP, TransactionReceipt.class);
    }


    /* [receipt, blockHash, index] */
    public byte[] getEncoded() {

        byte[] receiptRLP = this.receipt.getEncoded();
        byte[] blockHashRLP = Rlp.encodeBytes(blockHash);
        byte[] indexRLP = Rlp.encodeInt(index);

        return Rlp.encodeElements(receiptRLP, blockHashRLP, indexRLP);
    }

    public TransactionReceipt getReceipt() {
        return receipt;
    }

    public byte[] getBlockHash() {
        return blockHash;
    }


    public HexBytes getBlockHashHex() {
        return HexBytes.fromBytes(blockHash);
    }

    public byte[] getParentBlockHash() {
        return parentBlockHash;
    }

    public void setParentBlockHash(byte[] parentBlockHash) {
        this.parentBlockHash = parentBlockHash;
    }

    public int getIndex() {
        return index;
    }

    public boolean isPending() {
        return blockHash == null;
    }

    @Override
    public int writeToBuf(RlpBuffer rlpBuffer) {
        return rlpBuffer.writeRaw(getEncoded());
    }
}

