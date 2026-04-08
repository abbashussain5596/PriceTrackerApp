package com.abbas.pricetrackerapp.data.model

data class PriceUpdate(
    val symbol: String,
    val price: Double,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun calculateChangePercentage(previousPrice: Double): Double {
        if (previousPrice == 0.0) return 0.0
        return ((price - previousPrice) / previousPrice) * 100
    }

    fun isPriceIncreased(previousPrice: Double): Boolean {
        return price > previousPrice
    }
}
