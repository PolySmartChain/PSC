package org.tdf.sunflower.sync

import com.google.common.util.concurrent.RateLimiter

class Limiters(status: Long, blocks: Long) {
    val status: RateLimiter? = status.takeIf { it >= 0 }?.toDouble()?.let { RateLimiter.create(it) }

    val blocks: RateLimiter? = blocks.takeIf { it >= 0 }?.toDouble()?.let { RateLimiter.create(it) }
}