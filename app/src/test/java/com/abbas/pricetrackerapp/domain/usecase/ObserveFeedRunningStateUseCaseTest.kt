package com.abbas.pricetrackerapp.domain.usecase

import app.cash.turbine.test
import com.abbas.pricetrackerapp.domain.repository.PriceRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ObserveFeedRunningStateUseCaseTest {

    private val feedRunningStateFlow = MutableStateFlow(false)
    private val repository: PriceRepository = mockk()
    private lateinit var useCase: ObserveFeedRunningStateUseCase

    @Before
    fun setUp() {
        every { repository.observeFeedRunningState() } returns feedRunningStateFlow
        useCase = ObserveFeedRunningStateUseCase(repository)
    }

    @Test
    fun `invoke returns the flow from repository`() {
        val result = useCase()

        assertEquals(feedRunningStateFlow, result)
    }

    @Test
    fun `invoke delegates to repository exactly once`() {
        useCase()

        verify(exactly = 1) { repository.observeFeedRunningState() }
    }

    @Test
    fun `flow emits false initially when feed is stopped`() = runTest {
        useCase().test {
            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `flow emits true when feed starts running`() = runTest {
        useCase().test {
            awaitItem() // initial false

            feedRunningStateFlow.value = true
            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `flow reflects start and stop transitions`() = runTest {
        useCase().test {
            assertFalse(awaitItem())       // stopped

            feedRunningStateFlow.value = true
            assertTrue(awaitItem())        // started

            feedRunningStateFlow.value = false
            assertFalse(awaitItem())       // stopped again
            cancelAndIgnoreRemainingEvents()
        }
    }
}
