package com.abbas.pricetrackerapp.domain.usecase

import com.abbas.pricetrackerapp.domain.repository.PriceRepository
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class StopPriceFeedUseCaseTest {

    private val repository: PriceRepository = mockk()
    private lateinit var useCase: StopPriceFeedUseCase

    @Before
    fun setUp() {
        useCase = StopPriceFeedUseCase(repository)
    }

    @Test
    fun `invoke delegates to repository exactly once`() = runTest {
        coJustRun { repository.stopPriceFeed() }

        useCase()

        coVerify(exactly = 1) { repository.stopPriceFeed() }
    }

    @Test
    fun `invoke does not call startPriceFeed or any other method`() = runTest {
        coJustRun { repository.stopPriceFeed() }

        useCase()

        coVerify(exactly = 0) { repository.startPriceFeed() }
    }
}
