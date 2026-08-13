package com.bdf.saleor.data

import com.bdf.saleor.data.model.OrderDetail
import com.bdf.saleor.data.model.OrderPage

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
