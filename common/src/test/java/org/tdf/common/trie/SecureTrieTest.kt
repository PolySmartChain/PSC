package org.tdf.common.trie

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.tdf.common.TrieUtil.builder
import org.tdf.common.serialize.Codec.Companion.identity
import org.tdf.common.serialize.Codecs
import org.tdf.common.store.ByteArrayMapStore
import org.tdf.common.store.NoDeleteStore


@RunWith(JUnit4::class)
class SecureTrieTest {
    private lateinit var notSecured: Trie<ByteArray, String>
    private lateinit var secured: Trie<ByteArray, String>

    @Before
    fun before() {
        notSecured = builder<ByteArray, String>()
            .keyCodec(identity())
            .valueCodec(Codecs.string)
            .store(NoDeleteStore(ByteArrayMapStore()) { x: ByteArray? -> x == null || x.size == 0 })
            .build()
        secured = SecureTrie(notSecured)
    }

    @Test
    fun test() {
        secured["1".toByteArray()] = "1"
        assert(secured.size > 0)
        assert(secured.size == 1)
        assert(secured["1".toByteArray()] == "1")
    }

    @Test
    fun testRevert() {
        secured["1".toByteArray()] = "1"
        val root = secured.commit()
        secured["2".toByteArray()] = "2"
        val root2 = secured.commit()
        assert(secured.revert(root).size == 1)
        assert(secured.revert(root)["1".toByteArray()] == "1")
        assert(secured.revert(root2).size == 2)
        assert(secured.revert(root2)["1".toByteArray()] == "1")
        assert(secured.revert(root2)["2".toByteArray()] == "2")
    }
}