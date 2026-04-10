package com.abbas.pricetrackerapp.presentation.viewmodel

import app.cash.turbine.test
import com.abbas.pricetrackerapp.domain.model.StockPrice
import com.abbas.pricetrackerapp.domain.repository.PriceRepository
import com.abbas.pricetrackerapp.domain.usecase.GetStockSymbolsUseCase
import com.abbas.pricetrackerapp.domain.usecase.ObserveConnectionStateUseCase
import com.abbas.pricetrackerapp.domain.usecase.ObserveFeedRunningStateUseCase
import com.abbas.pricetrackerapp.domain.usecase.ObservePriceUpdatesUseCase
import com.abbas.pricetrackerapp.domain.usecase.StartPriceFeedUseCase
import com.abbas.pricetrackerapp.domain.usecase.StopPriceFeedUseCase
import com.abbas.pricetrackerapp.presentation.state.ConnectionState
import com.abbas.pricetrackerapp.presentation.state.PriceFeedUiState
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PriceFeedViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val priceUpdatesFlow = MutableSharedFlow<StockPrice>(extraBufferCapacity = 64)
    private val connectionStateFlow = MutableStateFlow(false)
    private val feedRunningStateFlow = MutableStateFlow(false)

    private val repository: PriceRepository = mockk()

    private lateinit var viewModel: PriceFeedViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        every { repository.observePriceUpdates() } returns priceUpdatesFlow
        every { repository.observeConnectionState() } returns connectionStateFlow
        every { repository.observeFeedRunningState() } returns feedRunningStateFlow
        coJustRun { repository.startPriceFeed() }
        coJustRun { repository.stopPriceFeed() }

        viewModel = PriceFeedViewModel(
            getStockSymbolsUseCase = GetStockSymbolsUseCase(repository),
            observePriceUpdatesUseCase = ObservePriceUpdatesUseCase(repository),
            observeConnectionStateUseCase = ObserveConnectionStateUseCase(repository),
            observeFeedRunningStateUseCase = ObserveFeedRunningStateUseCase(repository),
            startPriceFeedUseCase = StartPriceFeedUseCase(repository),
            stopPriceFeedUseCase = StopPriceFeedUseCase(repository)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Success with empty list and disconnected state`() {
        val state = viewModel.uiState.value as PriceFeedUiState.Success
        assertTrue(state.stockPrices.isEmpty())
        assertEquals(ConnectionState.DISCONNECTED, state.connectionState)
        assertFalse(state.isFeedRunning)
    }

    @Test
    fun `uiState reflects connected state when connection state emits true`() = runTest {
        viewModel.uiState.test {
            awaitItem() // initial state
            connectionStateFlow.emit(true)
            val connected = awaitItem() as PriceFeedUiState.Success
            assertEquals(ConnectionState.CONNECTED, connected.connectionState)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `uiState shows CONNECTING when feed is running but not yet connected`() = runTest {
        viewModel.uiState.test {
            awaitItem() // initial state
            feedRunningStateFlow.emit(true)
            val connecting = awaitItem() as PriceFeedUiState.Success
            assertEquals(ConnectionState.CONNECTING, connecting.connectionState)
            assertTrue(connecting.isFeedRunning)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `uiState updates stock prices sorted by price descending when price update arrives`() = runTest {
        viewModel.uiState.test {
            awaitItem() // initial
            priceUpdatesFlow.emit(StockPrice(symbol = "AAPL", price = 150.0))
            val first = awaitItem() as PriceFeedUiState.Success
            assertEquals(1, first.stockPrices.size)
            assertEquals("AAPL", first.stockPrices[0].symbol)

            priceUpdatesFlow.emit(StockPrice(symbol = "TSLA", price = 250.0))
            val second = awaitItem() as PriceFeedUiState.Success
            assertEquals(2, second.stockPrices.size)
            assertEquals("TSLA", second.stockPrices[0].symbol) // highest price first
            assertEquals("AAPL", second.stockPrices[1].symbol)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `startPriceFeed delegates to StartPriceFeedUseCase`() = runTest {
        viewModel.startPriceFeed()
        coVerify(exactly = 1) { repository.startPriceFeed() }
    }

    @Test
    fun `stopPriceFeed delegates to StopPriceFeedUseCase`() = runTest {
        viewModel.stopPriceFeed()
        coVerify(exactly = 1) { repository.stopPriceFeed() }
    }

    @Test
    fun `togglePriceFeed starts feed when stopped`() = runTest {
        // isFeedRunning is false initially
        viewModel.togglePriceFeed()
        coVerify(exactly = 1) { repository.startPriceFeed() }
        coVerify(exactly = 0) { repository.stopPriceFeed() }
    }

    @Test
    fun `togglePriceFeed stops feed when running`() = runTest {
        feedRunningStateFlow.emit(true)

        viewModel.togglePriceFeed()
        coVerify(exactly = 1) { repository.stopPriceFeed() }
        coVerify(exactly = 0) { repository.startPriceFeed() }
    }

    @Test
    fun `price update for same symbol replaces previous entry in list`() = runTest {
        viewModel.uiState.test {
            awaitItem() // initial
            priceUpdatesFlow.emit(StockPrice(symbol = "AAPL", price = 150.0))
            awaitItem() // first update

            priceUpdatesFlow.emit(StockPrice(symbol = "AAPL", price = 160.0))
            val updated = awaitItem() as PriceFeedUiState.Success
            assertEquals(1, updated.stockPrices.size)
            assertEquals(160.0, updated.stockPrices[0].price, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
