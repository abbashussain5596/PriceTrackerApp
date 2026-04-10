package com.abbas.pricetrackerapp.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.abbas.pricetrackerapp.presentation.ui.details.SymbolDetailsScreen
import com.abbas.pricetrackerapp.presentation.ui.feed.PriceFeedScreen

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.FEED
    ) {
        composable(route = NavRoutes.FEED) {
            PriceFeedScreen(
                onSymbolClick = { symbol ->
                    navController.navigate(NavRoutes.symbolDetails(symbol))
                }
            )
        }

        composable(
            route = "symbol_details/{symbol}",
            arguments = listOf(
                navArgument("symbol") { type = NavType.StringType }
            ),
            deepLinks = listOf(
                navDeepLink { uriPattern = "stocks://symbol/{symbol}" }
            )
        ) {
            // Koin reads "symbol" from the back-stack SavedStateHandle automatically
            SymbolDetailsScreen(
                onBackClick = {
                    val popped = navController.popBackStack()
                    if (!popped) {
                        navController.navigate(NavRoutes.FEED) { launchSingleTop = true }
                    }
                }
            )
        }
    }
}
