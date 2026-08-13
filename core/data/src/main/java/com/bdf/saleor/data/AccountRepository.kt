package com.bdf.saleor.data

import com.bdf.saleor.data.model.AddressDraft
import com.bdf.saleor.data.model.AddressKind
import com.bdf.saleor.data.model.AuthResult
import com.bdf.saleor.data.model.PointsPage

interface AccountRepository {
    suspend fun getPoints(first: Int = PAGE_SIZE, after: String? = null): PointsPage

    suspend fun createAddress(draft: AddressDraft, defaultKind: AddressKind? = null): AuthResult

    suspend fun updateAddress(id: String, draft: AddressDraft): AuthResult

    suspend fun deleteAddress(id: String): AuthResult

    suspend fun setDefaultAddress(id: String, kind: AddressKind): AuthResult

    suspend fun requestAccountDeletion(): AuthResult

    companion object {
        const val PAGE_SIZE = 20
    }
}
