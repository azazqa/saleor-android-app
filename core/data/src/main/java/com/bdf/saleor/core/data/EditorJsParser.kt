package com.bdf.saleor.core.data

import com.bdf.saleor.core.model.EditorJsBlock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

private val json = Json { ignoreUnknownKeys = true; isLenient = true }

fun parseEditorJsBlocks(raw: String?): List<EditorJsBlock> {
    val text = raw?.trim().orEmpty()
    if (text.isEmpty() || text == "{}" || text == "[]" || text == "null") return emptyList()
    return runCatching {
        val root = json.parseToJsonElement(text)
        val blocks = root.jsonObjectOrNull?.get("blocks")?.jsonArrayOrNull
            ?: return@runCatching if (root is JsonPrimitive) {
                listOfNotNull(root.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }?.let { EditorJsBlock.Paragraph(it) })
            } else {
                emptyList()
            }
        blocks.mapNotNull { element -> parseBlock(element) }
    }.getOrElse {
        if (text.startsWith("{") || text.startsWith("[")) emptyList() else listOf(EditorJsBlock.Paragraph(text))
    }
}

private fun parseBlock(element: JsonElement): EditorJsBlock? {
    val block = element.jsonObjectOrNull ?: return null
    val type = block["type"]?.stringOrNull?.lowercase().orEmpty()
    val data = block["data"]?.jsonObjectOrNull ?: JsonObject(emptyMap())
    return when (type) {
        "header" -> data.plainText("text")?.let {
            EditorJsBlock.Header(text = it, level = data["level"]?.intOrNull() ?: 2)
        }
        "paragraph", "text" -> data["text"]?.stringOrNull?.trim()?.takeIf { it.isNotEmpty() }
            ?.let { EditorJsBlock.Paragraph(it) }
        "image" -> {
            val url = data["file"]?.jsonObjectOrNull?.get("url")?.stringOrNull
                ?: data["url"]?.stringOrNull
            url?.takeIf { it.isNotBlank() }?.let { imageUrl ->
                EditorJsBlock.Image(url = imageUrl, caption = data.plainText("caption"))
            }
        }
        "list" -> {
            val items = data["items"]?.jsonArrayOrNull?.mapNotNull { item ->
                item.jsonObjectOrNull?.plainText("content")
                    ?: item.jsonObjectOrNull?.plainText("text")
                    ?: item.stringOrNull?.stripHtml()
            }.orEmpty()
            if (items.isEmpty()) {
                null
            } else {
                EditorJsBlock.ListBlock(
                    ordered = data["style"]?.stringOrNull == "ordered",
                    items = items,
                )
            }
        }
        "quote" -> data.plainText("text")?.let { EditorJsBlock.Quote(it) }
        "delimiter" -> EditorJsBlock.Delimiter
        else -> data.plainText("text")?.let { EditorJsBlock.Paragraph(it) }
    }
}

fun parseEditorJsDescription(raw: String?): String {
    return parseEditorJsBlocks(raw).joinToString("\n\n") { block ->
        when (block) {
            is EditorJsBlock.Header -> block.text
            is EditorJsBlock.Paragraph -> block.html.stripHtml()
            is EditorJsBlock.Image -> block.caption.orEmpty()
            is EditorJsBlock.ListBlock -> block.items.joinToString("\n")
            is EditorJsBlock.Quote -> block.text
            EditorJsBlock.Delimiter -> ""
        }
    }.trim()
}

private fun JsonObject.plainText(key: String): String? =
    this[key]?.stringOrNull?.stripHtml()?.takeIf { it.isNotBlank() }

private val JsonElement.jsonObjectOrNull: JsonObject?
    get() = this as? JsonObject

private val JsonElement.jsonArrayOrNull: JsonArray?
    get() = this as? JsonArray ?: runCatching { jsonArray }.getOrNull()

private val JsonElement.stringOrNull: String?
    get() = (this as? JsonPrimitive)?.contentOrNull

private fun JsonElement.intOrNull(): Int? =
    (this as? JsonPrimitive)?.intOrNull ?: stringOrNull?.toIntOrNull()

private fun String.stripHtml(): String =
    replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace(Regex("<[^>]*>"), "")
        .trim()
