package com.abbas.pricetrackerapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.NavHostController
import com.abbas.pricetrackerapp.presentation.navigation.AppNavHost
import com.abbas.pricetrackerapp.ui.theme.PriceTrackerAppTheme

class MainActivity : ComponentActivity() {

    // Hoisted so onNewIntent can forward the URI to the nav graph.
    private lateinit var navController: NavHostController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PriceTrackerAppTheme {
                AppNavHost(
                    onNavControllerReady = { navController = it }
                )
            }
        }
    }

    /**
     * Called when a deep link arrives while the activity is already running
     * (launchMode="singleTop" prevents a second instance from being created).
     * Forward the new intent to the nav controller so it navigates to the
     * matching destination immediately.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (::navController.isInitialized) {
            navController.handleDeepLink(intent)
        }
    }
}
