package org.tdf.sunflower.net

// context for communicating with peer server and listener
interface Context {
    // exit listeners chain
    fun exit()

    // disconnect to the peer
    fun disconnect()

    // block the peer for a while
    fun block()

    // keep the connection alive
    fun keep()

    // response to the remote peer
    fun response(message: ByteArray)

    // batch response
    fun response(messages: Collection<ByteArray>)

    // relay the received message
    fun relay()

    // get the message received from channel
    val message: ByteArray

    // get remote peer
    val remote: Peer

    val udp: Boolean
}