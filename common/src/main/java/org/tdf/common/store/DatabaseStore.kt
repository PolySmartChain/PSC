package org.tdf.common.store

/**
 * Interface represents DB source which is normally the final Source in the chain
 */
interface DatabaseStore : BatchStore<ByteArray, ByteArray>, AutoCloseable {
    /**
     * Initializes DB (open table, connection, etc)
     *
     * @param settings DB settings
     */
    fun init(settings: DBSettings)

    /**
     * @return true if DB connection is alive
     */
    val alive: Boolean

    /**
     * Closes the DB table/connection
     */
    override fun close()

    /**
     * Closes database, destroys its data and finally runs init()
     */
    fun clear()
}