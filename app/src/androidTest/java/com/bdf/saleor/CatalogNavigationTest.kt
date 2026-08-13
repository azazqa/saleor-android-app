package com.bdf.saleor

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
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
        composeRule.onNodeWithTag("account_welcome").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_account", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun storefrontTopBar_isVisibleOnHome() {
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("home_screen").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("storefront_top_bar").assertIsDisplayed()
        composeRule.onNodeWithTag("storefront_logo").assertIsDisplayed()
    }
}
