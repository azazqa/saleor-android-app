package com.bdf.saleor.core.testing.fake

import com.bdf.saleor.core.data.FavoritesRepository
import com.bdf.saleor.core.model.ProductSummary
import com.bdf.saleor.core.model.Money
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeFavoritesRepository : FavoritesRepository {
    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    override val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    var favoriteProducts: List<ProductSummary> = emptyList()
    var mergeCount: Int = 0
    var clearOnLogoutCount: Int = 0
    var shouldFailToggle: Boolean = false
    var lastToggledProductId: String? = null

    override suspend fun refresh() {
        // no-op for tests
    }

    override suspend fun toggle(productId: String): Result<Unit> {
        lastToggledProductId = productId
        if (shouldFailToggle) return Result.failure(IllegalStateException("toggle failed"))
        _favoriteIds.value = if (productId in _favoriteIds.value) {
            _favoriteIds.value - productId
        } else {
            _favoriteIds.value + productId
        }
        return Result.success(Unit)
    }

    override suspend fun mergeAnonymous() {
        mergeCount += 1
    }

    override suspend fun loadFavoriteProducts(): Result<List<ProductSummary>> =
        Result.success(favoriteProducts)

    override suspend fun clearOnLogout() {
        clearOnLogoutCount += 1
        _favoriteIds.value = emptySet()
    }

    fun setFavorited(productId: String, favorited: Boolean) {
        _favoriteIds.value = if (favorited) {
            _favoriteIds.value + productId
        } else {
            _favoriteIds.value - productId
        }
    }

    companion object {
        fun sampleProduct(): ProductSummary = ProductSummary(
            id = "p1",
            name = "Tea",
            slug = "tea",
            thumbnailUrl = null,
            price = Money(10.0, "KRW"),
            categoryName = "Drinks",
        )
    }
}
