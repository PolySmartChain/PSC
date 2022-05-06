package org.tdf.sunflower.facade

import java.util.*

interface PropertyLike {
    fun getProperty(key: String): String?
}

class PropertiesWrapper(val properties: Properties) : PropertyLike {
    override fun getProperty(key: String): String? {
        return properties.getProperty(key)
    }
}