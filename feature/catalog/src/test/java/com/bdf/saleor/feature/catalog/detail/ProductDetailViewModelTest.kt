package com.bdf.saleor.feature.catalog.detail

import com.bdf.saleor.core.model.Money
import com.bdf.saleor.core.model.ProductCmsBlock
import com.bdf.saleor.core.model.ProductVariant
import com.bdf.saleor.core.model.VariantOption
import com.bdf.saleor.core.testing.MainDispatcherRule
import com.bdf.saleor.core.testing.fake.FakeCartRepository
import com.bdf.saleor.core.testing.fake.FakeCatalogRepository
import com.bdf.saleor.core.testing.fake.FakeFavoritesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProductDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun refresh_multiVariant_doesNotAutoSelect() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = ProductDetailViewModel(
            repository = FakeCatalogRepository(),
            cartRepository = FakeCartRepository(),
            favoritesRepository = FakeFavoritesRepository(),
            slug = "tea",
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals("Tea", state.product?.name)
        assertNull(state.selectedVariantId)
        assertTrue(state.usesOptionSheet)
        assertEquals(10.0, state.displayPrice?.amount ?: -1.0, 0.0)
        assertTrue(state.descriptionText.contains("Nice tea"))
        assertTrue(state.descriptionBlocks.isNotEmpty())
    }

    @Test
    fun refresh_singleVariant_stillUsesOptionSheet() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeCatalogRepository().apply {
            productDetail = FakeCatalogRepository.sampleDetail(
                variants = listOf(
                    ProductVariant(
                        id = "v1",
                        name = "Default",
                        quantityAvailable = 5,
                        price = Money(10.0, "USD"),
                        mediaUrls = emptyList(),
                        options = emptyList(),
                    ),
                ),
            )
        }
        val viewModel = ProductDetailViewModel(
            repository = repository,
            cartRepository = FakeCartRepository(),
            favoritesRepository = FakeFavoritesRepository(),
            slug = "tea",
        )
        advanceUntilIdle()

        assertEquals("v1", viewModel.uiState.value.selectedVariantId)
        assertTrue(viewModel.uiState.value.usesOptionSheet)

        viewModel.openOptionSheet()
        assertTrue(viewModel.uiState.value.optionSheetOpen)
        assertEquals(1, viewModel.uiState.value.optionSheet.selected.size)
        assertEquals("v1", viewModel.uiState.value.optionSheet.selected.single().variantId)
        assertEquals(1, viewModel.uiState.value.optionSheet.selected.single().quantity)
    }

    @Test
    fun refresh_exposesCmsBlocks() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeCatalogRepository().apply {
            productDetail = FakeCatalogRepository.sampleDetail().copy(
                cmsBlocks = listOf(ProductCmsBlock.Heading("상품설명", 1)),
            )
        }
        val viewModel = ProductDetailViewModel(
            repository = repository,
            cartRepository = FakeCartRepository(),
            favoritesRepository = FakeFavoritesRepository(),
            slug = "tea",
        )
        advanceUntilIdle()

        val heading = viewModel.uiState.value.product?.cmsBlocks?.single() as ProductCmsBlock.Heading
        assertEquals("상품설명", heading.text)
        assertEquals(1, heading.level)
    }

    @Test
    fun refresh_notFound_setsNotFoundError() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeCatalogRepository().apply { productDetail = null }
        val viewModel = ProductDetailViewModel(
            repository = repository,
            cartRepository = FakeCartRepository(),
            favoritesRepository = FakeFavoritesRepository(),
            slug = "missing",
        )

        advanceUntilIdle()

        assertEquals("not_found", viewModel.uiState.value.error)
    }

    @Test
    fun singleVariant_purchaseViaSheet_setsBuyNowReady() = runTest(mainDispatcherRule.dispatcher) {
        val cart = FakeCartRepository()
        val repository = FakeCatalogRepository().apply {
            productDetail = FakeCatalogRepository.sampleDetail(
                variants = listOf(
                    ProductVariant(
                        id = "v1",
                        name = "Default",
                        quantityAvailable = 5,
                        price = Money(10.0, "USD"),
                        mediaUrls = emptyList(),
                        options = emptyList(),
                    ),
                ),
            )
        }
        val viewModel = ProductDetailViewModel(
            repository = repository,
            cartRepository = cart,
            favoritesRepository = FakeFavoritesRepository(),
            slug = "tea",
        )
        advanceUntilIdle()

        viewModel.openOptionSheet()
        viewModel.buyNow()
        advanceUntilIdle()

        assertEquals("v1", cart.lastAddedVariantId)
        assertEquals(1, cart.lastAddedQuantity)
        assertTrue(viewModel.uiState.value.buyNowReady)
        assertNull(viewModel.uiState.value.addToCartMessage)
    }

    @Test
    fun toggleFavorite_updatesFavoritedState() = runTest(mainDispatcherRule.dispatcher) {
        val favorites = FakeFavoritesRepository()
        val viewModel = ProductDetailViewModel(
            repository = FakeCatalogRepository(),
            cartRepository = FakeCartRepository(),
            favoritesRepository = favorites,
            slug = "tea",
        )
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isFavorited)

        viewModel.toggleFavorite()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isFavorited)
        assertEquals("p1", favorites.lastToggledProductId)
    }

    @Test
    fun availableTabs_excludesEmptySections() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeCatalogRepository().apply {
            productDetail = FakeCatalogRepository.sampleDetail().copy(
                descriptionJson = null,
                cmsBlocks = emptyList(),
            )
        }
        val viewModel = ProductDetailViewModel(
            repository = repository,
            cartRepository = FakeCartRepository(),
            favoritesRepository = FakeFavoritesRepository(),
            slug = "tea",
        )
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.availableTabs.isEmpty())
    }

    @Test
    fun discountPercent_whenUndiscountedHigher() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeCatalogRepository().apply {
            productDetail = FakeCatalogRepository.sampleDetail(
                variants = listOf(
                    ProductVariant(
                        id = "v1",
                        name = "Small",
                        quantityAvailable = 5,
                        price = Money(60.0, "KRW"),
                        priceUndiscounted = Money(70.0, "KRW"),
                        mediaUrls = emptyList(),
                        options = emptyList(),
                    ),
                ),
            )
        }
        val viewModel = ProductDetailViewModel(
            repository = repository,
            cartRepository = FakeCartRepository(),
            favoritesRepository = FakeFavoritesRepository(),
            slug = "tea",
        )
        advanceUntilIdle()

        assertEquals(14, viewModel.uiState.value.discountPercent)
        assertEquals(70.0, viewModel.uiState.value.displayUndiscountedPrice?.amount ?: -1.0, 0.0)
    }

    @Test
    fun relatedProducts_excludesCurrentProduct() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = ProductDetailViewModel(
            repository = FakeCatalogRepository(),
            cartRepository = FakeCartRepository(),
            favoritesRepository = FakeFavoritesRepository(),
            slug = "tea",
        )
        advanceUntilIdle()

        val related = viewModel.uiState.value.relatedProducts
        assertTrue(related.isNotEmpty())
        assertTrue(related.none { it.id == "p1" })
    }

    @Test
    fun sheet_selectOption_addsLine_andReselectIncrements() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = ProductDetailViewModel(
            repository = FakeCatalogRepository(),
            cartRepository = FakeCartRepository(),
            favoritesRepository = FakeFavoritesRepository(),
            slug = "tea",
        )
        advanceUntilIdle()

        viewModel.openOptionSheet()
        viewModel.selectSheetOption("v1")
        assertEquals(1, viewModel.uiState.value.optionSheet.selected.size)
        assertEquals(1, viewModel.uiState.value.optionSheet.selected.single().quantity)
        assertTrue(viewModel.uiState.value.optionSheet.canSubmit)

        viewModel.selectSheetOption("v1")
        assertEquals(1, viewModel.uiState.value.optionSheet.selected.size)
        assertEquals(2, viewModel.uiState.value.optionSheet.selected.single().quantity)
    }

    @Test
    fun sheet_removeLastLine_keepsSheetOpen_andCanSubmitFalse() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = ProductDetailViewModel(
            repository = FakeCatalogRepository(),
            cartRepository = FakeCartRepository(),
            favoritesRepository = FakeFavoritesRepository(),
            slug = "tea",
        )
        advanceUntilIdle()

        viewModel.openOptionSheet()
        viewModel.selectSheetOption("v1")
        viewModel.removeSheetLine("v1")

        assertTrue(viewModel.uiState.value.optionSheetOpen)
        assertTrue(viewModel.uiState.value.optionSheet.selected.isEmpty())
        assertFalse(viewModel.uiState.value.optionSheet.canSubmit)
    }

    @Test
    fun sheet_totalPrice_sumsLineAmounts() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = ProductDetailViewModel(
            repository = FakeCatalogRepository(),
            cartRepository = FakeCartRepository(),
            favoritesRepository = FakeFavoritesRepository(),
            slug = "tea",
        )
        advanceUntilIdle()

        viewModel.openOptionSheet()
        viewModel.selectSheetOption("v1") // 10
        viewModel.selectSheetOption("v1") // qty 2 -> 20
        viewModel.selectSheetOption("v2") // 15
        // total 35

        assertEquals(3, viewModel.uiState.value.optionSheet.totalCount)
        assertEquals(35.0, viewModel.uiState.value.optionSheet.totalPrice?.amount ?: -1.0, 0.0)
    }

    @Test
    fun sheet_incrementAtLimit_setsQuantityLimitMessage() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = ProductDetailViewModel(
            repository = FakeCatalogRepository(),
            cartRepository = FakeCartRepository(),
            favoritesRepository = FakeFavoritesRepository(),
            slug = "tea",
        )
        advanceUntilIdle()

        viewModel.openOptionSheet()
        viewModel.selectSheetOption("v1")
        repeat(10) { viewModel.incrementSheetLine("v1") }

        assertEquals(5, viewModel.uiState.value.optionSheet.selected.single().quantity)
        assertEquals("5", viewModel.uiState.value.quantityLimitMessage)
    }

    @Test
    fun dismissOptionSheet_clearsSelection() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = ProductDetailViewModel(
            repository = FakeCatalogRepository(),
            cartRepository = FakeCartRepository(),
            favoritesRepository = FakeFavoritesRepository(),
            slug = "tea",
        )
        advanceUntilIdle()

        viewModel.openOptionSheet()
        viewModel.selectSheetOption("v1")
        viewModel.dismissOptionSheet()

        assertFalse(viewModel.uiState.value.optionSheetOpen)
        assertTrue(viewModel.uiState.value.optionSheet.selected.isEmpty())
    }

    @Test
    fun sheet_purchase_addsAllSelectedLines_andSetsBuyNowReady() = runTest(mainDispatcherRule.dispatcher) {
        val cart = FakeCartRepository()
        val viewModel = ProductDetailViewModel(
            repository = FakeCatalogRepository(),
            cartRepository = cart,
            favoritesRepository = FakeFavoritesRepository(),
            slug = "tea",
        )
        advanceUntilIdle()

        viewModel.openOptionSheet()
        viewModel.selectSheetOption("v1")
        viewModel.selectSheetOption("v2")
        viewModel.selectSheetOption("v2")
        viewModel.buyNow()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.buyNowReady)
        assertNull(viewModel.uiState.value.addToCartMessage)
        assertFalse(viewModel.uiState.value.optionSheetOpen)
        assertEquals(3, cart.cart.value?.quantity)
        assertTrue(cart.cart.value?.lines?.any { it.variantId == "v1" && it.quantity == 1 } == true)
        assertTrue(cart.cart.value?.lines?.any { it.variantId == "v2" && it.quantity == 2 } == true)
    }

    @Test
    fun variantDisplayName_usesOptionValueNames() {
        val variant = ProductVariant(
            id = "v1",
            name = "ignored",
            quantityAvailable = 1,
            price = Money(1.0, "KRW"),
            mediaUrls = emptyList(),
            options = listOf(
                VariantOption("stage", "단계", "1", "1단계"),
                VariantOption("size", "용량", "800", "800g"),
            ),
        )
        assertEquals("1단계 / 800g", variantDisplayName(variant))
    }
}
