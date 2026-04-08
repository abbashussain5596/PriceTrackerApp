package com.abbas.pricetrackerapp.data.repository

import com.abbas.pricetrackerapp.data.datasource.WebSocketDataSource
import com.abbas.pricetrackerapp.data.mapper.PriceMapper
import com.abbas.pricetrackerapp.data.model.PriceUpdate
import com.abbas.pricetrackerapp.data.model.StockSymbols
import com.abbas.pricetrackerapp.domain.model.StockPrice
import com.abbas.pricetrackerapp.domain.repository.PriceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Random

class PriceRepositoryImpl(
    private val webSocketDataSource: WebSocketDataSource
) : PriceRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var priceFeedJob: Job? = null
    private var connectionJob: Job? = null
    private val _isRunning = MutableStateFlow(false)

    private val currentPrices = mutableMapOf<String, Double>()
    private val previousPrices = mutableMapOf<String, Double>()

    init {
        StockSymbols.SYMBOLS.forEach { symbol ->
            currentPrices[symbol] = generateRandomPrice()
        }
    }

    private val _priceUpdatesFlow: SharedFlow<StockPrice> = webSocketDataSource.observePriceUpdates()
        .map { priceUpdate ->
            val previousPrice = previousPrices[priceUpdate.symbol]
            val stockPrice = PriceMapper.toDomain(priceUpdate, previousPrice)
            previousPrices[priceUpdate.symbol] = priceUpdate.price
            stockPrice
        }
        .shareIn(
            scope = repositoryScope,
            started = SharingStarted.Eagerly,
            replay = 0
        )

    override suspend fun getStockSymbols(): List<String> = StockSymbols.SYMBOLS

    override suspend fun startPriceFeed() {
        if (_isRunning.value && priceFeedJob?.isActive == true) return

        priceFeedJob?.cancel()
        connectionJob?.cancel()

        connectionJob = repositoryScope.launch {
            webSocketDataSource.connect().collect { connected ->
                _isRunning.value = connected
            }
        }

        priceFeedJob = repositoryScope.launch {
            // Suspend until the connection is established — no polling needed
            webSocketDataSource.observeConnectionState().first { it }

            // Send initial batch once connected
            sendPriceUpdatesForAllSymbols()

            // Continue sending every 2 seconds for as long as connected
            while (isActive) {
                delay(2000)
                sendPriceUpdatesForAllSymbols()
            }
        }
    }

    override suspend fun stopPriceFeed() {
        priceFeedJob?.cancel()
        priceFeedJob = null
        connectionJob?.cancel()
        connectionJob = null
        webSocketDataSource.disconnect()
        _isRunning.value = false
    }

    override fun observePriceUpdates(): Flow<StockPrice> = _priceUpdatesFlow

    override fun observeConnectionState(): Flow<Boolean> = webSocketDataSource.observeConnectionState()

    override fun isPriceFeedRunning(): Boolean =
        _isRunning.value && priceFeedJob?.isActive == true

    override fun observeFeedRunningState(): Flow<Boolean> = _isRunning.asStateFlow()

    private suspend fun sendPriceUpdatesForAllSymbols() {
        StockSymbols.SYMBOLS.forEach { symbol ->
            val newPrice = generatePriceUpdate(symbol)
            webSocketDataSource.sendPriceUpdate(
                PriceUpdate(symbol, newPrice, System.currentTimeMillis())
            )
        }
    }

    private fun generateRandomPrice(): Double {
        val random = Random()
        return 10.0 + random.nextDouble() * (500.0 - 10.0)
    }

    private fun generatePriceUpdate(symbol: String): Double {
        val currentPrice = currentPrices[symbol] ?: generateRandomPrice()
        val random = Random()
        val changePercent = -5.0 + random.nextDouble() * 10.0
        val newPrice = currentPrice * (1 + changePercent / 100.0)
        val roundedPrice = String.format("%.2f", newPrice).toDouble()
        currentPrices[symbol] = roundedPrice
        return roundedPrice
    }

    fun cleanup() {
        priceFeedJob?.cancel()
        repositoryScope.cancel()
    }
}
