package org.tdf.sunflower

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.tdf.common.types.Uint256
import org.tdf.common.util.HexBytes
import org.tdf.common.util.hex
import org.tdf.sunflower.state.BN128Addition
import org.tdf.sunflower.state.BN128Multiplication
import org.tdf.sunflower.state.BN128Pairing
import org.tdf.sunflower.state.ECRecover
import org.tdf.sunflower.types.Transaction
import org.tdf.sunflower.util.MapperUtil
import java.math.BigInteger

@RunWith(JUnit4::class)
class StatusMessageTest {

    @Test
    fun test1() {
        val tx = Transaction(
            nonce = 20,
            gasPrice = Uint256.ZERO,
            gasLimit = 2000000,
            to = "bd8156c26c1b048bea6e085600444b66a2a87629".hex(),
            value = Uint256.of("1000000000000000000"),
            data = "b214faa5132fb44ad2a35cba64cc363dfb68c52525c24122c7f7f55386e7a86e64fd6257".hex(),
            vrs = Triple(
                BigInteger.valueOf(239),
                "f1dbadda31e56bd4003c86fc9cd600c32c8eabe0ad2f9507c33f3640bed0b800".hex(),
                "3f6636105794f8ff3c561168860915ebbbdbb9de763f737db2253274c32d86e8".hex()
            )
        )

        val data =
            "0x18c547e4f7b0f325ad1e56f57e26c745b09a3e503d86e00e5255ff7f715d3d1c000000000000000000000000000000000000000000000000000000000000001c73b1693892219d736caba55bdb67216e485557ea6b6af75f37096c9aa6a5a75feeb940b1d03b21e36b0e47e79769f095fe2ab855bd91e3a38756b7d75a9c4549".hex()

        println(ECRecover.execute(data.bytes).hex())
    }


    @Test
    fun test4() {
        val input =
            "0x17d456a67c1042e2b124645eb503e41c380beb0fec111e14d64e7441625155810e1e53c6f0698196e352f010924e761e327853829f0c5c3e892f6e13d4e16c2c0940e7b778ef8954f96425ab4b0951adfa125a6e3f13fd12e621a53247b52fce1c8383a51c76f016abea29f08a126bc6b1a589c71c86143cb5f7c28afe20c44200000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000".hex()
        val res = BN128Addition.execute(input.bytes).hex()
        assert(
            res == "0x207c2bd3dd194072b4ff2fca59f6cd8e313f4838fe5dcbda7d0b94211138673719c6ff8f93f25d2f97db44cf15fb06cd279edd8b1dc778cd7b12f406ab1ca157".hex(),
            { "bn128 add" })
    }


    @Test
    fun test5() {
        val input =
            "0x2433821e43166fa634ca8baf0ec88c407a12a02bad9732d3a31be8c8f92cbe99021d358f2812173e09554c1047c6c781549197274d209a0591faa95bd22e5d16000000000000000000000000accf21c397c989896a16d41e54bdb8391f2924ef0000000000000000000000000000000000000000000000000000000000000000".hex()
        val res = BN128Multiplication.execute(input.bytes).hex()
        assert(res == "0x2ab9e54e88b1006503dd14ed51689f2794eda4b4d5c0d14f13b2ddf4237ea8a3064d7edc6371255bc851e05255a936746f72bbcebf7cab1577cfca70a9c2f234".hex())
    }

    @Test
    fun test6() {
        val input =
            "0x02aae21ded18db0c6b59ac24c765c755ad5211194d83ccfb801f6b993c4d45341aa6a2ab06e4317b1f7b2e4e08a4c601fc3cffcacfc469f40e768edbf1eaec160313aa09aed9f72339325be693381a9fa55ee9fbb7b8cd9ecad8d1fa17b96e38273a364aee77ff8a5b0943d3e9760be24b201709c3a0916825de9703e767e4af2c594179cf2870898f38e36c161029c4200d80f14b560ac18b49ea041af196bd040dd226156de65b6aeecb13bd00283f757778c9bb87bd942d8d163aa8968ab72194e7510378d2bf4556eb34b77340464b1caa4dc3354f2f4c5148b1efe5f4c21b136e761e47931e542c8aad0923a89d70351270723ab2d5ea72dbcbbf509eb80f0be96e95a30fb89ccc570f1adc9d9844d0e3a95a2e6d137e86c19aca92f2840fe41c5d77ebaec65381b6cbd2d4cab1031dd9a8616dbc5c23d10421e0849682208e65460d59042db773b8ae232af537432756ae140c592bd7cd3e7c7381e72f00b0620f15a8d5d8f4cdf07c113ff4adc450942aafe2a72c262cbf905b79480926cba4e140ea71faa8ed86a7e8e33a34a3c27064ba4481740b2938e967c2fbe50ca57d709fb0730f22cec87030f332839de7db45656bc24b0ed370f8c0a3b7e10690cd09254000ca9bb4281a5400218b117e23ea32d2269a685055dfa20d5dbc04a94c6fb6b146d4ff40a24539eb8b60e3297a9a1af6d1db01d83cf94ec01cf90ab3fa2d3a79d2285ad539111100d34f031e6a42ca0f0a5a0c08861045a91b9f209ee2bae610be01661ecb764044e3a6ad1b6285d2abfa79867a58a0ac25a50e145ae8bbe2ab4ffad0086c3737ad8db6e2d5724c930ebbf85e6fe8aed339bcf429ff19f5a4a28f997291f8887410ba072590af4b4f98fff8290901fd5d9dc1b421c7b01f0aff624ac22bdac2efa9a7732c99fe668ade3c578869fb8c8dcfcb2b02578ecef52a785bdc5ada7b18e3f6fe00d42008734890a1c022d2c712c86e6c05cedde3cc13d924d0edda67c9049836b67e69fbbc365b9bc8483c69df86d17e218a18671d594406514e72555100aed7ccd67a2f569ee47a25fce88b1fc71f93".hex()
        val res = BN128Pairing.execute(input.bytes).hex()
        assert(res == "0x0000000000000000000000000000000000000000000000000000000000000001".hex())
    }
}

data class SchnorrPrivateKey(
    @JsonProperty("private_key") val privateKey: HexBytes,
    @JsonProperty("public_key") val publicKey: Point,
    @JsonProperty("round_1") val round1: JsonNode
) {
    val p0: Point = MapperUtil.OBJECT_MAPPER.convertValue(round1["p0"], Point::class.java)
    val p1: Point = MapperUtil.OBJECT_MAPPER.convertValue(round1["p1"], Point::class.java)
}

data class Round2(
    val prime: HexBytes,
    @JsonProperty("round_2") val round2: HexBytes,
    @JsonProperty("x_tilde") val tilde: Point,
    val c: HexBytes,
    val r: Point,
)


data class Point(val x: HexBytes, val y: HexBytes)
