package com.bdf.saleor.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.bdf.saleor.R
import com.bdf.saleor.core.designsystem.R as DesignR
import com.bdf.saleor.core.network.BuildConfig
import com.bdf.saleor.data.model.AuthState
import com.bdf.saleor.ui.account.AccountRoute
import com.bdf.saleor.ui.account.ForgotPasswordScreen
import com.bdf.saleor.ui.account.OrderDetailScreen
import com.bdf.saleor.ui.account.OrderDetailViewModel
import com.bdf.saleor.ui.account.RegisterScreen
import com.bdf.saleor.ui.catalog.ProductListArgs
import com.bdf.saleor.ui.catalog.ProductListScreen
import com.bdf.saleor.ui.catalog.ProductListSource
import com.bdf.saleor.ui.catalog.ProductListViewModel
import com.bdf.saleor.ui.category.CategoryListScreen
import com.bdf.saleor.ui.components.StorefrontTopBar
import com.bdf.saleor.ui.detail.ProductDetailScreen
import com.bdf.saleor.ui.detail.ProductDetailViewModel
import com.bdf.saleor.ui.home.HomeScreen
import com.bdf.saleor.ui.search.SearchScreen
import kotlinx.coroutines.launch

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaleorApp(
    chromeViewModel: StorefrontChromeViewModel = hiltViewModel(),
) {
    val backStack = rememberNavBackStack(Home)
    val current = backStack.lastOrNull()
    val authState by chromeViewModel.authState.collectAsStateWithLifecycle()
    val currentUser by chromeViewModel.currentUser.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var menuOpen by remember { mutableStateOf(false) }
    val cartMessage = stringResource(DesignR.string.cart_coming_soon)
    var selectedTopLevel by remember { mutableStateOf(TopLevelDestination.HOME) }

    LaunchedEffect(current) {
        val next = when (current) {
            is Home -> TopLevelDestination.HOME
            is Categories -> TopLevelDestination.CATEGORIES
            is Search -> TopLevelDestination.SEARCH
            is Account, is Register, is ForgotPassword, is OrderDetail -> TopLevelDestination.ACCOUNT
            else -> null
        }
        if (next != null) selectedTopLevel = next
    }

    fun goTo(destination: NavKey) {
        if (current != destination) {
            backStack.clear()
            backStack.add(destination)
        }
        menuOpen = false
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
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
                    selected = destination == selectedTopLevel,
                    onClick = {
                        selectedTopLevel = destination
                        goTo(destination.key)
                    },
                )
            }
        },
    ) {
        Column {
            StorefrontTopBar(
                storeName = stringResource(R.string.app_name),
                initials = if (authState is AuthState.LoggedIn) currentUser?.initials else null,
                onAccountClick = { goTo(Account) },
                onCartClick = {
                    scope.launch { snackbarHostState.showSnackbar(cartMessage) }
                },
                onMenuClick = { menuOpen = true },
            )
            Box(modifier = Modifier.weight(1f)) {
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
                                onBackToStore = { goTo(Home) },
                                onOrderClick = { order -> backStack.add(OrderDetail(order.id)) },
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
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }

    if (menuOpen) {
        ModalBottomSheet(
            onDismissRequest = { menuOpen = false },
            sheetState = rememberModalBottomSheetState(),
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                TextButton(onClick = { goTo(Home) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.nav_home))
                }
                TextButton(onClick = { goTo(Categories) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.nav_categories))
                }
                TextButton(onClick = { goTo(Search) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.nav_search))
                }
                TextButton(onClick = { goTo(Account) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.nav_account))
                }
                if (authState is AuthState.LoggedIn) {
                    TextButton(
                        onClick = {
                            chromeViewModel.logout()
                            menuOpen = false
                            goTo(Home)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.account_logout))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
