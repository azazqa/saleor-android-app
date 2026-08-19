package com.bdf.saleor.core.data

import com.bdf.saleor.core.model.AddressDraft
import com.bdf.saleor.core.model.AddressKind
import com.bdf.saleor.core.model.PointsPage

interface AccountRepository {
    suspend fun getPoints(first: Int = PAGE_SIZE, after: String? = null): PointsPage

    suspend fun createAddress(draft: AddressDraft, defaultKind: AddressKind? = null): Result<String?>

    suspend fun updateAddress(id: String, draft: AddressDraft): Result<String?>

    suspend fun deleteAddress(id: String): Result<String?>

    suspend fun setDefaultAddress(id: String, kind: AddressKind): Result<String?>

    suspend fun requestAccountDeletion(): Result<String?>

    companion object {
        const val PAGE_SIZE = 20
    }
}
