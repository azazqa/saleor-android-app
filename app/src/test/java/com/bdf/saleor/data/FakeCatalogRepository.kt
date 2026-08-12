package com.bdf.saleor.data

import com.bdf.saleor.data.model.CategoryItem
import com.bdf.saleor.data.model.HomeCatalog
import com.bdf.saleor.data.model.Money
import com.bdf.saleor.data.model.ProductDetail
import com.bdf.saleor.data.model.ProductPage
import com.bdf.saleor.data.model.ProductSummary
import com.bdf.saleor.data.model.ProductVariant
import com.bdf.saleor.data.model.VariantOption

class FakeCatalogRepository : CatalogRepository {
    var homeCatalog: HomeCatalog = HomeCatalog(
        featuredTitle = "Featured",
        featuredProducts = listOf(sampleProduct("1", "Tea")),
        categories = listOf(CategoryItem("c1", "Drinks", "drinks", null)),
    )
    var searchResults: ProductPage = ProductPage(
        items = listOf(sampleProduct("2", "Coffee")),
        endCursor = null,
        hasNextPage = false,
        totalCount = 1,
    )
    var productDetail: ProductDetail? = sampleDetail()
    var shouldFailHome: Boolean = false
    var shouldFailSearch: Boolean = false
    var shouldFailDetail: Boolean = false
    var searchQueryReceived: String? = null

    override suspend fun getHomeCatalog(): HomeCatalog {
        if (shouldFailHome) error("home failed")
        return homeCatalog
    }

    override suspend fun getCategories(first: Int): List<CategoryItem> = homeCatalog.categories

    override suspend fun getProducts(first: Int, after: String?): ProductPage {
        return ProductPage(
            items = homeCatalog.featuredProducts,
            endCursor = null,
            hasNextPage = false,
            totalCount = homeCatalog.featuredProducts.size,
        )
    }

    override suspend fun getProductsByCategory(
        slug: String,
        first: Int,
        after: String?,
    ): ProductPage = getProducts(first, after)

    override suspend fun getProductsByCollection(
        slug: String,
        first: Int,
        after: String?,
    ): ProductPage = getProducts(first, after)

    override suspend fun searchProducts(
        query: String,
        first: Int,
        after: String?,
    ): ProductPage {
        searchQueryReceived = query
        if (shouldFailSearch) error("search failed")
        return searchResults
    }

    override suspend fun getProductDetail(slug: String): ProductDetail? {
        if (shouldFailDetail) error("detail failed")
        return productDetail
    }

    companion object {
        fun sampleProduct(id: String, name: String) = ProductSummary(
            id = id,
            name = name,
            slug = name.lowercase(),
            thumbnailUrl = null,
            price = Money(10.0, "USD"),
            categoryName = "Drinks",
        )

        fun sampleDetail(
            slug: String = "tea",
            variants: List<ProductVariant> = listOf(
                ProductVariant(
                    id = "v1",
                    name = "Small",
                    quantityAvailable = 5,
                    price = Money(10.0, "USD"),
                    mediaUrls = emptyList(),
                    options = listOf(
                        VariantOption("size", "Size", "small", "Small"),
                    ),
                ),
                ProductVariant(
                    id = "v2",
                    name = "Large",
                    quantityAvailable = 3,
                    price = Money(15.0, "USD"),
                    mediaUrls = emptyList(),
                    options = listOf(
                        VariantOption("size", "Size", "large", "Large"),
                    ),
                ),
            ),
        ) = ProductDetail(
            id = "p1",
            name = "Tea",
            slug = slug,
            descriptionJson = """{"blocks":[{"type":"paragraph","data":{"text":"Nice tea"}}]}""",
            mediaUrls = listOf("https://example.com/tea.webp"),
            priceRangeStart = Money(10.0, "USD"),
            priceRangeStop = Money(15.0, "USD"),
            categoryName = "Drinks",
            categorySlug = "drinks",
            variants = variants,
        )
    }
}
