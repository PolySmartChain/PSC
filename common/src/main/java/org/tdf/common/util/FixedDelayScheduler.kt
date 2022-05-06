package org.tdf.common.util

import com.google.common.util.concurrent.ThreadFactoryBuilder
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class FixedDelayScheduler(name: String, private val delay: Long) {
    private val factory = ThreadFactoryBuilder().setNameFormat(name + "-%d").build()
    private val executor = Executors.newSingleThreadScheduledExecutor(factory)

    fun delay(runnable: Runnable) {
        executor.scheduleWithFixedDelay(runnable, delay, delay, TimeUnit.SECONDS)
    }

    fun shutdownNow() {
        this.executor.shutdownNow()
    }
}