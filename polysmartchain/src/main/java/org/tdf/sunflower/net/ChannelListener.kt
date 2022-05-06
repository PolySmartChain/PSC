package org.tdf.sunflower.net

import org.tdf.sunflower.proto.Message

interface ChannelListener {
    // triggered when channel is open, only once in the life cycle of the channel
    fun onConnect(remote: PeerImpl, channel: Channel)

    // when new message received
    fun onMessage(message: Message, channel: Channel, udp: Boolean = false)

    // when error occurred
    fun onError(throwable: Throwable, channel: Channel)

    // when the channel been closed
    fun onClose(channel: Channel)

    companion object {
        val NONE: ChannelListener = object : ChannelListener {
            override fun onConnect(remote: PeerImpl, channel: Channel) {}
            override fun onMessage(message: Message, channel: Channel, udp: Boolean) {}
            override fun onError(throwable: Throwable, channel: Channel) {}
            override fun onClose(channel: Channel) {}
        }
    }
}