package com.abbas.pricetrackerapp.domain.usecase

import com.abbas.pricetrackerapp.domain.repository.PriceRepository

class StartPriceFeedUseCase(
    private val repository: PriceRepository
) {
    suspend operator fun invoke() = repository.startPriceFeed()
}
