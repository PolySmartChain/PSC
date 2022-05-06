package org.tdf.sunflower.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.*;
import org.jetbrains.annotations.NotNull;
import org.tdf.common.types.Chained;
import org.tdf.common.util.EpochSecondDeserializer;
import org.tdf.common.util.EpochSecondsSerializer;
import org.tdf.common.util.HexBytes;
import org.tdf.common.util.IntSerializer;

@Getter
@ToString
@NoArgsConstructor
public class HeaderV1 implements Chained {
    public static HeaderV1 fromV2(Header header) {
        HeaderV1 r = new HeaderV1(
            1,
            header.getHashPrev(),
            header.getTransactionsRoot(),
            header.getStateRoot(),
            header.getHeight(),
            header.getCreatedAt(),
            header.getExtraData()
        );
        r.hash = header.getHash();
        return r;
    }

    /**
     * magic version number
     */
    protected int version;
    /**
     * hash of parent block
     */
    protected HexBytes hashPrev;
    /**
     * root hash of transaction trie
     */
    protected HexBytes transactionsRoot;
    /**
     * root hash of state trie
     */
    protected HexBytes stateRoot;
    /**
     * height of current header
     */
    @JsonSerialize(using = IntSerializer.class)
    protected long height;
    /**
     * unix epoch when the block mined
     */
    @JsonSerialize(using = EpochSecondsSerializer.class)
    @JsonDeserialize(using = EpochSecondDeserializer.class)
    protected long createdAt;
    /**
     * custom data
     */
    protected HexBytes payload;
    /**
     * hash of the block
     */
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    protected transient HexBytes hash;


    @Builder
    public HeaderV1(
        int version, HexBytes hashPrev, HexBytes transactionsRoot,
        HexBytes stateRoot, long height, long createdAt,
        HexBytes payload
    ) {
        this.version = version;
        this.hashPrev = hashPrev;
        this.transactionsRoot = transactionsRoot;
        this.stateRoot = stateRoot;
        this.height = height;
        this.createdAt = createdAt;
        this.payload = payload;
    }

    public HexBytes getHash() {
        return getHash(false);
    }

    private HexBytes getHash(boolean forceReHash) {
        return this.hash;
    }

    public void setVersion(int version) {
        this.version = version;
        getHash(true);
    }

    public void setHashPrev(HexBytes hashPrev) {
        this.hashPrev = hashPrev;
        getHash(true);
    }

    public void setTransactionsRoot(HexBytes transactionsRoot) {
        this.transactionsRoot = transactionsRoot;
        getHash(true);
    }

    public void setStateRoot(HexBytes stateRoot) {
        this.stateRoot = stateRoot;
        getHash(true);
    }

    public void setHeight(long height) {
        this.height = height;
        getHash(true);
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
        getHash(true);
    }

    public void setPayload(HexBytes payload) {
        this.payload = payload;
        getHash(true);
    }

    @Override
    public HeaderV1 clone() {
        return new HeaderV1(
            version, hashPrev, transactionsRoot, stateRoot,
            height, createdAt, payload
        );
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public int size() {
        return 0;
    }

    @Override
    public boolean isParentOf(@NotNull Chained another) {
        return false;
    }

    @Override
    public boolean isChildOf(@NotNull Chained another) {
        return false;
    }

    @Override
    public boolean isChildOf(@NotNull HexBytes hash) {
        return false;
    }
}

