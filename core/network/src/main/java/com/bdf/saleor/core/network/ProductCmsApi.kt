package com.bdf.saleor.core.network

import com.bdf.saleor.core.model.ProductCmsBlock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductCmsApi @Inject constructor(
    config: SaleorCatalogConfig,
    okHttpClient: OkHttpClient,
) {
    private val cmsBaseUrl = config.cmsUrl
    private val client = okHttpClient.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun fetchBlocks(slug: String): List<ProductCmsBlock> {
        val base = cmsBaseUrl.trim()
        if (base.isEmpty() || slug.isBlank()) return emptyList()
        return withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder()
                    .url(productCmsUrl(base, slug))
                    .header("Accept", "application/json")
                    .get()
                    .build()
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (!response.isSuccessful) return@runCatching emptyList()
                    parseProductCmsBlocks(body, base)
                }
            }.getOrDefault(emptyList())
        }
    }
}
