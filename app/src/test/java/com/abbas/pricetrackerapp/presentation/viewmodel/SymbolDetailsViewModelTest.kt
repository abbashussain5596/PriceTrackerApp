package com.abbas.pricetrackerapp.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.abbas.pricetrackerapp.domain.model.StockPrice
import com.abbas.pricetrackerapp.domain.repository.PriceRepository
import com.abbas.pricetrackerapp.domain.usecase.ObserveConnectionStateUseCase
import com.abbas.pricetrackerapp.domain.usecase.ObservePriceUpdatesUseCase
import com.abbas.pricetrackerapp.domain.usecase.StartPriceFeedUseCase
import com.abbas.pricetrackerapp.presentation.state.ConnectionState
import com.abbas.pricetrackerapp.presentation.state.SymbolDetailsUiState
import io.mockk.coJustRun
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SymbolDetailsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val priceUpdatesFlow = MutableSharedFlow<StockPrice>(extraBufferCapacity = 64)
    private val connectionStateFlow = MutableStateFlow(false)

    private val repository: PriceRepository = mockk()

    private lateinit var viewModel: SymbolDetailsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        every { repository.observePriceUpdates() } returns priceUpdatesFlow
        every { repository.observeConnectionState() } returns connectionStateFlow
        coJustRun { repository.startPriceFeed() }

        val savedStateHandle = SavedStateHandle().apply { set("symbol", "AAPL") }
        viewModel = SymbolDetailsViewModel(
            savedStateHandle = savedStateHandle,
            observePriceUpdatesUseCase = ObservePriceUpdatesUseCase(repository),
            observeConnectionStateUseCase = ObserveConnectionStateUseCase(repository),
            startPriceFeedUseCase = StartPriceFeedUseCase(repository)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Success with correct symbol and no price`() {
        val state = viewModel.uiState.value as SymbolDetailsUiState.Success
        assertEquals("AAPL", state.symbol)
        assertNull(state.stockPrice)
        assertEquals(ConnectionState.DISCONNECTED, state.connectionState)
    }

    @Test
    fun `symbol property reflects saved state handle value`() {
        assertEquals("AAPL", viewModel.symbol)
    }

    @Test
    fun `state updates when price arrives for the correct symbol`() = runTest {
        viewModel.uiState.test {
            awaitItem() // initial
            priceUpdatesFlow.emit(StockPrice(symbol = "AAPL", price = 182.50))
            val updated = awaitItem() as SymbolDetailsUiState.Success
            assertEquals(182.50, updated.stockPrice?.price ?: 0.0, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `state does not update when price arrives for a different symbol`() = runTest {
        viewModel.uiState.test {
            awaitItem() // initial
            priceUpdatesFlow.emit(StockPrice(symbol = "TSLA", price = 300.0))
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `state reflects connection state becoming connected`() = runTest {
        viewModel.uiState.test {
            awaitItem() // initial disconnected
            connectionStateFlow.emit(true)
            val connected = awaitItem() as SymbolDetailsUiState.Success
            assertEquals(ConnectionState.CONNECTED, connected.connectionState)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `state reflects connection state becoming disconnected after connected`() = runTest {
        viewModel.uiState.test {
            awaitItem() // initial
            connectionStateFlow.emit(true)
            awaitItem() // connected
            connectionStateFlow.emit(false)
            val disconnected = awaitItem() as SymbolDetailsUiState.Success
            assertEquals(ConnectionState.DISCONNECTED, disconnected.connectionState)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getPriceChangeIndicator returns null when no price available`() {
        assertNull(viewModel.getPriceChangeIndicator())
    }

    @Test
    fun `getPriceChangeIndicator returns true when price increased`() = runTest {
        priceUpdatesFlow.emit(StockPrice(symbol = "AAPL", price = 150.0, previousPrice = 140.0))
        assertEquals(true, viewModel.getPriceChangeIndicator())
    }

    @Test
    fun `getPriceChangeIndicator returns false when price decreased`() = runTest {
        priceUpdatesFlow.emit(StockPrice(symbol = "AAPL", price = 130.0, previousPrice = 140.0))
        assertEquals(false, viewModel.getPriceChangeIndicator())
    }
}
