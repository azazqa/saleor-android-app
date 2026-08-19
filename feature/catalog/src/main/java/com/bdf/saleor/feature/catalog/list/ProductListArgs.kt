package com.bdf.saleor.feature.catalog.list

data class ProductListArgs(
    val source: ProductListSource,
    val slug: String,
    val title: String,
)

enum class ProductListSource {
    ALL, CATEGORY, COLLECTION
}
