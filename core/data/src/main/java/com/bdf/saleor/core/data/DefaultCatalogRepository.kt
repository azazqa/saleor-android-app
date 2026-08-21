package com.bdf.saleor.core.data

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
import com.bdf.saleor.core.model.CategoryItem
import com.bdf.saleor.core.model.HomeCatalog
import com.bdf.saleor.core.model.Money
import com.bdf.saleor.core.model.ProductDetail
import com.bdf.saleor.core.model.ProductPage
import com.bdf.saleor.core.model.ProductSummary
import com.bdf.saleor.core.model.ProductVariant
import com.bdf.saleor.core.model.VariantOption
import com.bdf.saleor.graphql.CategoriesQuery
import com.bdf.saleor.graphql.ProductDetailsQuery
import com.bdf.saleor.graphql.ProductListByCategoryQuery
import com.bdf.saleor.graphql.ProductListByCollectionQuery
import com.bdf.saleor.graphql.ProductListPaginatedQuery
import com.bdf.saleor.graphql.SearchProductsQuery
import com.bdf.saleor.graphql.fragment.ProductListItem
import com.bdf.saleor.graphql.type.LanguageCodeEnum
import com.bdf.saleor.core.network.ProductCmsApi
import com.bdf.saleor.core.network.SaleorCatalogConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultCatalogRepository @Inject constructor(
    private val apolloClient: ApolloClient,
    private val config: SaleorCatalogConfig,
    private val cmsApi: ProductCmsApi,
) : CatalogRepository {
    private val channel: String get() = config.channel
    private val featuredCollectionSlug: String get() = config.featuredCollectionSlug
    private val languageCode: LanguageCodeEnum
        get() = LanguageCodeEnum.safeValueOf(config.graphqlLanguageCode)

    override suspend fun getHomeCatalog(): HomeCatalog {
        val categories = getCategories()
        val featured = runCatching {
            getProductsByCollection(featuredCollectionSlug, first = 8)
        }.getOrElse {
            getProducts(first = 8)
        }
        return HomeCatalog(
            featuredTitle = featured.title ?: "Featured",
            featuredProducts = featured.items,
            categories = categories,
        )
    }

    override suspend fun getCategories(first: Int): List<CategoryItem> {
        val data = apolloClient.query(
            CategoriesQuery(first = first, languageCode = languageCode),
        )
            .fetchPolicy(FetchPolicy.CacheFirst)
            .execute()
            .dataAssertNoErrors
        return data.categories?.edges.orEmpty().mapNotNull { edge ->
            edge?.node?.let { node ->
                CategoryItem(
                    id = node.id,
                    name = node.translation?.name ?: node.name,
                    slug = node.slug,
                    imageUrl = node.backgroundImage?.url,
                    children = node.children?.edges.orEmpty().mapNotNull { childEdge ->
                        childEdge?.node?.let { child ->
                            CategoryItem(
                                id = child.id,
                                name = child.translation?.name ?: child.name,
                                slug = child.slug,
                                imageUrl = null,
                            )
                        }
                    },
                )
            }
        }
    }

    override suspend fun getProducts(
        first: Int,
        after: String?,
    ): ProductPage {
        val data = apolloClient.query(
            ProductListPaginatedQuery(
                first = first,
                after = Optional.presentIfNotNull(after),
                channel = channel,
                languageCode = languageCode,
                sortBy = Optional.Absent,
            ),
        )
            .fetchPolicy(FetchPolicy.CacheFirst)
            .execute()
            .dataAssertNoErrors
        val connection = data.products
        return ProductPage(
            items = connection?.edges.orEmpty().mapNotNull { it?.node?.productListItem?.toSummary() },
            endCursor = connection?.pageInfo?.endCursor,
            hasNextPage = connection?.pageInfo?.hasNextPage == true,
            totalCount = connection?.totalCount,
            title = null,
        )
    }

    override suspend fun getProductsByCategory(
        slug: String,
        first: Int,
        after: String?,
    ): ProductPage {
        val data = apolloClient.query(
            ProductListByCategoryQuery(
                slug = slug,
                channel = channel,
                languageCode = languageCode,
                first = first,
                after = Optional.presentIfNotNull(after),
                sortBy = Optional.Absent,
            ),
        )
            .fetchPolicy(FetchPolicy.CacheFirst)
            .execute()
            .dataAssertNoErrors
        val category = data.category
        val connection = category?.products
        return ProductPage(
            items = connection?.edges.orEmpty().mapNotNull { it?.node?.productListItem?.toSummary() },
            endCursor = connection?.pageInfo?.endCursor,
            hasNextPage = connection?.pageInfo?.hasNextPage == true,
            totalCount = connection?.totalCount,
            title = category?.translation?.name ?: category?.name,
        )
    }

    override suspend fun getProductsByCollection(
        slug: String,
        first: Int,
        after: String?,
    ): ProductPage {
        val data = apolloClient.query(
            ProductListByCollectionQuery(
                slug = slug,
                channel = channel,
                languageCode = languageCode,
                first = first,
                after = Optional.presentIfNotNull(after),
                sortBy = Optional.Absent,
            ),
        )
            .fetchPolicy(FetchPolicy.CacheFirst)
            .execute()
            .dataAssertNoErrors
        val collection = data.collection
        val connection = collection?.products
        return ProductPage(
            items = connection?.edges.orEmpty().mapNotNull { it?.node?.productListItem?.toSummary() },
            endCursor = connection?.pageInfo?.endCursor,
            hasNextPage = connection?.pageInfo?.hasNextPage == true,
            totalCount = connection?.totalCount,
            title = collection?.translation?.name ?: collection?.name,
        )
    }

    override suspend fun searchProducts(
        query: String,
        first: Int,
        after: String?,
    ): ProductPage {
        val data = apolloClient.query(
            SearchProductsQuery(
                search = query,
                channel = channel,
                languageCode = languageCode,
                first = first,
                after = Optional.presentIfNotNull(after),
            ),
        )
            .fetchPolicy(FetchPolicy.NetworkOnly)
            .execute()
            .dataAssertNoErrors
        val connection = data.products
        return ProductPage(
            items = connection?.edges.orEmpty().mapNotNull { it?.node?.productListItem?.toSummary() },
            endCursor = connection?.pageInfo?.endCursor,
            hasNextPage = connection?.pageInfo?.hasNextPage == true,
            totalCount = connection?.totalCount,
            title = null,
        )
    }

    override suspend fun getProductDetail(slug: String): ProductDetail? {
        val data = apolloClient.query(
            ProductDetailsQuery(
                slug = slug,
                channel = channel,
                languageCode = languageCode,
            ),
        )
            .fetchPolicy(FetchPolicy.NetworkFirst)
            .execute()
            .dataAssertNoErrors
        val product = data.product ?: return null
        val mediaUrls = product.media
            .orEmpty()
            .mapNotNull { it?.url }
            .ifEmpty { listOfNotNull(product.thumbnail?.url) }
        val startGross = product.pricing?.priceRange?.start?.gross
        val stopGross = product.pricing?.priceRange?.stop?.gross
        val cmsBlocks = cmsApi.fetchBlocks(product.slug)
        return ProductDetail(
            id = product.id,
            name = product.translation?.name ?: product.name,
            slug = product.slug,
            descriptionJson = jsonScalarToString(product.translation?.description)
                ?: jsonScalarToString(product.description)
                ?: jsonScalarToString(product.descriptionJson),
            mediaUrls = mediaUrls,
            priceRangeStart = startGross?.let { Money(it.amount, it.currency) },
            priceRangeStop = stopGross?.let { Money(it.amount, it.currency) },
            categoryName = product.category?.translation?.name ?: product.category?.name,
            categorySlug = product.category?.slug,
            cmsBlocks = cmsBlocks,
            variants = product.variants.orEmpty().mapNotNull { variant ->
                variant?.let {
                    val priceGross = it.pricing?.price?.gross
                    ProductVariant(
                        id = it.id,
                        name = it.translation?.name ?: it.name,
                        quantityAvailable = it.quantityAvailable,
                        price = priceGross?.let { g -> Money(g.amount, g.currency) },
                        mediaUrls = it.media.orEmpty().mapNotNull { media -> media?.url },
                        options = it.selectionAttributes.flatMap { attr ->
                            val attribute = attr.attribute
                            attr.values.map { value ->
                                VariantOption(
                                    attributeSlug = attribute.slug.orEmpty(),
                                    attributeName = attribute.translation?.name ?: attribute.name.orEmpty(),
                                    valueSlug = value.slug.orEmpty(),
                                    valueName = value.translation?.name ?: value.name.orEmpty(),
                                )
                            }
                        },
                    )
                }
            },
        )
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
