package com.bdf.saleor.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import com.bdf.saleor.ui.components.AppBottomNavItem
import com.bdf.saleor.ui.components.AppBottomNavigation
import com.bdf.saleor.ui.components.LocalSnackbarHostState
import com.bdf.saleor.ui.components.LocalTabReselectTick
import com.bdf.saleor.ui.components.ScreenSurface
import com.bdf.saleor.ui.components.StorefrontTopBar
import com.bdf.saleor.ui.components.rememberBottomNavScrollConnection
import com.bdf.saleor.ui.detail.ProductDetailScreen
import com.bdf.saleor.ui.detail.ProductDetailViewModel
import com.bdf.saleor.ui.cart.CartRoute
import com.bdf.saleor.ui.checkout.CheckoutCompleteScreen
import com.bdf.saleor.ui.checkout.CheckoutRoute
import com.bdf.saleor.ui.home.HomeScreen
import com.bdf.saleor.ui.search.SearchScreen
import kotlinx.coroutines.launch

private enum class TopLevelDestination(
    val key: NavKey,
    val labelRes: Int,
    val iconOutlined: ImageVector,
    val iconFilled: ImageVector,
    val testTag: String,
) {
    HOME(Home, R.string.nav_home, Icons.Outlined.Home, Icons.Filled.Home, "nav_home"),
    CATEGORIES(Categories, R.string.nav_categories, Icons.Outlined.GridView, Icons.Filled.GridView, "nav_categories"),
    SEARCH(Search, R.string.nav_search, Icons.Outlined.Search, Icons.Filled.Search, "nav_search"),
    ACCOUNT(Account, R.string.nav_account, Icons.Outlined.Person, Icons.Filled.Person, "nav_account"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaleorApp(
    chromeViewModel: StorefrontChromeViewModel = hiltViewModel(),
) {
    val backStack = rememberNavBackStack(Home)
    val current = backStack.lastOrNull()
    val authState by chromeViewModel.authState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    val cartQuantity by chromeViewModel.cartQuantity.collectAsStateWithLifecycle()
    var selectedTopLevel by remember { mutableStateOf(TopLevelDestination.HOME) }
    var tabReselectTick by remember { mutableIntStateOf(0) }
    var bottomNavVisible by remember { mutableStateOf(true) }
    val hideBottomNavForRoute = when (current) {
        is Home, is Categories, is Search, is Account,
        is Register, is ForgotPassword, is OrderDetail,
        is ProductDetail,
        -> false
        else -> true
    }
    val topLevelScrollEnabled = current is Home ||
        current is Categories ||
        current is Search ||
        current is Account

    LaunchedEffect(current) {
        val next = when (current) {
            is Home -> TopLevelDestination.HOME
            is Categories -> TopLevelDestination.CATEGORIES
            is Search -> TopLevelDestination.SEARCH
            is Account, is Register, is ForgotPassword, is OrderDetail -> TopLevelDestination.ACCOUNT
            else -> null
        }
        if (next != null) selectedTopLevel = next
        bottomNavVisible = true
    }

    fun goTo(destination: NavKey) {
        bottomNavVisible = true
        if (current != destination) {
            backStack.clear()
            backStack.add(destination)
        }
    }

    val openCart = {
        if (current !is Cart) backStack.add(Cart)
    }

    val bottomNavConnection = rememberBottomNavScrollConnection(
        onHide = { bottomNavVisible = false },
        onShow = { bottomNavVisible = true },
        enabled = topLevelScrollEnabled && !hideBottomNavForRoute,
    )

    CompositionLocalProvider(
        LocalSnackbarHostState provides snackbarHostState,
        LocalTabReselectTick provides tabReselectTick,
    ) {
        // Do not apply a height constraint on this Scaffold; it collapses the content slot.
        // Bottom nav lives in a Column (not Scaffold.bottomBar) so it cannot inherit a
        // minHeight equal to the remaining window and swallow the content slot.
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = if (hideBottomNavForRoute) {
                WindowInsets.navigationBars.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                )
            } else {
                WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal)
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    NavDisplay(
                        backStack = backStack,
                        onBack = {
                            if (current is CheckoutComplete) {
                                goTo(Home)
                            } else {
                                backStack.removeLastOrNull()
                            }
                        },
                        transitionSpec = {
                            EnterTransition.None togetherWith ExitTransition.None
                        },
                        popTransitionSpec = {
                            EnterTransition.None togetherWith ExitTransition.None
                        },
                        predictivePopTransitionSpec = {
                            EnterTransition.None togetherWith ExitTransition.None
                        },
                        entryDecorators = listOf(
                            rememberSaveableStateHolderNavEntryDecorator(),
                            rememberViewModelStoreNavEntryDecorator(),
                        ),
                        entryProvider = entryProvider {
                            entry<Home> {
                                StorefrontTopLevel(
                                    storeName = stringResource(R.string.app_name),
                                    onStoreNameClick = { goTo(Home) },
                                    onCartClick = openCart,
                                    cartQuantity = cartQuantity,
                                    nestedScrollConnection = bottomNavConnection,
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
                                    onStoreNameClick = { goTo(Home) },
                                    onCartClick = openCart,
                                    cartQuantity = cartQuantity,
                                    nestedScrollConnection = bottomNavConnection,
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
                                    onStoreNameClick = { goTo(Home) },
                                    onCartClick = openCart,
                                    cartQuantity = cartQuantity,
                                    nestedScrollConnection = bottomNavConnection,
                                ) {
                                    SearchScreen(
                                        onProductClick = { product -> backStack.add(ProductDetail(product.slug)) },
                                    )
                                }
                            }
                            entry<Account> {
                                StorefrontTopLevel(
                                    storeName = stringResource(R.string.app_name),
                                    onStoreNameClick = { goTo(Home) },
                                    onCartClick = openCart,
                                    cartQuantity = cartQuantity,
                                    nestedScrollConnection = bottomNavConnection,
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
                                        onCartClick = openCart,
                                        onBuyNow = {
                                            if (current !is Cart) backStack.add(Cart)
                                        },
                                        cartQuantity = cartQuantity,
                                    )
                                }
                            }
                            entry<Register> {
                                ScreenSurface {
                                    RegisterScreen(
                                        onBack = { backStack.removeLastOrNull() },
                                        onRegistered = { message ->
                                            backStack.removeLastOrNull()
                                            if (message.isNotBlank()) {
                                                snackbarScope.launch {
                                                    snackbarHostState.showSnackbar(message)
                                                }
                                            }
                                        },
                                    )
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
                                        loggedIn = authState is AuthState.LoggedIn,
                                        onCheckout = { backStack.add(Checkout) },
                                        onLoginRequired = { backStack.add(Account) },
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
                                BackHandler { goTo(Home) }
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
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
                if (!hideBottomNavForRoute) {
                    AppBottomNavigation(
                        items = TopLevelDestination.entries.map { destination ->
                            AppBottomNavItem(
                                label = stringResource(destination.labelRes),
                                iconOutlined = destination.iconOutlined,
                                iconFilled = destination.iconFilled,
                                selected = destination == selectedTopLevel,
                                testTag = destination.testTag,
                                onClick = {
                                    if (destination == selectedTopLevel && current == destination.key) {
                                        bottomNavVisible = true
                                        tabReselectTick++
                                    } else {
                                        selectedTopLevel = destination
                                        goTo(destination.key)
                                    }
                                },
                            )
                        },
                        scrollVisible = bottomNavVisible,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StorefrontTopLevel(
    storeName: String,
    onStoreNameClick: () -> Unit,
    onCartClick: () -> Unit,
    cartQuantity: Int,
    nestedScrollConnection: NestedScrollConnection,
    content: @Composable () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    ScreenSurface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .nestedScroll(nestedScrollConnection),
        ) {
            StorefrontTopBar(
                storeName = storeName,
                onStoreNameClick = onStoreNameClick,
                onCartClick = onCartClick,
                cartQuantity = cartQuantity,
                scrollBehavior = scrollBehavior,
            )
            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                content()
            }
        }
    }
}
