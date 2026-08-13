package com.bdf.saleor.data

import com.bdf.saleor.data.model.Money
import com.bdf.saleor.data.model.OrderDetail
import com.bdf.saleor.data.model.OrderLineItem
import com.bdf.saleor.data.model.OrderPage
import com.bdf.saleor.data.model.OrderSummary

class FakeOrderRepository : OrderRepository {
    var orders: OrderPage = OrderPage(
        items = listOf(sampleSummary()),
        endCursor = null,
        hasNextPage = false,
        totalCount = 1,
    )
    var detail: OrderDetail? = sampleDetail()
    var shouldFailList: Boolean = false
    var shouldFailDetail: Boolean = false

    override suspend fun getOrders(first: Int, after: String?): OrderPage {
        if (shouldFailList) error("orders failed")
        return orders
    }

    override suspend fun getOrderDetail(id: String): OrderDetail? {
        if (shouldFailDetail) error("order detail failed")
        return detail?.takeIf { it.id == id } ?: detail
    }

    companion object {
        fun sampleSummary() = OrderSummary(
            id = "o1",
            number = "12",
            created = "2024-05-01T10:00:00+00:00",
            status = "UNFULFILLED",
            statusDisplay = "Unfulfilled",
            paymentStatusDisplay = "Fully charged",
            total = Money(25.0, "USD"),
            thumbnailUrl = null,
            lineCount = 1,
        )

        fun sampleDetail() = OrderDetail(
            id = "o1",
            number = "12",
            created = "2024-05-01T10:00:00+00:00",
            status = "UNFULFILLED",
            statusDisplay = "Unfulfilled",
            paymentStatusDisplay = "Fully charged",
            subtotal = Money(20.0, "USD"),
            shippingPrice = Money(5.0, "USD"),
            total = Money(25.0, "USD"),
            shippingAddress = "Test User\n123 Street\nSeoul",
            lines = listOf(
                OrderLineItem(
                    id = "l1",
                    productName = "Tea",
                    variantName = "Small",
                    quantity = 2,
                    thumbnailUrl = null,
                    totalPrice = Money(20.0, "USD"),
                ),
            ),
        )
    }
}
