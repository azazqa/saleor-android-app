package com.bdf.saleor

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class CatalogNavigationTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun home_toProductDetail_andBack() {
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("home_screen").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("product_card_tea").assertIsDisplayed().performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("product_detail_screen").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("product_detail_name").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_home", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("nav_account", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("nav_home", useUnmergedTree = true).assertIsSelected()
        composeRule.onNodeWithTag("nav_account", useUnmergedTree = true).assertIsNotSelected()
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("home_screen").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("home_screen").assertIsDisplayed()
    }

    @Test
    fun search_showsResults() {
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("home_screen").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("nav_search", useUnmergedTree = true).performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("search_screen").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("nav_search", useUnmergedTree = true).assertIsSelected()
        composeRule.onNodeWithTag("nav_home", useUnmergedTree = true).assertIsNotSelected()
        composeRule.onNodeWithTag("search_field").performTextInput("cof")
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("product_card_coffee").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("product_card_coffee").assertIsDisplayed()
    }

    @Test
    fun accountTab_login_showsMyPage() {
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("home_screen").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("nav_account", useUnmergedTree = true).performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("login_screen").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("login_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("login_email").performTextInput("user@test.com")
        composeRule.onNodeWithTag("login_password").performTextInput("password")
        composeRule.onNodeWithTag("login_submit").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("account_screen").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("account_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("account_tab_overview", useUnmergedTree = true).assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithTag("account_tab_logout", useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
        composeRule.onNodeWithTag("account_welcome").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_account", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("account_tab_settings", useUnmergedTree = true).performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("account_logout").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("account_logout", useUnmergedTree = true)
            .performScrollTo()
            .performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("account_logout_confirm", useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithTag("account_logout_confirm", useUnmergedTree = true).performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("login_screen").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("login_screen").assertIsDisplayed()
    }

    @Test
    fun storefrontTopBar_isVisibleOnHome() {
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("home_screen").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("storefront_top_bar").assertIsDisplayed()
        composeRule.onNodeWithTag("storefront_logo").assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithTag("storefront_login").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithTag("storefront_logout").fetchSemanticsNodes().isEmpty())
        composeRule.onNodeWithTag("storefront_cart").assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithTag("storefront_account").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun addToCart_opensCartAndUpdatesQuantity() {
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("home_screen").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("product_card_tea").assertIsDisplayed().performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("product_detail_screen").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("add_to_cart").assertIsDisplayed().performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("product_detail_cart_badge", useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithText("장바구니 보기").performClick()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithTag("cart_screen").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("cart_screen").assertIsDisplayed()
        assertTrue(
            composeRule.onAllNodesWithTag("nav_home", useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isEmpty(),
        )
        composeRule.onNodeWithTag("cart_line_v1").assertIsDisplayed()
        composeRule.onNodeWithTag("cart_qty").assertIsDisplayed()
        composeRule.onNodeWithTag("cart_qty_increase").performClick()
        composeRule.onNodeWithTag("cart_checkout").assertIsDisplayed()
    }

    @Test
    fun storefrontLogo_navigatesToHome() {
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("home_screen").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("nav_search", useUnmergedTree = true).performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("search_screen").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("storefront_logo").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("home_screen").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("home_screen").assertIsDisplayed()
    }

    @Test
    fun home_scrollDownHidesBottomNav_scrollUpShowsIt() {
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("product_card_tea").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("nav_home", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNode(hasScrollToIndexAction()).performTouchInput { swipeUp() }
        composeRule.waitUntil(5_000) {
            !isNavHomeDisplayed()
        }
        assertTrue(!isNavHomeDisplayed())
        composeRule.onNode(hasScrollToIndexAction()).performTouchInput { swipeDown() }
        composeRule.waitUntil(5_000) {
            isNavHomeDisplayed()
        }
        composeRule.onNodeWithTag("nav_home", useUnmergedTree = true).assertIsDisplayed()
    }

    private fun isNavHomeDisplayed(): Boolean {
        val nodes = composeRule.onAllNodesWithTag("nav_home", useUnmergedTree = true)
            .fetchSemanticsNodes()
        if (nodes.isEmpty()) return false
        return runCatching {
            composeRule.onNodeWithTag("nav_home", useUnmergedTree = true).assertIsDisplayed()
        }.isSuccess
    }
}
