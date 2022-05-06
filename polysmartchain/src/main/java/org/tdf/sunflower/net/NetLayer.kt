package org.tdf.sunflower.net

import java.io.Closeable
import java.util.function.Consumer

// transport layer where p2p network builds on
interface NetLayer : Closeable {
    // start listening
    fun start()

    // register channel incoming handler, server side api
    var handler: Consumer<Channel>

    // create a channel, client side api
    fun createChannel(host: String, port: Int, vararg listeners: ChannelListener): Channel?
}