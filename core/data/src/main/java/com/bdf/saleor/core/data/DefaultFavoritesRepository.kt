package com.bdf.saleor.core.data

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
import com.bdf.saleor.core.datastore.FavoritesTokenStore
import com.bdf.saleor.core.datastore.TokenStore
import com.bdf.saleor.core.model.Money
import com.bdf.saleor.core.model.ProductSummary
import com.bdf.saleor.core.model.SaleorException
import com.bdf.saleor.core.network.SaleorCatalogConfig
import com.bdf.saleor.graphql.MeProductFavoritesIdsQuery
import com.bdf.saleor.graphql.ProductFavoriteAddMutation
import com.bdf.saleor.graphql.ProductFavoriteRemoveMutation
import com.bdf.saleor.graphql.ProductFavoritesIdsQuery
import com.bdf.saleor.graphql.ProductFavoritesMergeMutation
import com.bdf.saleor.graphql.ProductFavoritesProductsQuery
import com.bdf.saleor.graphql.fragment.ProductListItem
import com.bdf.saleor.graphql.type.LanguageCodeEnum
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Singleton
class DefaultFavoritesRepository @Inject constructor(
    private val apolloClient: ApolloClient,
    @Named("bare") private val bareClient: ApolloClient,
    private val tokenStore: TokenStore,
    private val favoritesTokenStore: FavoritesTokenStore,
    private val config: SaleorCatalogConfig,
) : FavoritesRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    override val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    private val languageCode: LanguageCodeEnum
        get() = LanguageCodeEnum.safeValueOf(config.graphqlLanguageCode)

    init {
        scope.launch { refresh() }
    }

    private fun isLoggedIn(): Boolean = !tokenStore.accessToken().isNullOrBlank()

    override suspend fun refresh() {
        _favoriteIds.value = runCatching { fetchFavoriteIds() }.getOrDefault(emptySet())
    }

    override suspend fun toggle(productId: String): Result<Unit> = runCatching {
        val wasFavorited = productId in _favoriteIds.value
        _favoriteIds.update {
            if (wasFavorited) it - productId else it + productId
        }
        try {
            if (wasFavorited) {
                removeFavorite(productId)
            } else {
                addFavorite(productId)
            }
        } catch (error: Throwable) {
            _favoriteIds.update {
                if (wasFavorited) it + productId else it - productId
            }
            throw error
        }
    }

    override suspend fun mergeAnonymous() {
        val token = favoritesTokenStore.token() ?: return
        try {
            val data = apolloClient.mutation(ProductFavoritesMergeMutation(token))
                .execute()
                .dataAssertNoErrors
            val errors = data.productFavoritesMerge?.errors.orEmpty()
            if (errors.isNotEmpty()) {
                android.util.Log.w(
                    TAG,
                    "Favorites merge failed: ${errors.firstOrNull()?.message}",
                )
            }
        } catch (error: Throwable) {
            android.util.Log.w(TAG, "Favorites merge failed", error)
        } finally {
            favoritesTokenStore.clear()
        }
        refresh()
    }

    override suspend fun loadFavoriteProducts(): Result<List<ProductSummary>> = runCatching {
        val orderedIds = fetchOrderedFavoriteIds()
        if (orderedIds.isEmpty()) return@runCatching emptyList()

        val data = apolloClient.query(
            ProductFavoritesProductsQuery(
                channel = config.channel,
                ids = orderedIds,
                languageCode = languageCode,
            ),
        )
            .fetchPolicy(FetchPolicy.NetworkOnly)
            .execute()
            .dataAssertNoErrors

        val byId = data.products?.edges.orEmpty()
            .mapNotNull { it?.node?.productListItem }
            .associateBy { it.id }
            .mapValues { (_, item) -> item.toSummary() }

        orderedIds.mapNotNull { byId[it] }
    }

    override suspend fun clearOnLogout() {
        _favoriteIds.value = emptySet()
        val token = favoritesTokenStore.token()
        if (token != null) {
            refresh()
        }
    }

    private suspend fun fetchFavoriteIds(): Set<String> {
        if (isLoggedIn()) {
            return fetchLoggedInFavoriteIds()
        }
        val token = favoritesTokenStore.token() ?: return emptySet()
        return fetchGuestFavoriteIds(token)
    }

    private suspend fun fetchLoggedInFavoriteIds(): Set<String> {
        val data = apolloClient.query(MeProductFavoritesIdsQuery())
            .fetchPolicy(FetchPolicy.NetworkOnly)
            .execute()
            .dataAssertNoErrors
        return data.me?.productFavorites.orEmpty()
            .mapNotNull { it?.product?.id }
            .toSet()
    }

    private suspend fun fetchGuestFavoriteIds(token: String): Set<String> =
        fetchOrderedGuestFavoriteIds(token).toSet()

    private suspend fun fetchOrderedFavoriteIds(): List<String> {
        if (isLoggedIn()) {
            val data = apolloClient.query(MeProductFavoritesIdsQuery())
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()
                .dataAssertNoErrors
            return data.me?.productFavorites.orEmpty()
                .mapNotNull { it?.product?.id }
        }
        val token = favoritesTokenStore.token() ?: return emptyList()
        return fetchOrderedGuestFavoriteIds(token)
    }

    private suspend fun fetchOrderedGuestFavoriteIds(token: String): List<String> {
        val data = bareClient.query(ProductFavoritesIdsQuery(token))
            .fetchPolicy(FetchPolicy.NetworkOnly)
            .execute()
            .dataAssertNoErrors
        return data.productFavorites.orEmpty()
            .mapNotNull { it?.product?.id }
    }

    private suspend fun addFavorite(productId: String) {
        if (isLoggedIn()) {
            val data = apolloClient.mutation(
                ProductFavoriteAddMutation(productId = productId),
            ).execute().dataAssertNoErrors
            val errors = data.productFavoriteAdd?.errors.orEmpty()
            if (errors.isNotEmpty()) {
                throw SaleorException(errors.firstOrNull()?.message ?: "즐겨찾기 추가에 실패했습니다")
            }
            return
        }
        val existingToken = favoritesTokenStore.token()
        val data = bareClient.mutation(
            ProductFavoriteAddMutation(
                productId = productId,
                token = Optional.presentIfNotNull(existingToken),
            ),
        ).execute().dataAssertNoErrors
        val payload = data.productFavoriteAdd
        val errors = payload?.errors.orEmpty()
        if (errors.isNotEmpty()) {
            throw SaleorException(errors.firstOrNull()?.message ?: "즐겨찾기 추가에 실패했습니다")
        }
        payload?.token?.toString()?.takeIf { it.isNotBlank() }?.let { favoritesTokenStore.setToken(it) }
    }

    private suspend fun removeFavorite(productId: String) {
        if (isLoggedIn()) {
            val data = apolloClient.mutation(
                ProductFavoriteRemoveMutation(productId = productId),
            ).execute().dataAssertNoErrors
            val errors = data.productFavoriteRemove?.errors.orEmpty()
            if (errors.isNotEmpty()) {
                throw SaleorException(errors.firstOrNull()?.message ?: "즐겨찾기 해제에 실패했습니다")
            }
            return
        }
        val token = favoritesTokenStore.token()
            ?: throw SaleorException("즐겨찾기 토큰이 없습니다")
        val data = bareClient.mutation(
            ProductFavoriteRemoveMutation(
                productId = productId,
                token = Optional.present(token),
            ),
        ).execute().dataAssertNoErrors
        val errors = data.productFavoriteRemove?.errors.orEmpty()
        if (errors.isNotEmpty()) {
            throw SaleorException(errors.firstOrNull()?.message ?: "즐겨찾기 해제에 실패했습니다")
        }
    }

    private companion object {
        const val TAG = "FavoritesRepository"
    }
}

private fun ProductListItem.toSummary(): ProductSummary {
    val start = pricing?.priceRange?.start?.gross
    return ProductSummary(
        id = id,
        name = translation?.name ?: name,
        slug = slug,
        thumbnailUrl = thumbnail?.url,
        price = start?.let { Money(it.amount, it.currency) },
        categoryName = category?.translation?.name ?: category?.name,
    )
}
