package com.abbas.pricetrackerapp.domain.repository

import com.abbas.pricetrackerapp.domain.model.StockPrice
import kotlinx.coroutines.flow.Flow

interface PriceRepository {
    suspend fun getStockSymbols(): List<String>
    suspend fun startPriceFeed()
    suspend fun stopPriceFeed()
    fun observePriceUpdates(): Flow<StockPrice>
    fun observeConnectionState(): Flow<Boolean>
    fun isPriceFeedRunning(): Boolean
    fun observeFeedRunningState(): Flow<Boolean>
}
