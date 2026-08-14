package com.bdf.saleor.ui.navigation

import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.bdf.saleor.R
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
import com.bdf.saleor.ui.components.LocalSnackbarHostState
import com.bdf.saleor.ui.components.ScreenSurface
import com.bdf.saleor.ui.components.StorefrontTopBar
import com.bdf.saleor.ui.detail.ProductDetailScreen
import com.bdf.saleor.ui.detail.ProductDetailViewModel
import com.bdf.saleor.ui.cart.CartRoute
import com.bdf.saleor.ui.checkout.CheckoutCompleteScreen
import com.bdf.saleor.ui.checkout.CheckoutRoute
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
fun SaleorApp(
    chromeViewModel: StorefrontChromeViewModel = hiltViewModel(),
) {
    val backStack = rememberNavBackStack(Home)
    val current = backStack.lastOrNull()
    val authState by chromeViewModel.authState.collectAsStateWithLifecycle()
    val currentUser by chromeViewModel.currentUser.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val cartQuantity by chromeViewModel.cartQuantity.collectAsStateWithLifecycle()
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
    }

    val openCart = {
        if (current !is Cart) backStack.add(Cart)
    }

    CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
        val navColors = MaterialTheme.colorScheme
        val navigationSuiteColors = NavigationSuiteDefaults.colors(
            navigationBarContainerColor = navColors.surfaceContainer,
        )
        val navigationItemColors = NavigationSuiteDefaults.itemColors(
            navigationBarItemColors = NavigationBarItemDefaults.colors(
                indicatorColor = navColors.secondaryContainer,
                selectedIconColor = navColors.onSecondaryContainer,
                selectedTextColor = navColors.onSecondaryContainer,
                unselectedIconColor = navColors.onSurfaceVariant,
                unselectedTextColor = navColors.onSurfaceVariant,
            ),
        )
        NavigationSuiteScaffold(
            navigationSuiteColors = navigationSuiteColors,
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
                        colors = navigationItemColors,
                        onClick = {
                            selectedTopLevel = destination
                            goTo(destination.key)
                        },
                    )
                }
            },
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    NavDisplay(
                        backStack = backStack,
                        onBack = { backStack.removeLastOrNull() },
                        transitionSpec = {
                            slideInHorizontally(
                                animationSpec = tween(280),
                                initialOffsetX = { it },
                            ) togetherWith ExitTransition.KeepUntilTransitionsFinished
                        },
                        popTransitionSpec = {
                            slideInHorizontally(
                                animationSpec = tween(280),
                                initialOffsetX = { -it / 4 },
                            ) togetherWith slideOutHorizontally(
                                animationSpec = tween(280),
                                targetOffsetX = { it },
                            )
                        },
                        predictivePopTransitionSpec = {
                            slideInHorizontally(
                                animationSpec = tween(280),
                                initialOffsetX = { -it / 4 },
                            ) togetherWith slideOutHorizontally(
                                animationSpec = tween(280),
                                targetOffsetX = { it },
                            )
                        },
                        entryDecorators = listOf(
                            rememberSaveableStateHolderNavEntryDecorator(),
                            rememberViewModelStoreNavEntryDecorator(),
                        ),
                        entryProvider = entryProvider {
                            entry<Home> {
                                StorefrontTopLevel(
                                    storeName = stringResource(R.string.app_name),
                                    initials = if (authState is AuthState.LoggedIn) currentUser?.initials else null,
                                    onStoreNameClick = { goTo(Home) },
                                    onAccountClick = { goTo(Account) },
                                    onCartClick = openCart,
                                    cartQuantity = cartQuantity,
                                ) {
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
                            }
                            entry<Categories> {
                                StorefrontTopLevel(
                                    storeName = stringResource(R.string.app_name),
                                    initials = if (authState is AuthState.LoggedIn) currentUser?.initials else null,
                                    onStoreNameClick = { goTo(Home) },
                                    onAccountClick = { goTo(Account) },
                                    onCartClick = openCart,
                                    cartQuantity = cartQuantity,
                                ) {
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
                            }
                            entry<Search> {
                                StorefrontTopLevel(
                                    storeName = stringResource(R.string.app_name),
                                    initials = if (authState is AuthState.LoggedIn) currentUser?.initials else null,
                                    onStoreNameClick = { goTo(Home) },
                                    onAccountClick = { goTo(Account) },
                                    onCartClick = openCart,
                                    cartQuantity = cartQuantity,
                                ) {
                                    SearchScreen(
                                        onProductClick = { product -> backStack.add(ProductDetail(product.slug)) },
                                    )
                                }
                            }
                            entry<Account> {
                                StorefrontTopLevel(
                                    storeName = stringResource(R.string.app_name),
                                    initials = if (authState is AuthState.LoggedIn) currentUser?.initials else null,
                                    onStoreNameClick = { goTo(Home) },
                                    onAccountClick = { goTo(Account) },
                                    onCartClick = openCart,
                                    cartQuantity = cartQuantity,
                                ) {
                                    AccountRoute(
                                        onRegisterClick = { backStack.add(Register) },
                                        onForgotPasswordClick = { backStack.add(ForgotPassword) },
                                        onOrderClick = { order -> backStack.add(OrderDetail(order.id)) },
                                    )
                                }
                            }
                            entry<ProductList> { key ->
                                val args = remember(key) {
                                    ProductListArgs(source = key.source, slug = key.slug, title = key.title)
                                }
                                val viewModel = hiltViewModel<ProductListViewModel, ProductListViewModel.Factory>(
                                    creationCallback = { factory -> factory.create(args) },
                                )
                                ScreenSurface {
                                    ProductListScreen(
                                        viewModel = viewModel,
                                        onProductClick = { product -> backStack.add(ProductDetail(product.slug)) },
                                        onBack = { backStack.removeLastOrNull() },
                                    )
                                }
                            }
                            entry<ProductDetail> { key ->
                                val viewModel = hiltViewModel<ProductDetailViewModel, ProductDetailViewModel.Factory>(
                                    creationCallback = { factory -> factory.create(key.slug) },
                                )
                                ScreenSurface {
                                    ProductDetailScreen(
                                        viewModel = viewModel,
                                        onBack = { backStack.removeLastOrNull() },
                                    )
                                }
                            }
                            entry<Register> {
                                ScreenSurface {
                                    RegisterScreen(onBack = { backStack.removeLastOrNull() })
                                }
                            }
                            entry<ForgotPassword> {
                                ScreenSurface {
                                    ForgotPasswordScreen(onBack = { backStack.removeLastOrNull() })
                                }
                            }
                            entry<OrderDetail> { key ->
                                val viewModel = hiltViewModel<OrderDetailViewModel, OrderDetailViewModel.Factory>(
                                    creationCallback = { factory -> factory.create(key.id) },
                                )
                                ScreenSurface {
                                    OrderDetailScreen(
                                        viewModel = viewModel,
                                        onBack = { backStack.removeLastOrNull() },
                                    )
                                }
                            }
                            entry<Cart> {
                                ScreenSurface {
                                    CartRoute(
                                        onBack = { backStack.removeLastOrNull() },
                                        onCheckout = { backStack.add(Checkout) },
                                    )
                                }
                            }
                            entry<Checkout> {
                                ScreenSurface {
                                    CheckoutRoute(
                                        onBack = { backStack.removeLastOrNull() },
                                        onCompleted = { orderId, orderNumber ->
                                            backStack.removeLastOrNull()
                                            backStack.add(CheckoutComplete(orderId, orderNumber))
                                        },
                                    )
                                }
                            }
                            entry<CheckoutComplete> { key ->
                                ScreenSurface {
                                    CheckoutCompleteScreen(
                                        orderId = key.orderId,
                                        orderNumber = key.orderNumber,
                                        onHome = { goTo(Home) },
                                        onViewOrder = { orderId ->
                                            backStack.removeLastOrNull()
                                            backStack.add(OrderDetail(orderId))
                                        },
                                    )
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun StorefrontTopLevel(
    storeName: String,
    initials: String?,
    onStoreNameClick: () -> Unit,
    onAccountClick: () -> Unit,
    onCartClick: () -> Unit,
    cartQuantity: Int,
    content: @Composable () -> Unit,
) {
    ScreenSurface {
        Column(modifier = Modifier.fillMaxSize()) {
            StorefrontTopBar(
                storeName = storeName,
                initials = initials,
                onStoreNameClick = onStoreNameClick,
                onAccountClick = onAccountClick,
                onCartClick = onCartClick,
                cartQuantity = cartQuantity,
            )
            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                content()
            }
        }
    }
}
