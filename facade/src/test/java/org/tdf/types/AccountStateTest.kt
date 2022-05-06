package org.tdf.types

import com.github.salpadding.rlpstream.Rlp
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.spongycastle.util.encoders.Hex
import org.tdf.common.types.Uint256
import org.tdf.common.util.Permutation
import org.tdf.sunflower.state.Account
import java.math.BigInteger

@RunWith(JUnit4::class)
class AccountStateTest {
    @Test
    fun testGetEncoded() {
        val expected = ("f85e809"
                + "a0100000000000000000000000000000000000000000000000000"
                + "a056e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421"
                + "a0c5d2460186f7233c927e7db2dcc703c0e500b653ca82273b7bfad8045d85a470")
        val acct = Account(balance = Uint256.of(BigInteger.valueOf(2).pow(200)))
        Assert.assertEquals(expected, Hex.toHexString(Rlp.encode(acct)))
    }

    @Test
    fun testPermuation() {
        val arr: List<List<Int>> = arrayListOf(
            arrayListOf(1, 2),
            arrayListOf(3, 4),
        )
        val p = Permutation(arr)
        while (true) {
            val n = p.next() ?: break
            println(n)
        }
    }
}