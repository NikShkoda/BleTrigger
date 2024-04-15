package com.solvek.bletrigger.data

import org.json.JSONObject

class Registry(json: String) {
    private val r = mutableMapOf<String, Boolean>()

    init {
        val obj = JSONObject(json)
        obj.keys().forEach { id ->
            store(id, obj.getBoolean(id))
        }
    }

    fun toJson(): String {
        val json = JSONObject()
        r.entries.forEach { entry ->
            json.put(entry.key, entry.value)
        }
        return json.toString()
    }

    fun isSameStatus(id: String, needs: Boolean) =
        r[id] == needs

    fun store(id: String, needs: Boolean) {
        r[id] = needs
    }
}