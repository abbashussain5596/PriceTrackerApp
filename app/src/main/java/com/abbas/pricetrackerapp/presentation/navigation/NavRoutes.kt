package com.abbas.pricetrackerapp.presentation.navigation

object NavRoutes {
    const val FEED = "feed"

    fun symbolDetails(symbol: String): String = "symbol_details/$symbol"
}
