package com.abbas.pricetrackerapp.domain.model

data class StockPrice(
    val symbol: String,
    val price: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val previousPrice: Double? = null
) {
    val changePercentage: Double
        get() = previousPrice?.let {
            if (it == 0.0) 0.0
            else ((price - it) / it) * 100
        } ?: 0.0

    val isPriceIncreased: Boolean
        get() = previousPrice?.let { price > it } ?: false

    val isPriceDecreased: Boolean
        get() = previousPrice?.let { price < it } ?: false
}
