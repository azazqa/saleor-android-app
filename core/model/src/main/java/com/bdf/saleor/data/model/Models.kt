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

data class UserProfile(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val avatarUrl: String?,
    val dateJoined: String?,
) {
    val displayName: String
        get() = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ").ifBlank { email }
}

sealed interface AuthState {
    data object Unknown : AuthState
    data object LoggedOut : AuthState
    data class LoggedIn(val email: String?) : AuthState
}

data class AuthResult(
    val success: Boolean,
    val message: String? = null,
    val requiresConfirmation: Boolean = false,
)

data class OrderSummary(
    val id: String,
    val number: String,
    val created: String,
    val status: String,
    val statusDisplay: String,
    val paymentStatusDisplay: String?,
    val total: Money?,
    val thumbnailUrl: String?,
    val lineCount: Int,
)

data class OrderLineItem(
    val id: String,
    val productName: String,
    val variantName: String,
    val quantity: Int,
    val thumbnailUrl: String?,
    val totalPrice: Money?,
)

data class OrderDetail(
    val id: String,
    val number: String,
    val created: String,
    val status: String,
    val statusDisplay: String,
    val paymentStatusDisplay: String?,
    val subtotal: Money?,
    val shippingPrice: Money?,
    val total: Money?,
    val shippingAddress: String?,
    val lines: List<OrderLineItem>,
)

data class OrderPage(
    val items: List<OrderSummary>,
    val endCursor: String?,
    val hasNextPage: Boolean,
    val totalCount: Int?,
)
