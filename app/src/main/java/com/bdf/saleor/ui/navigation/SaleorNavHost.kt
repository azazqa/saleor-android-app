package com.bdf.saleor.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.bdf.saleor.R
import com.bdf.saleor.core.network.BuildConfig
import com.bdf.saleor.ui.account.AccountRoute
import com.bdf.saleor.ui.account.ForgotPasswordScreen
import com.bdf.saleor.ui.account.OrderDetailScreen
import com.bdf.saleor.ui.account.OrderDetailViewModel
import com.bdf.saleor.ui.account.OrderListScreen
import com.bdf.saleor.ui.account.RegisterScreen
import com.bdf.saleor.ui.catalog.ProductListArgs
import com.bdf.saleor.ui.catalog.ProductListScreen
import com.bdf.saleor.ui.catalog.ProductListSource
import com.bdf.saleor.ui.catalog.ProductListViewModel
import com.bdf.saleor.ui.category.CategoryListScreen
import com.bdf.saleor.ui.detail.ProductDetailScreen
import com.bdf.saleor.ui.detail.ProductDetailViewModel
import com.bdf.saleor.ui.home.HomeScreen
import com.bdf.saleor.ui.search.SearchScreen

private enum class TopLevelDestination(
    val key: NavKey,
    val labelRes: Int,
    val icon: ImageVector,
    val testTag: String,
) {
    HOME(Home, R.string.nav_home, Icons.Default.Home, "nav_home"),
    CATEGORIES(Categories, R.string.nav_categories, Icons.Default.Category, "nav_categories"),
    SEARCH(Search, R.string.nav_search, Icons.Default.Search, "nav_search"),
    ACCOUNT(Account, R.string.nav_account, Icons.Default.Person, "nav_account"),
}

@Composable
fun SaleorApp() {
    val backStack = rememberNavBackStack(Home)
    val current = backStack.lastOrNull()
    val showBottomBar = current is Home || current is Categories || current is Search || current is Account

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            if (showBottomBar) {
                TopLevelDestination.entries.forEach { destination ->
                    item(
                        icon = {
                            Icon(
                                destination.icon,
                                contentDescription = stringResource(destination.labelRes),
                                modifier = Modifier.testTag(destination.testTag),
                            )
                        },
                        label = { Text(stringResource(destination.labelRes)) },
                        selected = current == destination.key ||
                            (destination.key is Home && current is Home),
                        onClick = {
                            if (current != destination.key) {
                                backStack.clear()
                                backStack.add(destination.key)
                            }
                        },
                    )
                }
            }
        },
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                entry<Home> {
                    HomeScreen(
                        onProductClick = { product -> backStack.add(ProductDetail(product.slug)) },
                        onCategoryClick = { category ->
                            backStack.add(
                                ProductList(
                                    source = ProductListSource.CATEGORY,
                                    slug = category.slug,
                                    title = category.name,
                                ),
                            )
                        },
                        onViewAllFeatured = {
                            backStack.add(
                                ProductList(
                                    source = ProductListSource.COLLECTION,
                                    slug = BuildConfig.FEATURED_COLLECTION_SLUG,
                                    title = "Featured",
                                ),
                            )
                        },
                    )
                }
                entry<Categories> {
                    CategoryListScreen(
                        onCategoryClick = { category ->
                            backStack.add(
                                ProductList(
                                    source = ProductListSource.CATEGORY,
                                    slug = category.slug,
                                    title = category.name,
                                ),
                            )
                        },
                    )
                }
                entry<Search> {
                    SearchScreen(
                        onProductClick = { product -> backStack.add(ProductDetail(product.slug)) },
                    )
                }
                entry<Account> {
                    AccountRoute(
                        onRegisterClick = { backStack.add(Register) },
                        onForgotPasswordClick = { backStack.add(ForgotPassword) },
                        onOrdersClick = { backStack.add(Orders) },
                    )
                }
                entry<ProductList> { key ->
                    val args = remember(key) {
                        ProductListArgs(source = key.source, slug = key.slug, title = key.title)
                    }
                    val viewModel = hiltViewModel<ProductListViewModel, ProductListViewModel.Factory>(
                        creationCallback = { factory -> factory.create(args) },
                    )
                    ProductListScreen(
                        viewModel = viewModel,
                        onProductClick = { product -> backStack.add(ProductDetail(product.slug)) },
                        onBack = { backStack.removeLastOrNull() },
                    )
                }
                entry<ProductDetail> { key ->
                    val viewModel = hiltViewModel<ProductDetailViewModel, ProductDetailViewModel.Factory>(
                        creationCallback = { factory -> factory.create(key.slug) },
                    )
                    ProductDetailScreen(
                        viewModel = viewModel,
                        onBack = { backStack.removeLastOrNull() },
                    )
                }
                entry<Register> {
                    RegisterScreen(onBack = { backStack.removeLastOrNull() })
                }
                entry<ForgotPassword> {
                    ForgotPasswordScreen(onBack = { backStack.removeLastOrNull() })
                }
                entry<Orders> {
                    OrderListScreen(
                        onOrderClick = { order -> backStack.add(OrderDetail(order.id)) },
                        onBack = { backStack.removeLastOrNull() },
                    )
                }
                entry<OrderDetail> { key ->
                    val viewModel = hiltViewModel<OrderDetailViewModel, OrderDetailViewModel.Factory>(
                        creationCallback = { factory -> factory.create(key.id) },
                    )
                    OrderDetailScreen(
                        viewModel = viewModel,
                        onBack = { backStack.removeLastOrNull() },
                    )
                }
            },
        )
    }
}
