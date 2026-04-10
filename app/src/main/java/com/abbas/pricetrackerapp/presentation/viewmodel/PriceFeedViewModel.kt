package com.abbas.pricetrackerapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abbas.pricetrackerapp.domain.model.StockPrice
import com.abbas.pricetrackerapp.domain.usecase.GetStockSymbolsUseCase
import com.abbas.pricetrackerapp.domain.usecase.ObserveConnectionStateUseCase
import com.abbas.pricetrackerapp.domain.usecase.ObserveFeedRunningStateUseCase
import com.abbas.pricetrackerapp.domain.usecase.ObservePriceUpdatesUseCase
import com.abbas.pricetrackerapp.domain.usecase.StartPriceFeedUseCase
import com.abbas.pricetrackerapp.domain.usecase.StopPriceFeedUseCase
import com.abbas.pricetrackerapp.presentation.state.ConnectionState
import com.abbas.pricetrackerapp.presentation.state.PriceFeedUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PriceFeedViewModel(
    private val getStockSymbolsUseCase: GetStockSymbolsUseCase,
    private val observePriceUpdatesUseCase: ObservePriceUpdatesUseCase,
    private val observeConnectionStateUseCase: ObserveConnectionStateUseCase,
    private val observeFeedRunningStateUseCase: ObserveFeedRunningStateUseCase,
    private val startPriceFeedUseCase: StartPriceFeedUseCase,
    private val stopPriceFeedUseCase: StopPriceFeedUseCase
) : ViewModel() {

    private val stockPricesMap = mutableMapOf<String, StockPrice>()

    private val _uiState = MutableStateFlow<PriceFeedUiState>(
        PriceFeedUiState.Success(
            stockPrices = emptyList(),
            connectionState = ConnectionState.DISCONNECTED,
            isFeedRunning = false
        )
    )
    val uiState: StateFlow<PriceFeedUiState> = _uiState.asStateFlow()

    init {
        observeAllStates()
    }

    private fun observeAllStates() {
        viewModelScope.launch {
            combine(
                observeFeedRunningStateUseCase(),
                observeConnectionStateUseCase()
            ) { isFeedRunning, isConnected ->
                val connectionState = when {
                    isConnected -> ConnectionState.CONNECTED
                    isFeedRunning -> ConnectionState.CONNECTING
                    else -> ConnectionState.DISCONNECTED
                }
                Pair(isFeedRunning, connectionState)
            }.collect { (isFeedRunning, connectionState) ->
                _uiState.update { current ->
                    val success = current as? PriceFeedUiState.Success
                    PriceFeedUiState.Success(
                        stockPrices = success?.stockPrices ?: emptyList(),
                        connectionState = connectionState,
                        isFeedRunning = isFeedRunning
                    )
                }
            }
        }

        viewModelScope.launch {
            observePriceUpdatesUseCase()
                .catch { e ->
                    _uiState.value = PriceFeedUiState.Error(e.message ?: "Unknown error")
                }
                .collect { stockPrice ->
                    stockPricesMap[stockPrice.symbol] = stockPrice
                    val sortedPrices = stockPricesMap.values.sortedByDescending { it.price }
                    _uiState.update { current ->
                        val success = current as? PriceFeedUiState.Success
                        PriceFeedUiState.Success(
                            stockPrices = sortedPrices,
                            connectionState = success?.connectionState ?: ConnectionState.DISCONNECTED,
                            isFeedRunning = success?.isFeedRunning ?: false
                        )
                    }
                }
        }
    }

    fun startPriceFeed() {
        viewModelScope.launch {
            try {
                startPriceFeedUseCase()
            } catch (e: Exception) {
                _uiState.value = PriceFeedUiState.Error(e.message ?: "Failed to start price feed")
            }
        }
    }

    fun stopPriceFeed() {
        viewModelScope.launch {
            try {
                stopPriceFeedUseCase()
            } catch (e: Exception) {
                _uiState.value = PriceFeedUiState.Error(e.message ?: "Failed to stop price feed")
            }
        }
    }

    fun togglePriceFeed() {
        val isRunning = (_uiState.value as? PriceFeedUiState.Success)?.isFeedRunning ?: false
        if (isRunning) stopPriceFeed() else startPriceFeed()
    }

    fun loadStockSymbols() {
        viewModelScope.launch {
            try {
                getStockSymbolsUseCase()
            } catch (e: Exception) {
                _uiState.value = PriceFeedUiState.Error(e.message ?: "Failed to load stock symbols")
            }
        }
    }
}
