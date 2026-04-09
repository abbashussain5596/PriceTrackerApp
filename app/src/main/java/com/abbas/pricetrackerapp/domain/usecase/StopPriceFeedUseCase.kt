package com.abbas.pricetrackerapp.domain.usecase

import com.abbas.pricetrackerapp.domain.repository.PriceRepository

class StopPriceFeedUseCase(
    private val repository: PriceRepository
) {
    suspend operator fun invoke() = repository.stopPriceFeed()
}
