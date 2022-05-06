package org.tdf.sunflower.types;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.github.salpadding.rlpstream.Rlp;
import lombok.*;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.EpochSecondDeserializer;
import org.tdf.common.util.EpochSecondsSerializer;
import org.tdf.common.util.HexBytes;
import org.tdf.common.util.IntSerializer;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@ToString
@NoArgsConstructor
public class TransactionV1 {
    public static final long TRANSFER_GAS = 10;
    public static final long BUILTIN_CALL_GAS = 10;

    public static final HexBytes EMPTY_SIG = HexBytes.fromBytes(new byte[64]);

    public static TransactionV1 fromV2(Transaction t) {
        TransactionV1 r = new TransactionV1(
            1,
            t.getTo().isEmpty() ? Type.CONTRACT_DEPLOY.code : (t.getData().isEmpty() ? Type.TRANSFER.code : Type.CONTRACT_CALL.code),
            System.currentTimeMillis() / 1000,
            t.getNonce(),
            t.getSender(),
            t.getGasLimit(),
            t.getGasPrice(),
            t.getValue(),
            t.getData(),
            t.getTo(),
            EMPTY_SIG
        );
        r.hash = t.getHash();
        return r;
    }


    protected int version;
    protected int type;
    @JsonSerialize(using = EpochSecondsSerializer.class)
    @JsonDeserialize(using = EpochSecondDeserializer.class)
    protected long createdAt;
    @JsonSerialize(using = IntSerializer.class)
    protected long nonce;
    /**
     * for coinbase, this field is null or empty bytes
     */
    protected HexBytes from;
    @JsonSerialize(using = IntSerializer.class)
    protected long gasLimit;
    @JsonSerialize(using = IntSerializer.class)
    protected Uint256 gasPrice;
    @JsonSerialize(using = IntSerializer.class)
    protected Uint256 amount;
    /**
     * for coinbase and transfer, this field is null or empty bytes
     */
    protected HexBytes payload;
    /**
     * for contract deploy, this field is null or empty bytes
     */
    protected HexBytes to;
    /**
     * not null
     */
    protected HexBytes signature;
    // generated value, no need to encode into rlp
    @Getter(AccessLevel.NONE)
    protected transient HexBytes hash;

    @Builder
    public TransactionV1(
        int version,
        int type, long createdAt, long nonce, HexBytes from,
        long gasLimit,
        Uint256 gasPrice, Uint256 amount, HexBytes payload,
        HexBytes to, HexBytes signature
    ) {
        this.version = version;
        this.type = type;
        this.createdAt = createdAt;
        this.nonce = nonce;
        this.from = from;
        this.gasPrice = gasPrice;
        this.amount = amount;
        this.payload = payload;
        this.to = to;
        this.signature = signature;
        this.gasLimit = gasLimit;
    }


    public long getGasLimit() {
        return gasLimit;
    }

    public void setGasLimit(long gasLimit) {
        this.gasLimit = gasLimit;
        getHash(true);
    }

    @JsonIgnore
    public byte[] getSignaturePlain() {
        return Rlp.encode(new Object[]{
            version,
            type,
            createdAt,
            nonce,
            from,
            gasLimit,
            gasPrice,
            amount,
            payload,
            to
        });
    }

    public HexBytes getHash() {
        return getHash(false);
    }

    private HexBytes getHash(boolean forceReHash) {
        return this.hash;
    }

    @Override
    public TransactionV1 clone() {
        return new TransactionV1(version, type, createdAt, nonce, from, gasLimit, gasPrice, amount, payload, to, signature);
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public int size() {
        return 0;
    }


    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public Uint256 getFee() {
        if (type == Type.COIN_BASE.code)
            return Uint256.ZERO;
        if (type == Type.TRANSFER.code)
            return Uint256.of(TRANSFER_GAS).times(getGasPrice());
        return Uint256.ZERO;
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, type, createdAt, nonce, from, gasPrice, amount, payload, to, signature);
    }


    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
        getHash(true);
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
        getHash(true);
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
        getHash(true);
    }

    public long getNonce() {
        return nonce;
    }

    public void setNonce(long nonce) {
        this.nonce = nonce;
        getHash(true);
    }

    public HexBytes getFrom() {
        return from == null ? HexBytes.EMPTY : from;
    }

    public void setFrom(HexBytes from) {
        this.from = from;
        getHash(true);
    }

    @JsonIgnore
    public HexBytes getFromAddress() {
        return getFrom();
    }

    public Uint256 getGasPrice() {
        return gasPrice;
    }

    public void setGasPrice(Uint256 gasPrice) {
        this.gasPrice = gasPrice;
        getHash(true);
    }

    public Uint256 getAmount() {
        return amount;
    }

    public void setAmount(Uint256 amount) {
        this.amount = amount;
        getHash(true);
    }

    public HexBytes getPayload() {
        return payload == null ? HexBytes.EMPTY : payload;
    }

    public void setPayload(HexBytes payload) {
        this.payload = payload;
        getHash(true);
    }

    public HexBytes getTo() {
        return to == null ? HexBytes.EMPTY : to;
    }

    public void setTo(HexBytes to) {
        this.to = to;
        getHash(true);
    }

    public HexBytes getSignature() {
        return signature == null ? HexBytes.EMPTY : signature;
    }

    public void setSignature(HexBytes signature) {
        this.signature = signature;
        getHash(true);
    }

    public enum Status {
        PENDING,
        INCLUDED,
        CONFIRMED,
        DROPPED
    }

    public enum Type {
        // coinbase transaction has code 0, may trigger bios
        COIN_BASE(0x00),
        // the amount is transferred from sender to recipient
        // if type is transfer, payload is null
        // and fee is a constant
        TRANSFER(0x01),
        // if type is contract deploy, payload is wasm binary module
        // fee = gasPrice * gasUsage
        CONTRACT_DEPLOY(0x02),
        // if type is contract call, payload = gasLimit(little endian, 8 bytes) +
        // method name length (an unsigned byte) +
        // method name(ascii string, [_a-zA-Z][_a-zA-Z0-9]*) +
        // custom parameters, could be load by Parameters.load() in contract
        // fee = gasPrice * gasUsage
        // e.g.
        /**
         * const parameter: Parameters = Parameters.load();
         * const customData: Uint8Array = parameter.raw();
         */
        CONTRACT_CALL(0x03);
        public static final Map<Integer, Type> TYPE_MAP =
            Arrays.stream(values()).collect(Collectors.toMap(x -> x.code, Function.identity()));
        public final int code;

        Type(int code) {
            this.code = code;
        }
    }
}

