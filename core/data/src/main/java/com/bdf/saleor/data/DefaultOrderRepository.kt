package com.bdf.saleor.data

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
import com.bdf.saleor.data.model.Money
import com.bdf.saleor.data.model.OrderDetail
import com.bdf.saleor.data.model.OrderLineItem
import com.bdf.saleor.data.model.OrderPage
import com.bdf.saleor.data.model.OrderSummary
import com.bdf.saleor.graphql.CurrentUserOrderDetailQuery
import com.bdf.saleor.graphql.CurrentUserOrdersPaginatedQuery
import com.bdf.saleor.graphql.fragment.OrderDetails
import com.bdf.saleor.graphql.type.LanguageCodeEnum
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultOrderRepository @Inject constructor(
    private val apolloClient: ApolloClient,
    private val config: SaleorCatalogConfig,
) : OrderRepository {
    private val languageCode: LanguageCodeEnum
        get() = LanguageCodeEnum.safeValueOf(config.graphqlLanguageCode)

    override suspend fun getOrders(first: Int, after: String?): OrderPage {
        val data = apolloClient.query(
            CurrentUserOrdersPaginatedQuery(
                first = Optional.present(first),
                after = Optional.presentIfNotNull(after),
                languageCode = languageCode,
            ),
        )
            .fetchPolicy(FetchPolicy.NetworkOnly)
            .execute()
            .dataAssertNoErrors
        val connection = data.me?.orders
        return OrderPage(
            items = connection?.edges.orEmpty().mapNotNull { it?.node?.orderDetails?.toSummary() },
            endCursor = connection?.pageInfo?.endCursor,
            hasNextPage = connection?.pageInfo?.hasNextPage == true,
            totalCount = connection?.totalCount,
        )
    }

    override suspend fun getOrderDetail(id: String): OrderDetail? {
        val data = apolloClient.query(
            CurrentUserOrderDetailQuery(
                id = id,
                languageCode = languageCode,
            ),
        )
            .fetchPolicy(FetchPolicy.NetworkOnly)
            .execute()
            .dataAssertNoErrors
        return data.me?.orders?.edges?.firstOrNull()?.node?.orderDetails?.toDetail()
    }
}

private fun OrderDetails.toSummary(): OrderSummary = OrderSummary(
    id = id,
    number = number,
    created = created.toString(),
    status = status.rawValue,
    statusDisplay = statusDisplay,
    paymentStatusDisplay = paymentStatusDisplay,
    total = total.gross.let { Money(it.amount, it.currency) },
    thumbnailUrl = lines.firstOrNull()?.thumbnail?.url
        ?: lines.firstOrNull()?.variant?.product?.thumbnail?.url,
    lineCount = lines.size,
)

private fun OrderDetails.toDetail(): OrderDetail = OrderDetail(
    id = id,
    number = number,
    created = created.toString(),
    status = status.rawValue,
    statusDisplay = statusDisplay,
    paymentStatusDisplay = paymentStatusDisplay,
    subtotal = subtotal.gross.let { Money(it.amount, it.currency) },
    shippingPrice = shippingPrice.gross.let { Money(it.amount, it.currency) },
    total = total.gross.let { Money(it.amount, it.currency) },
    shippingAddress = shippingAddress?.formatAddress(),
    lines = lines.map { line ->
        val productName = line.variant?.product?.translation?.name
            ?: line.variant?.product?.name
            ?: line.productName
        val variantName = line.variant?.translation?.name
            ?: line.variant?.name
            ?: line.variantName
        OrderLineItem(
            id = line.id,
            productName = productName,
            variantName = variantName,
            quantity = line.quantity,
            thumbnailUrl = line.thumbnail?.url ?: line.variant?.product?.thumbnail?.url,
            totalPrice = line.totalPrice.gross.let { Money(it.amount, it.currency) },
        )
    },
)

private fun OrderDetails.ShippingAddress.formatAddress(): String {
    return listOfNotNull(
        listOf(firstName, lastName).filter { !it.isNullOrBlank() }.joinToString(" ").ifBlank { null },
        streetAddress1,
        streetAddress2?.takeIf { it.isNotBlank() },
        listOfNotNull(postalCode, city).joinToString(" ").ifBlank { null },
        country.country,
        phone?.takeIf { it.isNotBlank() },
    ).joinToString("\n")
}
