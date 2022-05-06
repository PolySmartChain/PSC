package org.tdf.sunflower

import com.github.salpadding.rlpstream.Rlp
import com.github.salpadding.rlpstream.annotation.RlpCreator
import com.github.salpadding.rlpstream.annotation.RlpProps
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.spongycastle.util.encoders.Hex
import org.tdf.common.types.Uint256
import org.tdf.common.util.HexBytes
import org.tdf.common.util.address
import org.tdf.common.util.hex
import org.tdf.sunflower.state.Account
import org.tdf.sunflower.types.Bloom
import org.tdf.sunflower.types.LogFilter
import java.math.BigInteger

@RunWith(JUnit4::class)
class AccountTests {

    @Test
    fun testGetEncoded() {
        val expected = ("f85e809"
                + "a0100000000000000000000000000000000000000000000000000"
                + "a056e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421"
                + "a0c5d2460186f7233c927e7db2dcc703c0e500b653ca82273b7bfad8045d85a470")
        val acct = Account(balance = Uint256.Companion.of(BigInteger.valueOf(2).pow(200)))
        Assert.assertEquals(expected, Hex.toHexString(Rlp.encode(acct)))
        val decoded = Rlp.decode(HexBytes.decode(expected), Account::class.java)
        assert(decoded == acct)
    }

    @Test
    fun testGetEncoded1() {
        val expected = ("f85e809"
                + "a0100000000000000000000000000000000000000000000000000"
                + "a056e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421"
                + "a0c5d2460186f7233c927e7db2dcc703c0e500b653ca82273b7bfad8045d85a470")
        val acct = AccountJ.emptyAccount(Uint256.Companion.of(BigInteger.valueOf(2).pow(200)))
        Assert.assertEquals(expected, Hex.toHexString(Rlp.encode(acct)))
    }

    @RlpProps("n")
    data class Foo @RlpCreator constructor(val n: String = "")

    @Test
    fun testDefault() {
        val s = "0xc483616263"
        val f = Rlp.decode(HexBytes.decode(s), Foo::class.java)
        println(f)
    }

    @Test
    fun testFilter() {
        val bloom =
            "00000000000000040000000000000000000000000400000000000000000080000000000000000000000000000000000000000000000000000000000000000000000000000000100000000000800000000000000000000000000000000000000000000000020000000000000000000800000000000000000000000000000000000000000000000000000400000000000000000000000000000000000000000000000001000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000010000000000000000000".hex()
        val f = LogFilter()
        f
            .withContractAddress("0xf8B2d82d74c83A6A0e2A6FA9823B30508b987277".address().bytes)
            .withTopic("0x342827c97908e5e2f71151c08502a66d44b6f758e3ac2f1de95f02eb95f0a735".hex().bytes)

        println(f.matchBloom(Bloom(bloom.bytes)))
    }


}