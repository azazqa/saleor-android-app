package com.bdf.saleor.data.model

data class Money(
    val amount: Double,
    val currency: String,
) {
    fun format(): String {
        val symbol = when (currency.uppercase()) {
            "KRW" -> "₩"
            "USD" -> "$"
            "EUR" -> "€"
            "JPY" -> "¥"
            else -> "$currency "
        }
        return if (currency.equals("KRW", ignoreCase = true) || currency.equals("JPY", ignoreCase = true)) {
            "$symbol${"%,.0f".format(amount)}"
        } else {
            "$symbol${"%,.2f".format(amount)}"
        }
    }
}

data class ProductSummary(
    val id: String,
    val name: String,
    val slug: String,
    val thumbnailUrl: String?,
    val price: Money?,
    val categoryName: String?,
)

data class ProductPage(
    val items: List<ProductSummary>,
    val endCursor: String?,
    val hasNextPage: Boolean,
    val totalCount: Int?,
    val title: String? = null,
)

data class CategoryItem(
    val id: String,
    val name: String,
    val slug: String,
    val imageUrl: String?,
    val children: List<CategoryItem> = emptyList(),
)

data class VariantOption(
    val attributeSlug: String,
    val attributeName: String,
    val valueSlug: String,
    val valueName: String,
)

data class ProductVariant(
    val id: String,
    val name: String,
    val quantityAvailable: Int,
    val price: Money?,
    val mediaUrls: List<String>,
    val options: List<VariantOption>,
)

data class ProductDetail(
    val id: String,
    val name: String,
    val slug: String,
    val descriptionJson: String?,
    val mediaUrls: List<String>,
    val priceRangeStart: Money?,
    val priceRangeStop: Money?,
    val categoryName: String?,
    val categorySlug: String?,
    val variants: List<ProductVariant>,
)

data class HomeCatalog(
    val featuredTitle: String?,
    val featuredProducts: List<ProductSummary>,
    val categories: List<CategoryItem>,
)
