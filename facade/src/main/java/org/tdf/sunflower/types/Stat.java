package org.tdf.sunflower.types;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tdf.common.types.Uint256;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Stat {
    // cpu usage
    private double cpu;

    // memory used (bytes)
    private long memoryUsed;

    // total memory (bytes)
    private long totalMemory;

    // the best block height
    private long height;

    // average block internal (last 10 block)
    private double averageBlockInterval;

    // average transaction fee (last
    private Uint256 averageGasPrice;

    private long transactionPoolSize;

    // if current consensus is pow, returns current difficulty
    private String currentDifficulty;

    // if is mining, the coin base exists in last 10 blocks
    private boolean mining;

    // blocks per day =
    private long blocksPerDay;

    // p2p net address
    private String p2pAddress;

    // genesis block file
    private JsonNode genesis;

    // hash algorithm
    private String hash;

    // Signature algorithm
    private String ec;

    // Symmetric encryption algorithm
    private String ae;

    // block speed
    private int blockInterval;

    // pos number of miners
    private int maxMiners;

    // pow Number of blocks per epoch
    private int blocksPerEra;

    // poa Number of blocks per epoch
    private boolean allowUnauthorized;

    // tds version
    @Builder.Default
    private String version = "v1.0.0";

    // consensus mechanism
    private String consensus;

    private long gasPrice;

    private boolean allowEmptyBlock;
}
