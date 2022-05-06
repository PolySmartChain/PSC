package org.tdf.sunflower.p2pv2

import java.net.DatagramSocket
import java.util.concurrent.Executors

class UdpServer(val port: Int): AutoCloseable{
    val socket = DatagramSocket(port)
    val ex = Executors.newSingleThreadExecutor()
    @Volatile var running: Boolean = true

    fun start() {
        ex.submit { this.run() }
    }

    private fun run() {
        while(running) {

        }
    }

    override fun close() {
        running = false
        ex.shutdown()
    }
}