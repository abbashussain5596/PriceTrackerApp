package com.abbas.pricetrackerapp.presentation.state

import com.abbas.pricetrackerapp.domain.model.StockPrice

sealed interface SymbolDetailsUiState {
    data object Loading : SymbolDetailsUiState

    data class Success(
        val symbol: String,
        val stockPrice: StockPrice?,
        val connectionState: ConnectionState
    ) : SymbolDetailsUiState

    data class Error(
        val message: String
    ) : SymbolDetailsUiState
}

fun SymbolDetailsUiState.getStockPrice(): StockPrice? =
    (this as? SymbolDetailsUiState.Success)?.stockPrice
