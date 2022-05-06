package org.tdf.sunflower

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.tdf.sunflower.facade.DatabaseStoreFactory
import org.tdf.sunflower.facade.Miner
import org.tdf.sunflower.net.PeerServer
import javax.annotation.PreDestroy

// resource cleaner
@Component
class Stop(
    private val miner: Miner,
    private val peerServer: PeerServer,
    private val factory: DatabaseStoreFactory
) {
    @PreDestroy
    @Throws(Exception::class)
    fun onDestroy() {
        log.info("clean resources before exit program...")
        miner.stop()
        peerServer.stop()
        factory.cleanup()
    }

    companion object {
        private val log = LoggerFactory.getLogger("stop")
    }
}