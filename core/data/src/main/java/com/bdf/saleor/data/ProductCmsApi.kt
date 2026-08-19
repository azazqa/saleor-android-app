package com.bdf.saleor.data

import com.bdf.saleor.data.model.ProductCmsBlock
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class ProductCmsApi(
    private val cmsBaseUrl: String,
) {
    suspend fun fetchBlocks(slug: String): List<ProductCmsBlock> {
        val base = cmsBaseUrl.trim()
        if (base.isEmpty() || slug.isBlank()) return emptyList()
        return withContext(Dispatchers.IO) {
            runCatching {
                val connection = (URL(productCmsUrl(base, slug)).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10_000
                    readTimeout = 15_000
                    setRequestProperty("Accept", "application/json")
                }
                try {
                    val code = connection.responseCode
                    val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                    val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                    if (code !in 200..299) return@runCatching emptyList()
                    parseProductCmsBlocks(body, base)
                } finally {
                    connection.disconnect()
                }
            }.getOrDefault(emptyList())
        }
    }
}
