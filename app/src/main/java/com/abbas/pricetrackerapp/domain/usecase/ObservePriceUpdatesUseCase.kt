package com.abbas.pricetrackerapp.domain.usecase

import com.abbas.pricetrackerapp.domain.model.StockPrice
import com.abbas.pricetrackerapp.domain.repository.PriceRepository
import kotlinx.coroutines.flow.Flow

class ObservePriceUpdatesUseCase(
    private val repository: PriceRepository
) {
    operator fun invoke(): Flow<StockPrice> = repository.observePriceUpdates()
}
