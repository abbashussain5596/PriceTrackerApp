package com.abbas.pricetrackerapp.domain.usecase

import com.abbas.pricetrackerapp.domain.repository.PriceRepository
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class StartPriceFeedUseCaseTest {

    private val repository: PriceRepository = mockk()
    private lateinit var useCase: StartPriceFeedUseCase

    @Before
    fun setUp() {
        useCase = StartPriceFeedUseCase(repository)
    }

    @Test
    fun `invoke delegates to repository exactly once`() = runTest {
        coJustRun { repository.startPriceFeed() }

        useCase()

        coVerify(exactly = 1) { repository.startPriceFeed() }
    }

    @Test
    fun `invoke does not call stopPriceFeed or any other method`() = runTest {
        coJustRun { repository.startPriceFeed() }

        useCase()

        coVerify(exactly = 0) { repository.stopPriceFeed() }
    }
}
