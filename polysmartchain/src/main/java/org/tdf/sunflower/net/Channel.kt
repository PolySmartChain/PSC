package org.tdf.sunflower.net

import org.tdf.sunflower.proto.Message

/**
 * channel for message transports
 */
interface Channel {
    /**
     * write message to channel
     */
    fun write(message: Message, ctx: UdpCtx? = null)

    /**
     * close the channel
     */
    fun close() {
        close("")
    }

    /**
     * close the channel
     */
    fun close(reason: String)
    val isAlive: Boolean
        get() = !closed


    /**
     * whether the channel is closed
     */
    val closed: Boolean

    /**
     * notify listeners new message received
     */
    fun message(message: Message, udp: Boolean = false)

    /**
     * notify listeners error
     */
    fun error(throwable: Throwable)

    val remote: PeerImpl?

    // bind listener to the channel
    fun addListeners(vararg listeners: ChannelListener)

    val listeners: List<ChannelListener>

    // 0 = self as client, 1 = self as server
    val direction: Int
}