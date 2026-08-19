package com.bdf.saleor.core.data

import com.bdf.saleor.core.model.CategoryItem
import com.bdf.saleor.core.model.HomeCatalog
import com.bdf.saleor.core.model.ProductDetail
import com.bdf.saleor.core.model.ProductPage

interface CatalogRepository {
    suspend fun getHomeCatalog(): HomeCatalog

    suspend fun getCategories(first: Int = DEFAULT_CATEGORY_PAGE_SIZE): List<CategoryItem>

    suspend fun getProducts(
        first: Int = PAGE_SIZE,
        after: String? = null,
    ): ProductPage

    suspend fun getProductsByCategory(
        slug: String,
        first: Int = PAGE_SIZE,
        after: String? = null,
    ): ProductPage

    suspend fun getProductsByCollection(
        slug: String,
        first: Int = PAGE_SIZE,
        after: String? = null,
    ): ProductPage

    suspend fun searchProducts(
        query: String,
        first: Int = PAGE_SIZE,
        after: String? = null,
    ): ProductPage

    suspend fun getProductDetail(slug: String): ProductDetail?

    companion object {
        const val PAGE_SIZE = 20
        const val DEFAULT_CATEGORY_PAGE_SIZE = 50
    }
}
