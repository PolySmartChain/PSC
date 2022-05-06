package org.tdf.sunflower;


import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.github.salpadding.rlpstream.annotation.RlpProps;
import org.tdf.common.types.Uint256;
import org.tdf.common.util.HashUtil;
import org.tdf.common.util.HexBytes;
import org.tdf.common.util.IntSerializer;

@RlpProps({"nonce", "balance", "storageRoot", "contractHash"})
public class AccountJ {
    public AccountJ() {

    }

    private long nonce;
    // the balance of account
    // for contract account, this field is zero
    @JsonSerialize(using = IntSerializer.class)
    private Uint256 balance;

    // root hash of contract db
    // if the account is not contract account, this field will be null
    private HexBytes storageRoot;

    // hash code of contract code
    // if the account contains none contract, contract hash will be null
    private HexBytes contractHash;


    public AccountJ(long nonce, Uint256 balance, HexBytes contractHash, HexBytes storageRoot) {
        this.nonce = nonce;
        this.balance = balance;
        this.contractHash = contractHash;
        this.storageRoot = storageRoot;
    }

    public static AccountJ emptyAccount(Uint256 balance) {
        return new AccountJ(0, balance, HashUtil.EMPTY_DATA_HASH_HEX, HashUtil.EMPTY_TRIE_HASH_HEX);
    }


    @Override
    public AccountJ clone() {
        return new AccountJ(nonce, balance, contractHash, storageRoot);
    }


    public void setNonce(long nonce) {
        this.nonce = nonce;
    }

    public void setBalance(Uint256 balance) {
        this.balance = balance;
    }

    public void setContractHash(HexBytes contractHash) {
        this.contractHash = contractHash;
    }

    public void setStorageRoot(HexBytes storageRoot) {
        this.storageRoot = storageRoot;
    }

    public long getNonce() {
        return nonce;
    }

    public Uint256 getBalance() {
        return balance;
    }

    public HexBytes getContractHash() {
        return contractHash;
    }

    public HexBytes getStorageRoot() {
        return storageRoot;
    }

    public boolean isEmpty() {
        return nonce == 0 && balance.isZero() && contractHash.equals(HashUtil.EMPTY_DATA_HASH_HEX)
                && storageRoot.equals(HashUtil.EMPTY_TRIE_HASH_HEX);
    }
}
