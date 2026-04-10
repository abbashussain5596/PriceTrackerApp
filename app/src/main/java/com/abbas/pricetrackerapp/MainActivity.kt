package com.abbas.pricetrackerapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.abbas.pricetrackerapp.presentation.navigation.AppNavHost
import com.abbas.pricetrackerapp.ui.theme.PriceTrackerAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PriceTrackerAppTheme {
                AppNavHost()
            }
        }
    }
}
