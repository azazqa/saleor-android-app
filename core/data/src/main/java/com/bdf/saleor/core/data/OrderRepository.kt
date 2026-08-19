package com.bdf.saleor.core.data

import com.bdf.saleor.core.model.OrderDetail
import com.bdf.saleor.core.model.OrderPage

interface OrderRepository {
    suspend fun getOrders(
        first: Int = PAGE_SIZE,
        after: String? = null,
    ): OrderPage

    suspend fun getOrderDetail(id: String): OrderDetail?

    companion object {
        const val PAGE_SIZE = 10
    }
}
