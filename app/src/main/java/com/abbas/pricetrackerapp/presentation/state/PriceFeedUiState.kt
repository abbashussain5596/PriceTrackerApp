package com.abbas.pricetrackerapp.presentation.state

import com.abbas.pricetrackerapp.domain.model.StockPrice

sealed interface PriceFeedUiState {
    data object Loading : PriceFeedUiState

    data class Success(
        val stockPrices: List<StockPrice>,
        val connectionState: ConnectionState,
        val isFeedRunning: Boolean
    ) : PriceFeedUiState

    data class Error(
        val message: String
    ) : PriceFeedUiState
}

fun PriceFeedUiState.getStockPrices(): List<StockPrice> =
    (this as? PriceFeedUiState.Success)?.stockPrices ?: emptyList()
