package org.tdf.sunflower.vm.hosts

import org.tdf.lotusvm.runtime.HostFunction

class Hosts(
    val context: ContextHost? = null,
    val db: DBFunctions? = null,
    val transfer: Transfer? = null,
    val reflect: Reflect? = null,
    val u256: U256Host? = null,
) {
    val all: Set<HostFunction>
        get() = listOfNotNull(context, db, transfer, reflect, u256, Log()).toHashSet()
}