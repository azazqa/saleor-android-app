package com.bdf.saleor.ui.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private val json = Json { ignoreUnknownKeys = true }

/**
 * Extracts plain text from Saleor EditorJS description JSON.
 */
fun parseEditorJsDescription(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    return runCatching {
        val root = json.parseToJsonElement(raw).jsonObject
        val blocks = root["blocks"]?.jsonArray.orEmpty()
        blocks.mapNotNull { block ->
            val data = block.jsonObject["data"]?.jsonObject ?: return@mapNotNull null
            data["text"]?.jsonPrimitive?.contentOrNull
                ?: data["items"]?.jsonArray?.mapNotNull { item ->
                    when (item) {
                        is JsonObject -> item["content"]?.jsonPrimitive?.contentOrNull
                            ?: item["text"]?.jsonPrimitive?.contentOrNull
                        else -> item.jsonPrimitive.contentOrNull
                    }
                }?.joinToString("\n")
        }.joinToString("\n\n")
            .replace(Regex("<[^>]*>"), "")
            .trim()
    }.getOrElse { raw }
}
