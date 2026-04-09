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
    private var feedJob: Job? = null
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
        if (feedJob?.isActive == true) return

        feedJob?.cancel()
        _isRunning.value = true

        feedJob = repositoryScope.launch {
            // Open the WebSocket and suspend until it reports connected.
            // connect() opens the socket eagerly as a side effect, then returns
            // observeConnectionState() — so .first { it } waits for the first true event.
            webSocketDataSource.connect().first { it }

            sendPriceUpdatesForAllSymbols()

            while (isActive) {
                delay(Config.PRICE_UPDATE_INTERVAL_MS)
                sendPriceUpdatesForAllSymbols()
            }
        }

        feedJob?.invokeOnCompletion { _isRunning.value = false }
    }

    override suspend fun stopPriceFeed() {
        feedJob?.cancel()
        feedJob = null
        webSocketDataSource.disconnect()
    }

    override fun observePriceUpdates(): Flow<StockPrice> = _priceUpdatesFlow

    override fun observeConnectionState(): Flow<Boolean> = webSocketDataSource.observeConnectionState()

    override fun isPriceFeedRunning(): Boolean = feedJob?.isActive == true

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
        return Config.MIN_BASE_PRICE + random.nextDouble() * (Config.MAX_BASE_PRICE - Config.MIN_BASE_PRICE)
    }

    private fun generatePriceUpdate(symbol: String): Double {
        val currentPrice = currentPrices[symbol] ?: generateRandomPrice()
        val random = Random()
        val changePercent = -Config.MAX_CHANGE_PERCENT + random.nextDouble() * (Config.MAX_CHANGE_PERCENT * 2)
        val newPrice = currentPrice * (1 + changePercent / 100.0)
        val roundedPrice = String.format("%.${Config.PRICE_DECIMAL_PLACES}f", newPrice).toDouble()
        currentPrices[symbol] = roundedPrice
        return roundedPrice
    }

    fun cleanup() {
        feedJob?.cancel()
        repositoryScope.cancel()
    }
}

private object Config {
    const val PRICE_UPDATE_INTERVAL_MS = 2000L
    const val MIN_BASE_PRICE = 15.0
    const val MAX_BASE_PRICE = 600.0
    const val MAX_CHANGE_PERCENT = 7.0
    const val PRICE_DECIMAL_PLACES = 2
}
