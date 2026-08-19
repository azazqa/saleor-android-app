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
    val cmsBlocks: List<ProductCmsBlock> = emptyList(),
)

sealed class ProductCmsBlock {
    data class Heading(val text: String, val level: Int) : ProductCmsBlock()
    data class Paragraph(val text: String) : ProductCmsBlock()
    data class Image(val url: String, val alt: String?) : ProductCmsBlock()
    data class Quote(val title: String?, val body: String?) : ProductCmsBlock()
}

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
    val pointsBalance: Money? = null,
    val membership: MembershipInfo? = null,
    val addresses: List<Address> = emptyList(),
    val defaultShippingAddressId: String? = null,
    val defaultBillingAddressId: String? = null,
) {
    val displayName: String
        get() = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ").ifBlank { email }

    val initials: String
        get() {
            val first = firstName.trim().firstOrNull()
            val last = lastName.trim().firstOrNull()
            return when {
                first != null && last != null -> "$first$last"
                first != null -> first.toString()
                else -> email.take(2).uppercase().ifBlank { "?" }
            }
        }

    val welcomeName: String
        get() = firstName.trim().ifBlank { email.substringBefore("@") }

    val defaultShippingAddress: Address?
        get() = addresses.firstOrNull { it.id == defaultShippingAddressId }
            ?: addresses.firstOrNull { it.isDefaultShipping }
}

data class MembershipInfo(
    val tierName: String?,
    val nextTierName: String?,
    val currentSpend: Money,
    val amountToNextTier: Money,
    val couponCode: String?,
    val validFrom: String?,
    val validUntil: String?,
    val discountPercentage: Double?,
)

data class Address(
    val id: String,
    val firstName: String,
    val lastName: String,
    val companyName: String,
    val streetAddress1: String,
    val streetAddress2: String,
    val city: String,
    val cityArea: String,
    val postalCode: String,
    val countryCode: String,
    val countryName: String,
    val countryArea: String,
    val phone: String,
    val isDefaultShipping: Boolean,
    val isDefaultBilling: Boolean,
) {
    val recipientName: String
        get() = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")

    fun localityLine(): String = listOf(city, countryArea)
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .ifBlank { streetAddress1 }

    fun formattedLines(): List<String> = buildList {
        val name = recipientName
        if (name.isNotBlank()) add(name)
        if (companyName.isNotBlank()) add(companyName)
        if (streetAddress1.isNotBlank()) add(streetAddress1)
        if (streetAddress2.isNotBlank()) add(streetAddress2)
        val cityLine = listOf(postalCode, city, countryArea).filter { it.isNotBlank() }.joinToString(" ")
        if (cityLine.isNotBlank()) add(cityLine)
        if (countryName.isNotBlank()) add(countryName)
        val displayPhone = displayPhone()
        if (displayPhone.isNotBlank()) add(displayPhone)
    }

    fun displayPhone(): String = formatKoreanDisplayPhone(phone, countryCode)
}

data class AddressDraft(
    val firstName: String = "",
    val lastName: String = "",
    val companyName: String = "",
    val streetAddress1: String = "",
    val streetAddress2: String = "",
    val city: String = "",
    val cityArea: String = "",
    val postalCode: String = "",
    val countryCode: String = "KR",
    val countryArea: String = "",
    val phone: String = "",
)

enum class AddressKind {
    SHIPPING,
    BILLING,
}

data class PointsHistoryEntry(
    val id: String,
    val date: String,
    val type: String,
    val amount: Money,
    val balanceAfter: Money,
    val reason: String?,
    val orderId: String?,
    val orderNumber: String?,
)

data class PointsPage(
    val balance: Money?,
    val entries: List<PointsHistoryEntry>,
    val endCursor: String?,
    val hasNextPage: Boolean,
    val totalCount: Int,
)

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

data class CartLine(
    val id: String,
    val variantId: String,
    val productName: String,
    val variantName: String,
    val thumbnailUrl: String?,
    val quantity: Int,
    val unitPrice: Money?,
    val totalPrice: Money?,
)

data class Cart(
    val id: String,
    val lines: List<CartLine>,
    val subtotal: Money?,
    val shipping: Money?,
    val total: Money?,
    /** Total units across all lines (Saleor checkout quantity). */
    val quantity: Int,
) {
    /** Distinct line-item count shown on the cart icon badge. */
    val lineCount: Int get() = lines.size
}

data class DeliveryOption(
    val id: String,
    val shippingMethodId: String,
    val name: String,
    val price: Money?,
    val minDeliveryDays: Int?,
    val maxDeliveryDays: Int?,
    val active: Boolean = true,
)

data class PaymentGatewayInfo(
    val id: String,
    val name: String,
    val currencies: List<String> = emptyList(),
    val config: Map<String, String> = emptyMap(),
)

enum class CheckoutAuthorizeStatus {
    NONE,
    PARTIAL,
    FULL,
    UNKNOWN,
}

data class CheckoutSession(
    val id: String,
    val email: String?,
    val lines: List<CartLine>,
    val quantity: Int,
    val subtotal: Money?,
    val shipping: Money?,
    val discount: Money?,
    val voucherCode: String? = null,
    val discountName: String? = null,
    val total: Money?,
    val totalBalance: Money?,
    val shippingAddress: Address?,
    val billingAddress: Address?,
    val isShippingRequired: Boolean,
    val selectedDeliveryMethodId: String?,
    val authorizeStatus: CheckoutAuthorizeStatus,
    val availablePaymentGateways: List<PaymentGatewayInfo>,
    val channelSlug: String?,
) {
    fun toCart(): Cart = Cart(
        id = id,
        lines = lines,
        subtotal = subtotal,
        shipping = shipping,
        total = total,
        quantity = quantity,
    )
}

data class CompletedOrder(
    val id: String,
    val number: String,
)

data class PaymentResult(
    val success: Boolean,
    val message: String? = null,
    val transactionId: String? = null,
    val orderId: String? = null,
    val orderName: String? = null,
    val amount: Double? = null,
    val currency: String? = null,
    val customerKey: String? = null,
    val clientKey: String? = null,
    val authorizedAmount: Double? = null,
)

object PaymentGateways {
    const val TOSS = "klms.app.payment.tosspayments"
    const val POINTS = "saleor.io.points-payment-gateway"
}
