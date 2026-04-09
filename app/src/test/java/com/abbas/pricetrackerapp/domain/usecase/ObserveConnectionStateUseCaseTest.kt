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

class ObserveConnectionStateUseCaseTest {

    private val connectionStateFlow = MutableStateFlow(false)
    private val repository: PriceRepository = mockk()
    private lateinit var useCase: ObserveConnectionStateUseCase

    @Before
    fun setUp() {
        every { repository.observeConnectionState() } returns connectionStateFlow
        useCase = ObserveConnectionStateUseCase(repository)
    }

    @Test
    fun `invoke returns the flow from repository`() {
        val result = useCase()

        assertEquals(connectionStateFlow, result)
    }

    @Test
    fun `invoke delegates to repository exactly once`() {
        useCase()

        verify(exactly = 1) { repository.observeConnectionState() }
    }

    @Test
    fun `flow emits false initially when disconnected`() = runTest {
        useCase().test {
            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `flow emits true when connection is established`() = runTest {
        useCase().test {
            awaitItem() // initial false

            connectionStateFlow.value = true
            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `flow reflects each connection state transition`() = runTest {
        useCase().test {
            assertFalse(awaitItem())       // disconnected

            connectionStateFlow.value = true
            assertTrue(awaitItem())        // connected

            connectionStateFlow.value = false
            assertFalse(awaitItem())       // disconnected again
            cancelAndIgnoreRemainingEvents()
        }
    }
}
