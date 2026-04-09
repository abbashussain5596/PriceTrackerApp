package com.abbas.pricetrackerapp.data.repository

import app.cash.turbine.test
import com.abbas.pricetrackerapp.data.datasource.WebSocketDataSource
import com.abbas.pricetrackerapp.data.model.PriceUpdate
import com.abbas.pricetrackerapp.data.model.StockSymbols
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PriceRepositoryImplTest {

    // Backing flows that drive the mock's return values — controlled per-test
    private val connectionStateFlow = MutableStateFlow(false)
    private val priceUpdatesFlow = MutableSharedFlow<PriceUpdate>(extraBufferCapacity = 64)

    private val dataSource: WebSocketDataSource = mockk()
    private lateinit var repository: PriceRepositoryImpl

    @Before
    fun setUp() {
        // observePriceUpdates() is called at construction time (in _priceUpdatesFlow),
        // so it must be stubbed before PriceRepositoryImpl is instantiated.
        every { dataSource.observePriceUpdates() } returns priceUpdatesFlow
        every { dataSource.observeConnectionState() } returns connectionStateFlow
        every { dataSource.connect() } returns connectionStateFlow
        coJustRun { dataSource.disconnect() }
        coJustRun { dataSource.sendPriceUpdate(any()) }

        repository = PriceRepositoryImpl(dataSource)
    }

    @After
    fun tearDown() {
        repository.cleanup()
    }

    // ─── getStockSymbols ─────────────────────────────────────────────────────

    @Test
    fun `getStockSymbols returns all 25 symbols`() = runTest {
        val symbols = repository.getStockSymbols()
        assertEquals(25, symbols.size)
    }

    @Test
    fun `getStockSymbols returns the canonical StockSymbols list`() = runTest {
        val symbols = repository.getStockSymbols()
        assertEquals(StockSymbols.SYMBOLS, symbols)
    }

    // ─── isPriceFeedRunning ───────────────────────────────────────────────────

    @Test
    fun `isPriceFeedRunning is false before feed is started`() {
        assertFalse(repository.isPriceFeedRunning())
    }

    // ─── stopPriceFeed ────────────────────────────────────────────────────────

    @Test
    fun `stopPriceFeed calls disconnect on the data source exactly once`() = runTest {
        repository.stopPriceFeed()
        coVerify(exactly = 1) { dataSource.disconnect() }
    }

    @Test
    fun `stopPriceFeed sets feed running state to false`() = runTest {
        repository.stopPriceFeed()
        assertFalse(repository.isPriceFeedRunning())
    }

    // ─── observeConnectionState ───────────────────────────────────────────────

    @Test
    fun `observeConnectionState delegates to the data source`() = runTest {
        repository.observeConnectionState().test {
            assertFalse(awaitItem())        // initial false from MutableStateFlow
            connectionStateFlow.value = true
            assertTrue(awaitItem())
            connectionStateFlow.value = false
            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ─── observeFeedRunningState ──────────────────────────────────────────────

    @Test
    fun `observeFeedRunningState emits false initially`() = runTest {
        repository.observeFeedRunningState().test {
            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ─── observePriceUpdates ─────────────────────────────────────────────────

    @Test
    fun `price updates are mapped from PriceUpdate to StockPrice`() = runTest {
        repository.observePriceUpdates().test {
            priceUpdatesFlow.emit(PriceUpdate(symbol = "AAPL", price = 182.10, timestamp = 1000L))
            val result = awaitItem()
            assertEquals("AAPL", result.symbol)
            assertEquals(182.10, result.price, 0.001)
            assertEquals(1000L, result.timestamp)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `first price update for a symbol has no previousPrice`() = runTest {
        repository.observePriceUpdates().test {
            priceUpdatesFlow.emit(PriceUpdate(symbol = "AAPL", price = 182.10, timestamp = 1000L))
            assertNull(awaitItem().previousPrice)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `second price update for same symbol carries previousPrice`() = runTest {
        repository.observePriceUpdates().test {
            priceUpdatesFlow.emit(PriceUpdate(symbol = "AAPL", price = 182.10, timestamp = 1000L))
            awaitItem() // first — previousPrice is null

            priceUpdatesFlow.emit(PriceUpdate(symbol = "AAPL", price = 185.00, timestamp = 2000L))
            val second = awaitItem()

            assertNotNull(second.previousPrice)
            assertEquals(182.10, second.previousPrice!!, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `price updates from different symbols track previousPrice independently`() = runTest {
        repository.observePriceUpdates().test {
            priceUpdatesFlow.emit(PriceUpdate(symbol = "AAPL", price = 182.10, timestamp = 1000L))
            assertNull(awaitItem().previousPrice)

            priceUpdatesFlow.emit(PriceUpdate(symbol = "TSLA", price = 241.55, timestamp = 2000L))
            // TSLA has no history — AAPL's state must not bleed into it
            assertNull(awaitItem().previousPrice)

            priceUpdatesFlow.emit(PriceUpdate(symbol = "AAPL", price = 185.00, timestamp = 3000L))
            assertEquals(182.10, awaitItem().previousPrice!!, 0.001)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `changePercentage is calculated correctly from consecutive updates`() = runTest {
        repository.observePriceUpdates().test {
            priceUpdatesFlow.emit(PriceUpdate(symbol = "AAPL", price = 100.0, timestamp = 1000L))
            awaitItem()

            priceUpdatesFlow.emit(PriceUpdate(symbol = "AAPL", price = 110.0, timestamp = 2000L))
            val result = awaitItem()

            assertEquals(10.0, result.changePercentage, 0.001)
            assertTrue(result.isPriceIncreased)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ─── round-trip: send → observe ──────────────────────────────────────────

    @Test
    fun `random price update sent to data source is observed back through the repository`() = runTest {
        // Capture whatever is passed to sendPriceUpdate (simulating WebSocket send).
        val capturedUpdate = slot<PriceUpdate>()
        coEvery { dataSource.sendPriceUpdate(capture(capturedUpdate)) } just Runs

        val randomSymbol = StockSymbols.SYMBOLS.random()
        val randomPrice = Random.nextDouble(10.0, 500.0)
        val randomTimestamp = Random.nextLong(1_000_000L, 9_999_999L)
        val sent = PriceUpdate(symbol = randomSymbol, price = randomPrice, timestamp = randomTimestamp)

        repository.observePriceUpdates().test {
            dataSource.sendPriceUpdate(sent)

            // Echo the captured update back through the flow, simulating the
            // WebSocket server returning exactly what the client sent.
            priceUpdatesFlow.emit(capturedUpdate.captured)

            val observed = awaitItem()
            assertEquals(sent.symbol, observed.symbol)
            assertEquals(sent.price, observed.price, 0.001)
            assertEquals(sent.timestamp, observed.timestamp)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
