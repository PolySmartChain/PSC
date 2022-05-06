package org.tdf.common.event

import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.function.Consumer

/**
 * lock-free event bus implementation
 */
class EventBus(factory: ThreadFactory) {
    private val listenersLock = false
    private var listeners: Map<Class<*>, List<Consumer<Any>>> = mutableMapOf()
    private val executor = Executors.newFixedThreadPool(4)

    /**
     * subscribe a listener to event
     *
     * @param eventType type of event
     * @param listener  listener which applied when some event published
     * @param <T>       generic
    </T> */
    fun <T> subscribe(eventType: Class<T>, listener: Consumer<in T>) {
        synchronized(listenersLock) {
            // copy when write, avoid concurrent modifications
            val copied = copy(listeners)
            val li: MutableList<Consumer<Any>> = copied[eventType] ?: mutableListOf()

            li.add(listener as Consumer<Any>)
            copied[eventType] = li
            listeners = copied
        }
    }

    /**
     * publish a event to listeners
     *
     * @param event the event to publish
     */
    fun publish(event: Any) {
        val consumers: List<Consumer<Any>> = listeners.getOrDefault(event.javaClass, emptyList())
        for (consumer in consumers) {
            executor.submit {
                try {
                    consumer.accept(event)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun copy(listeners: Map<Class<*>, List<Consumer<Any>>>): MutableMap<Class<*>, MutableList<Consumer<Any>>> {
        val ret: MutableMap<Class<*>, MutableList<Consumer<Any>>> = mutableMapOf()
        listeners.forEach { (k: Class<*>, v: List<Consumer<Any>>) -> ret[k] = v.toMutableList() }
        return ret
    }
}