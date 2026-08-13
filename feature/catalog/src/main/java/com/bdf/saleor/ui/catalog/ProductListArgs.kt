package com.bdf.saleor.ui.catalog

data class ProductListArgs(
    val source: String,
    val slug: String,
    val title: String,
)

object ProductListSource {
    const val ALL = "all"
    const val CATEGORY = "category"
    const val COLLECTION = "collection"
}
