package org.tdf.common.util

import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock

class LogLock(private val lock: Lock, private val name: String = "") : Lock {
    companion object {
        private val log = LoggerFactory.getLogger("lock")
    }

    override fun lock() {
        log.debug("{} lock required by thread {}", name, Thread.currentThread().name)
        lock.lock()
        log.debug("{} lock successfully by thread {}", name, Thread.currentThread().name)

    }

    override fun lockInterruptibly() {
        log.debug("{} lock required by thread {}", name, Thread.currentThread().name)
        lock.lockInterruptibly()
        log.debug("{} lock successfully by thread {}", name, Thread.currentThread().name)
    }

    override fun tryLock(): Boolean {
        val r = lock.tryLock()
        log.debug("{} try lock by thread {} result = {}", name, Thread.currentThread().name, r)
        return r
    }

    override fun tryLock(time: Long, unit: TimeUnit): Boolean {
        val r = lock.tryLock(time, unit)
        log.debug("{} try lock by thread {} result = {}", name, Thread.currentThread().name, r)
        return r
    }

    override fun unlock() {
        log.debug("{} unlock required by thread {}", name, Thread.currentThread().name)
        lock.unlock()
        log.debug("{} unlock successfully by thread {}", name, Thread.currentThread().name)
    }


    override fun newCondition(): Condition {
        return lock.newCondition()
    }
}