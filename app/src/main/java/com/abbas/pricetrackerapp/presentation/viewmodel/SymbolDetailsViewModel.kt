package com.abbas.pricetrackerapp.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abbas.pricetrackerapp.domain.usecase.ObserveConnectionStateUseCase
import com.abbas.pricetrackerapp.domain.usecase.ObservePriceUpdatesUseCase
import com.abbas.pricetrackerapp.domain.usecase.StartPriceFeedUseCase
import com.abbas.pricetrackerapp.presentation.state.ConnectionState
import com.abbas.pricetrackerapp.presentation.state.SymbolDetailsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SymbolDetailsViewModel(
    savedStateHandle: SavedStateHandle,
    private val observePriceUpdatesUseCase: ObservePriceUpdatesUseCase,
    private val observeConnectionStateUseCase: ObserveConnectionStateUseCase,
    private val startPriceFeedUseCase: StartPriceFeedUseCase
) : ViewModel() {

    val symbol: String = savedStateHandle.get<String>("symbol")
        ?: throw IllegalArgumentException("Symbol parameter is required")

    private val _uiState = MutableStateFlow<SymbolDetailsUiState>(
        SymbolDetailsUiState.Success(
            symbol = symbol,
            stockPrice = null,
            connectionState = ConnectionState.DISCONNECTED
        )
    )
    val uiState: StateFlow<SymbolDetailsUiState> = _uiState.asStateFlow()

    init {
        // Ensure the price feed is running when navigating directly to this screen
        // (e.g. via a deep link). startPriceFeed() is idempotent – it no-ops if
        // the feed is already active.
        viewModelScope.launch { startPriceFeedUseCase() }
        observePriceUpdates()
        observeConnectionState()
    }

    private fun observePriceUpdates() {
        viewModelScope.launch {
            observePriceUpdatesUseCase()
                .filter { it.symbol == symbol }
                .catch { e ->
                    _uiState.value = SymbolDetailsUiState.Error(e.message ?: "Unknown error")
                }
                .collect { stockPrice ->
                    _uiState.update { current ->
                        val connectionState = (current as? SymbolDetailsUiState.Success)
                            ?.connectionState ?: ConnectionState.DISCONNECTED
                        SymbolDetailsUiState.Success(
                            symbol = symbol,
                            stockPrice = stockPrice,
                            connectionState = connectionState
                        )
                    }
                }
        }
    }

    private fun observeConnectionState() {
        viewModelScope.launch {
            observeConnectionStateUseCase().collect { isConnected ->
                val connectionState = if (isConnected) ConnectionState.CONNECTED
                else ConnectionState.DISCONNECTED

                _uiState.update { current ->
                    when (current) {
                        is SymbolDetailsUiState.Success -> current.copy(connectionState = connectionState)
                        else -> current
                    }
                }
            }
        }
    }

    fun getPriceChangeIndicator(): Boolean? =
        (uiState.value as? SymbolDetailsUiState.Success)?.stockPrice?.isPriceIncreased
}
