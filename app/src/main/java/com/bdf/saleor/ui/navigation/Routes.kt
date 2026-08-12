package com.bdf.saleor.ui.navigation

object Routes {
    const val HOME = "home"
    const val CATEGORIES = "categories"
    const val SEARCH = "search"
    const val PRODUCT_LIST = "products?source={source}&slug={slug}&title={title}"
    const val PRODUCT_DETAIL = "product/{slug}"

    fun productList(source: String, slug: String = "", title: String = ""): String {
        return "products?source=$source&slug=$slug&title=$title"
    }

    fun productDetail(slug: String): String = "product/$slug"

    object Source {
        const val ALL = "all"
        const val CATEGORY = "category"
        const val COLLECTION = "collection"
    }
}
