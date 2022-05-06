package org.tdf.sunflower.types;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.tdf.common.types.Chained;
import org.tdf.common.util.EpochSecondDeserializer;
import org.tdf.common.util.EpochSecondsSerializer;
import org.tdf.common.util.HexBytes;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BlockV1 implements Chained {
    public static BlockV1 fromV2(Block b) {
        BlockV1 r = new BlockV1(HeaderV1.fromV2(b.getHeader()));
        r.body = b.getBody().stream().map(TransactionV1::fromV2).collect(Collectors.toList());
        return r;
    }

    // extend from header
    @Getter
    @JsonIgnore
    protected HeaderV1 header;


    @Getter
    @Setter
    protected List<TransactionV1> body;

    public BlockV1() {
        header = new HeaderV1();
        body = new ArrayList<>();
    }

    public BlockV1(@NonNull HeaderV1 header) {
        this.header = header;
        body = new ArrayList<>();
    }

    @JsonSerialize(using = EpochSecondsSerializer.class)
    @JsonDeserialize(using = EpochSecondDeserializer.class)
    public long getCreatedAt() {
        return header.getCreatedAt();
    }

    public void setCreatedAt(long createdAt) {
        header.setCreatedAt(createdAt);
    }


    // serialization only
    @JsonProperty(access = JsonProperty.Access.READ_ONLY, value = "size")
    public int size() {
        return 0;
    }

    public int getVersion() {
        return header.getVersion();
    }

    public void setVersion(int version) {
        header.setVersion(version);
    }

    public HexBytes getHashPrev() {
        return header.getHashPrev();
    }

    public void setHashPrev(HexBytes hashPrev) {
        header.setHashPrev(hashPrev);
    }

    public HexBytes getTransactionsRoot() {
        return header.getTransactionsRoot();
    }

    public void setTransactionsRoot(HexBytes transactionsRoot) {
        header.setTransactionsRoot(transactionsRoot);
    }


    public HexBytes getStateRoot() {
        return header.getStateRoot();
    }

    public void setStateRoot(HexBytes stateRoot) {
        header.setStateRoot(stateRoot);
    }

    public long getHeight() {
        return header.getHeight();
    }

    public void setHeight(long height) {
        header.setHeight(height);
    }

    public HexBytes getPayload() {
        return header.getPayload();
    }

    public void setPayload(HexBytes payload) {
        header.setPayload(payload);
    }

    public HexBytes getHash() {
        return header.getHash();
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
