package com.bdf.saleor.core.data

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject

/**
 * Saleor `JSONString` arrives from Apollo as [String] or a parsed [Map]/[List].
 */
internal fun jsonScalarToString(value: Any?): String? {
    val raw = when (value) {
        null -> null
        is String -> value.trim().takeIf { it.isNotEmpty() && it != "null" }
        else -> jsonElementOf(value).toString()
    } ?: return null
    if (raw == "{}" || raw == "[]" || raw == "{\"blocks\":[]}") return null
    return raw
}

private fun jsonElementOf(value: Any?): JsonElement = when (value) {
    null -> JsonNull
    is JsonElement -> value
    is String -> JsonPrimitive(value)
    is Boolean -> JsonPrimitive(value)
    is Int -> JsonPrimitive(value)
    is Long -> JsonPrimitive(value)
    is Float -> JsonPrimitive(value)
    is Double -> JsonPrimitive(value)
    is Number -> JsonPrimitive(value.toDouble())
    is Map<*, *> -> buildJsonObject {
        value.forEach { (key, nested) ->
            val name = key as? String ?: return@forEach
            put(name, jsonElementOf(nested))
        }
    }
    is List<*> -> buildJsonArray {
        value.forEach { add(jsonElementOf(it)) }
    }
    else -> JsonPrimitive(value.toString())
}
