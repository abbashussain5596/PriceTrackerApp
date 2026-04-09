package com.abbas.pricetrackerapp.domain.usecase

import com.abbas.pricetrackerapp.domain.repository.PriceRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetStockSymbolsUseCaseTest {

    private val repository: PriceRepository = mockk()
    private lateinit var useCase: GetStockSymbolsUseCase

    @Before
    fun setUp() {
        useCase = GetStockSymbolsUseCase(repository)
    }

    @Test
    fun `invoke returns the symbol list from repository`() = runTest {
        val expected = listOf("AAPL", "GOOG", "TSLA")
        coEvery { repository.getStockSymbols() } returns expected

        val result = useCase()

        assertEquals(expected, result)
    }

    @Test
    fun `invoke delegates to repository exactly once`() = runTest {
        coEvery { repository.getStockSymbols() } returns emptyList()

        useCase()

        coVerify(exactly = 1) { repository.getStockSymbols() }
    }

    @Test
    fun `invoke returns empty list when repository returns empty`() = runTest {
        coEvery { repository.getStockSymbols() } returns emptyList()

        val result = useCase()

        assertEquals(emptyList<String>(), result)
    }
}
