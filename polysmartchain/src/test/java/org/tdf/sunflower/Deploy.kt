package org.tdf.sunflower

import com.google.common.io.ByteStreams
import org.tdf.common.crypto.ECKey
import org.tdf.common.util.HexBytes
import org.tdf.sunflower.controller.jsonHex
import org.tdf.sunflower.vm.abi.Abi
import org.web3j.crypto.Credentials
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.http.HttpService
import java.io.FileInputStream
import java.math.BigInteger


const val path = "C:\\Users\\sal\\Desktop\\dev\\inlined.wasm"
const val abi = """
[
    {
        "type": "constructor",
        "inputs": [],
        "outputs": [
            {
                "type": "address"
            }
        ],
        "stateMutability": "payable"
    },
    {
        "name": "updateCode",
        "type": "function",
        "inputs": [
            {
                "name": "code",
                "type": "bytes"
            }
        ],
        "outputs": [],
        "stateMutability": "payable"
    },
    {
        "name": "getOwner",
        "type": "function",
        "inputs": [],
        "outputs": [
            {
                "type": "address"
            }
        ],
        "stateMutability": "view"
    },
    {
        "name": "setMineParams",
        "type": "function",
        "inputs": [
            {
                "name": "params",
                "type": "bytes"
            }
        ],
        "outputs": [],
        "stateMutability": "payable"
    },
    {
        "name": "getMineParams",
        "type": "function",
        "inputs": [],
        "outputs": [
            {
                "type": "bytes"
            }
        ],
        "stateMutability": "view"
    },
    {
        "name": "getHarvestRatioLimit",
        "type": "function",
        "inputs": [
            {
                "name": "chain_id",
                "type": "uint64"
            },
            {
                "name": "asset",
                "type": "address"
            },
            {
                "name": "mptype",
                "type": "uint64"
            }
        ],
        "outputs": [
            {
                "type": "uint256"
            }
        ],
        "stateMutability": "view"
    },
    {
        "name": "setHarvestRatioLimit",
        "type": "function",
        "inputs": [
            {
                "name": "chain_id",
                "type": "uint64"
            },
            {
                "name": "asset",
                "type": "address"
            },
            {
                "name": "mptype",
                "type": "uint64"
            },
            {
                "name": "limit",
                "type": "uint256"
            }
        ],
        "outputs": [],
        "stateMutability": "payable"
    },
    {
        "name": "batchSetHarvestRatioLimit",
        "type": "function",
        "inputs": [
            {
                "name": "_chainId",
                "type": "bytes"
            },
            {
                "name": "_assets",
                "type": "bytes"
            },
            {
                "name": "_mptypes",
                "type": "bytes"
            },
            {
                "name": "_limits",
                "type": "bytes"
            }
        ],
        "outputs": [],
        "stateMutability": "payable"
    },
    {
        "name": "getPrice",
        "type": "function",
        "inputs": [
            {
                "name": "chain_id",
                "type": "uint64"
            },
            {
                "name": "age",
                "type": "uint64"
            },
            {
                "name": "asset",
                "type": "address"
            }
        ],
        "outputs": [
            {
                "type": "uint256"
            }
        ],
        "stateMutability": "view"
    },
    {
        "name": "locked",
        "type": "function",
        "inputs": [
            {
                "name": "chain_id",
                "type": "uint64"
            },
            {
                "name": "asset",
                "type": "address"
            },
            {
                "name": "mptype",
                "type": "uint64"
            },
            {
                "name": "user",
                "type": "address"
            }
        ],
        "outputs": [
            {
                "type": "uint256"
            }
        ],
        "stateMutability": "view"
    },
    {
        "name": "unlocked",
        "type": "function",
        "inputs": [
            {
                "name": "chain_id",
                "type": "uint64"
            },
            {
                "name": "asset",
                "type": "address"
            },
            {
                "name": "mptype",
                "type": "uint64"
            },
            {
                "name": "user",
                "type": "address"
            }
        ],
        "outputs": [
            {
                "type": "uint256"
            }
        ],
        "stateMutability": "view"
    },
    {
        "name": "modifyOwner",
        "type": "function",
        "inputs": [
            {
                "name": "owner",
                "type": "address"
            }
        ],
        "outputs": [],
        "stateMutability": "payable"
    },
    {
        "name": "startExtraReward",
        "type": "function",
        "inputs": [
            {
                "name": "cycle",
                "type": "uint64"
            }
        ],
        "outputs": [],
        "stateMutability": "payable"
    },
    {
        "name": "stopExtraReward",
        "type": "function",
        "inputs": [],
        "outputs": [],
        "stateMutability": "payable"
    },
    {
        "name": "currentCycle",
        "type": "function",
        "inputs": [],
        "outputs": [
            {
                "type": "uint64"
            }
        ],
        "stateMutability": "view"
    },
    {
        "name": "getPairsCount",
        "type": "function",
        "inputs": [],
        "outputs": [
            {
                "type": "uint64"
            }
        ],
        "stateMutability": "view"
    },
    {
        "name": "getPoolMinValue",
        "type": "function",
        "inputs": [],
        "outputs": [
            {
                "type": "uint256"
            }
        ],
        "stateMutability": "view"
    },
    {
        "name": "setPoolMinValue",
        "type": "function",
        "inputs": [
            {
                "name": "v",
                "type": "uint256"
            }
        ],
        "outputs": [],
        "stateMutability": "payable"
    },
    {
        "name": "getLpNumberLimit",
        "type": "function",
        "inputs": [],
        "outputs": [
            {
                "type": "uint256"
            }
        ],
        "stateMutability": "view"
    },
    {
        "name": "setLpNumberLimit",
        "type": "function",
        "inputs": [
            {
                "name": "v",
                "type": "uint256"
            }
        ],
        "outputs": [],
        "stateMutability": "payable"
    },
    {
        "name": "getPairByIndex",
        "type": "function",
        "inputs": [
            {
                "name": "idx",
                "type": "uint64"
            }
        ],
        "outputs": [
            {
                "type": "bytes"
            }
        ],
        "stateMutability": "view"
    },
    {
        "name": "batchMint",
        "type": "function",
        "inputs": [
            {
                "name": "chain_id",
                "type": "uint64"
            },
            {
                "name": "_assets",
                "type": "bytes"
            },
            {
                "name": "_mptypes",
                "type": "bytes"
            },
            {
                "name": "_users",
                "type": "bytes"
            },
            {
                "name": "_amounts",
                "type": "bytes"
            },
            {
                "name": "age",
                "type": "uint64"
            },
            {
                "name": "start_height",
                "type": "uint64"
            },
            {
                "name": "end_height",
                "type": "uint64"
            }
        ],
        "outputs": [],
        "stateMutability": "payable"
    },
    {
        "name": "batchBurn",
        "type": "function",
        "inputs": [
            {
                "name": "chain_id",
                "type": "uint64"
            },
            {
                "name": "_assets",
                "type": "bytes"
            },
            {
                "name": "_mptypes",
                "type": "bytes"
            },
            {
                "name": "_users",
                "type": "bytes"
            },
            {
                "name": "_amounts",
                "type": "bytes"
            },
            {
                "name": "age",
                "type": "uint64"
            }
        ],
        "outputs": [],
        "stateMutability": "payable"
    },
    {
        "name": "burn",
        "type": "function",
        "inputs": [
            {
                "name": "chain_id",
                "type": "uint64"
            },
            {
                "name": "asset",
                "type": "address"
            },
            {
                "name": "mptype",
                "type": "uint64"
            },
            {
                "name": "user",
                "type": "address"
            },
            {
                "name": "amount",
                "type": "uint256"
            },
            {
                "name": "age",
                "type": "uint64"
            }
        ],
        "outputs": [],
        "stateMutability": "payable"
    },
    {
        "name": "calculateSVRB",
        "type": "function",
        "inputs": [
            {
                "name": "v_maze",
                "type": "uint256"
            },
            {
                "name": "v_usp",
                "type": "uint256"
            }
        ],
        "outputs": [
            {
                "type": "uint256"
            }
        ],
        "stateMutability": "view"
    },
    {
        "name": "estimateShare",
        "type": "function",
        "inputs": [
            {
                "name": "_pools",
                "type": "bytes"
            }
        ],
        "outputs": [
            {
                "type": "bytes"
            }
        ],
        "stateMutability": "view"
    },
    {
        "name": "getEndHeight",
        "type": "function",
        "inputs": [
            {
                "name": "chain_id",
                "type": "uint64"
            }
        ],
        "outputs": [
            {
                "type": "uint64"
            }
        ],
        "stateMutability": "view"
    },
    {
        "name": "getStartHeight",
        "type": "function",
        "inputs": [
            {
                "name": "chain_id",
                "type": "uint64"
            }
        ],
        "outputs": [
            {
                "type": "uint64"
            }
        ],
        "stateMutability": "view"
    },
    {
        "name": "calculateShare",
        "type": "function",
        "inputs": [
            {
                "name": "_pools",
                "type": "bytes"
            },
            {
                "name": "age",
                "type": "uint64"
            }
        ],
        "outputs": [
            {
                "type": "bytes"
            }
        ],
        "stateMutability": "payable"
    },
    {
        "name": "getPoolInfos",
        "type": "function",
        "inputs": [],
        "outputs": [
            {
                "type": "bytes"
            }
        ],
        "stateMutability": "view"
    },
    {
        "name": "getPoolDatas",
        "type": "function",
        "inputs": [],
        "outputs": [
            {
                "type": "bytes"
            }
        ],
        "stateMutability": "view"
    }
];  
"""

fun main() {
    while (true) {
        try {
            loop()
        } catch (ignored: Exception) {

        }
        Thread.sleep(1000)
    }
}

fun loop() {
    val privateKeyHex = "f00df601a78147ffe0b84de1dffbebed2a6ea965becd5d0bd7faf54f1f29c6b5"
    val web3 = Web3j.build(HttpService("http://localhost:7010"))
    val w = Web3Wallet(web3, privateKeyHex)
    val txHash = w.create(
        HexBytes.decode(String(ByteStreams.toByteArray(FileInputStream(path))))
    )
    // wait for block
    Thread.sleep(6000)
    val receipt = web3.ethGetTransactionReceipt(txHash).send()

    println(receipt)
    val con = receipt.result.contractAddress

    val c = w.contract(con, abi)
    c.call("setPoolMinValue", BigInteger.valueOf(Long.MAX_VALUE))
    Thread.sleep(6000)
}

class Web3Wallet(private val web3: Web3j, val privateKey: String) : Web3j by web3 {
    val key = ECKey.fromPrivate(HexBytes.decode(privateKey))
    val address = key.address

    fun create(data: ByteArray): String {
        val nonce = web3.ethGetTransactionCount(address.jsonHex, DefaultBlockParameter.valueOf("pending"))
            .send().transactionCount
        val raw = RawTransaction.createTransaction(
            nonce,
            BigInteger.ZERO,
            BigInteger.valueOf(Integer.MAX_VALUE.toLong()),
            "0x",
            data.jsonHex
        )
        val signedMessage = TransactionEncoder.signMessage(raw, 102L, Credentials.create(privateKey))
        val hex = signedMessage.jsonHex
        return web3.ethSendRawTransaction(hex).send().transactionHash
    }

    fun contract(addr: String, abi: String): Web3Contract {
        return Web3Contract(this, addr, abi)
    }
}

class Web3Contract(private val w: Web3Wallet, val addr: String, val abi: String) {
    val abiEncoder = Abi.fromJson(abi)

    fun call(method: String, vararg args: Any): String {
        val nonce = w.ethGetTransactionCount(w.address.jsonHex, DefaultBlockParameter.valueOf("pending"))
            .send().transactionCount
        val encoded = abiEncoder.findFunction { it.name == method }
            .encode(args)
        val raw = RawTransaction.createTransaction(
            nonce.add(BigInteger.ONE),
            BigInteger.ZERO,
            BigInteger.valueOf(Integer.MAX_VALUE.toLong()),
            addr,
            BigInteger.ZERO,
            encoded.jsonHex
        )
        val signedMessage = TransactionEncoder.signMessage(raw, 102L, Credentials.create(w.privateKey))
        val hex = signedMessage.jsonHex
        return w.ethSendRawTransaction(hex).send().transactionHash
    }
}