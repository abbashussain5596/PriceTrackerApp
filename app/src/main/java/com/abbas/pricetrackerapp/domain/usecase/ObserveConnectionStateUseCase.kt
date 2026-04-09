package com.abbas.pricetrackerapp.domain.usecase

import com.abbas.pricetrackerapp.domain.repository.PriceRepository
import kotlinx.coroutines.flow.Flow

class ObserveConnectionStateUseCase(
    private val repository: PriceRepository
) {
    operator fun invoke(): Flow<Boolean> = repository.observeConnectionState()
}
