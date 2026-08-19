package com.bdf.saleor.core.network

import com.bdf.saleor.core.model.ProductCmsBlock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private val json = Json { ignoreUnknownKeys = true; isLenient = true }

private val markdownImage = Regex("""!\[([^\]]*)]\(([^)]+)\)""")
private val markdownHeading = Regex("""^(#{1,6})\s+(.+)$""")

internal fun productCmsUrl(baseUrl: String, slug: String): String {
    val root = baseUrl.trimEnd('/')
    val encoded = java.net.URLEncoder.encode(slug, Charsets.UTF_8.name())
    return "$root/api/product-contents?filters[saleorSlug][\$eq]=$encoded&populate[blocks][populate]=*"
}

internal fun parseProductCmsBlocks(raw: String, cmsBaseUrl: String): List<ProductCmsBlock> {
    val root = runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull() ?: return emptyList()
    val items = root["data"] as? JsonArray ?: return emptyList()
    val first = items.firstOrNull() as? JsonObject ?: return emptyList()
    val blocks = first["blocks"] as? JsonArray ?: return emptyList()
    return blocks.flatMap { element ->
        val block = element as? JsonObject ?: return@flatMap emptyList()
        when (block["__component"]?.jsonPrimitive?.contentOrNull) {
            "shared.rich-text" -> parseRichTextMarkdown(
                markdown = block["body"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                cmsBaseUrl = cmsBaseUrl,
            )
            "shared.media" -> {
                val url = mediaUrl(block["file"] as? JsonObject, cmsBaseUrl) ?: return@flatMap emptyList()
                val alt = (block["file"] as? JsonObject)
                    ?.get("alternativeText")
                    ?.jsonPrimitive
                    ?.contentOrNull
                listOf(ProductCmsBlock.Image(url = url, alt = alt))
            }
            "shared.quote" -> {
                val title = block["title"]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }
                val body = block["body"]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }
                if (title == null && body == null) emptyList()
                else listOf(ProductCmsBlock.Quote(title = title, body = body))
            }
            else -> emptyList()
        }
    }
}

internal fun parseRichTextMarkdown(markdown: String, cmsBaseUrl: String): List<ProductCmsBlock> {
    val text = markdown.replace("\r\n", "\n").trim()
    if (text.isEmpty()) return emptyList()
    return text.split(Regex("\n{2,}"))
        .flatMap { chunk -> parseMarkdownChunk(chunk.trim(), cmsBaseUrl) }
}

private fun parseMarkdownChunk(chunk: String, cmsBaseUrl: String): List<ProductCmsBlock> {
    if (chunk.isEmpty()) return emptyList()
    val heading = markdownHeading.matchEntire(chunk.lineSequence().first())
    if (heading != null && !chunk.contains('\n')) {
        return listOf(
            ProductCmsBlock.Heading(
                text = heading.groupValues[2].trim(),
                level = heading.groupValues[1].length,
            ),
        )
    }
    val blocks = mutableListOf<ProductCmsBlock>()
    var cursor = 0
    markdownImage.findAll(chunk).forEach { match ->
        val before = chunk.substring(cursor, match.range.first).trim()
        if (before.isNotEmpty()) blocks += textOrHeading(before)
        val url = resolveCmsUrl(match.groupValues[2].trim(), cmsBaseUrl)
        val alt = match.groupValues[1].trim().takeIf { it.isNotEmpty() }
        if (url.isNotEmpty()) blocks += ProductCmsBlock.Image(url = url, alt = alt)
        cursor = match.range.last + 1
    }
    val after = chunk.substring(cursor).trim()
    if (after.isNotEmpty()) blocks += textOrHeading(after)
    if (blocks.isEmpty()) blocks += textOrHeading(chunk)
    return blocks
}

private fun textOrHeading(raw: String): ProductCmsBlock {
    val heading = markdownHeading.matchEntire(raw.trim())
    return if (heading != null) {
        ProductCmsBlock.Heading(
            text = heading.groupValues[2].trim(),
            level = heading.groupValues[1].length,
        )
    } else {
        ProductCmsBlock.Paragraph(raw.trim())
    }
}

private fun mediaUrl(file: JsonObject?, cmsBaseUrl: String): String? {
    if (file == null) return null
    val large = file["formats"]?.jsonObjectOrNull
        ?.get("large")
        ?.jsonObjectOrNull
        ?.get("url")
        ?.jsonPrimitive
        ?.contentOrNull
    val url = large
        ?: file["url"]?.jsonPrimitive?.contentOrNull
        ?: return null
    return resolveCmsUrl(url, cmsBaseUrl).takeIf { it.isNotEmpty() }
}

private fun resolveCmsUrl(url: String, cmsBaseUrl: String): String {
    val trimmed = url.trim()
    if (trimmed.isEmpty()) return ""
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
    val root = cmsBaseUrl.trimEnd('/')
    return if (trimmed.startsWith("/")) "$root$trimmed" else "$root/$trimmed"
}

private val kotlinx.serialization.json.JsonElement.jsonObjectOrNull: JsonObject?
    get() = this as? JsonObject
