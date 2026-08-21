package com.bdf.saleor.core.data

import com.bdf.saleor.core.model.ProductSummary
import kotlinx.coroutines.flow.StateFlow

interface FavoritesRepository {
    val favoriteIds: StateFlow<Set<String>>

    suspend fun refresh()

    suspend fun toggle(productId: String): Result<Unit>

    suspend fun mergeAnonymous()

    suspend fun loadFavoriteProducts(): Result<List<ProductSummary>>

    suspend fun clearOnLogout()
}
