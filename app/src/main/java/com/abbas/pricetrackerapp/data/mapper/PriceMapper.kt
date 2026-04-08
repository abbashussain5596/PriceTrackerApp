package com.abbas.pricetrackerapp.data.mapper

import com.abbas.pricetrackerapp.data.model.PriceUpdate
import com.abbas.pricetrackerapp.domain.model.StockPrice

object PriceMapper {
    fun toDomain(
        priceUpdate: PriceUpdate,
        previousPrice: Double? = null
    ): StockPrice {
        return StockPrice(
            symbol = priceUpdate.symbol,
            price = priceUpdate.price,
            timestamp = priceUpdate.timestamp,
            previousPrice = previousPrice
        )
    }
}
