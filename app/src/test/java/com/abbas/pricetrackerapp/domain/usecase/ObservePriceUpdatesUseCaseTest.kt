package com.abbas.pricetrackerapp.domain.usecase

import app.cash.turbine.test
import com.abbas.pricetrackerapp.domain.model.StockPrice
import com.abbas.pricetrackerapp.domain.repository.PriceRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ObservePriceUpdatesUseCaseTest {

    private val priceUpdatesFlow = MutableSharedFlow<StockPrice>(extraBufferCapacity = 8)
    private val repository: PriceRepository = mockk()
    private lateinit var useCase: ObservePriceUpdatesUseCase

    @Before
    fun setUp() {
        every { repository.observePriceUpdates() } returns priceUpdatesFlow
        useCase = ObservePriceUpdatesUseCase(repository)
    }

    @Test
    fun `invoke returns the flow from repository`() {
        val result = useCase()

        assertEquals(priceUpdatesFlow, result)
    }

    @Test
    fun `invoke delegates to repository exactly once`() {
        useCase()

        verify(exactly = 1) { repository.observePriceUpdates() }
    }

    @Test
    fun `flow emits StockPrice items produced by the repository`() = runTest {
        val update = StockPrice(symbol = "AAPL", price = 182.10, timestamp = 1000L)

        useCase().test {
            priceUpdatesFlow.emit(update)
            val observed = awaitItem()
            assertEquals("AAPL", observed.symbol)
            assertEquals(182.10, observed.price, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `flow emits multiple items in the order they are produced`() = runTest {
        val updates = listOf(
            StockPrice(symbol = "AAPL", price = 182.10, timestamp = 1000L),
            StockPrice(symbol = "TSLA", price = 241.55, timestamp = 2000L),
            StockPrice(symbol = "NVDA", price = 890.00, timestamp = 3000L),
        )

        useCase().test {
            updates.forEach { priceUpdatesFlow.emit(it) }
            assertEquals("AAPL", awaitItem().symbol)
            assertEquals("TSLA", awaitItem().symbol)
            assertEquals("NVDA", awaitItem().symbol)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
