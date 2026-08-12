package com.bdf.saleor.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bdf.saleor.BuildConfig
import com.bdf.saleor.R
import com.bdf.saleor.ui.catalog.ProductListScreen
import com.bdf.saleor.ui.category.CategoryListScreen
import com.bdf.saleor.ui.detail.ProductDetailScreen
import com.bdf.saleor.ui.home.HomeScreen
import com.bdf.saleor.ui.search.SearchScreen
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private enum class TopLevelDestination(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
) {
    HOME(Routes.HOME, R.string.nav_home, Icons.Default.Home),
    CATEGORIES(Routes.CATEGORIES, R.string.nav_categories, Icons.Default.Category),
    SEARCH(Routes.SEARCH, R.string.nav_search, Icons.Default.Search),
}

@Composable
fun SaleorApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in TopLevelDestination.entries.map { it.route }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            if (showBottomBar) {
                TopLevelDestination.entries.forEach { destination ->
                    item(
                        icon = {
                            Icon(destination.icon, contentDescription = stringResource(destination.labelRes))
                        },
                        label = { Text(stringResource(destination.labelRes)) },
                        selected = currentRoute == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            }
        },
    ) {
        SaleorNavHost(navController = navController)
    }
}

@Composable
private fun SaleorNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier,
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onProductClick = { product ->
                    navController.navigate(Routes.productDetail(product.slug))
                },
                onCategoryClick = { category ->
                    navController.navigate(
                        Routes.productList(
                            source = Routes.Source.CATEGORY,
                            slug = category.slug,
                            title = encode(category.name),
                        ),
                    )
                },
                onViewAllFeatured = {
                    navController.navigate(
                        Routes.productList(
                            source = Routes.Source.COLLECTION,
                            slug = BuildConfig.FEATURED_COLLECTION_SLUG,
                            title = encode("Featured"),
                        ),
                    )
                },
            )
        }
        composable(Routes.CATEGORIES) {
            CategoryListScreen(
                onCategoryClick = { category ->
                    navController.navigate(
                        Routes.productList(
                            source = Routes.Source.CATEGORY,
                            slug = category.slug,
                            title = encode(category.name),
                        ),
                    )
                },
            )
        }
        composable(Routes.SEARCH) {
            SearchScreen(
                onProductClick = { product ->
                    navController.navigate(Routes.productDetail(product.slug))
                },
            )
        }
        composable(
            route = Routes.PRODUCT_LIST,
            arguments = listOf(
                navArgument("source") { type = NavType.StringType; defaultValue = Routes.Source.ALL },
                navArgument("slug") { type = NavType.StringType; defaultValue = "" },
                navArgument("title") { type = NavType.StringType; defaultValue = "" },
            ),
        ) {
            ProductListScreen(
                onProductClick = { product ->
                    navController.navigate(Routes.productDetail(product.slug))
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.PRODUCT_DETAIL,
            arguments = listOf(navArgument("slug") { type = NavType.StringType }),
        ) {
            ProductDetailScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}

private fun encode(value: String): String =
    URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
