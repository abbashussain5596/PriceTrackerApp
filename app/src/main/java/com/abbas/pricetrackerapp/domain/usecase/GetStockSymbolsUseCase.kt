package com.abbas.pricetrackerapp.domain.usecase

import com.abbas.pricetrackerapp.domain.repository.PriceRepository

class GetStockSymbolsUseCase(
    private val repository: PriceRepository
) {
    suspend operator fun invoke(): List<String> = repository.getStockSymbols()
}
