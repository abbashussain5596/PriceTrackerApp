package com.abbas.pricetrackerapp.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abbas.pricetrackerapp.domain.model.StockPrice
import com.abbas.pricetrackerapp.presentation.state.ConnectionState
import com.abbas.pricetrackerapp.presentation.state.PriceFeedUiState
import com.abbas.pricetrackerapp.presentation.ui.feed.PriceFeedContent
import com.abbas.pricetrackerapp.ui.theme.PriceTrackerAppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PriceFeedScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private val successEmpty = PriceFeedUiState.Success(
        stockPrices = emptyList(),
        connectionState = ConnectionState.DISCONNECTED,
        isFeedRunning = false
    )

    private fun setContent(
        uiState: PriceFeedUiState,
        onSymbolClick: (String) -> Unit = {},
        onToggleFeed: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            PriceTrackerAppTheme {
                PriceFeedContent(
                    uiState = uiState,
                    onSymbolClick = onSymbolClick,
                    onToggleFeed = onToggleFeed
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // App bar
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun topBar_shows_app_title() {
        setContent(successEmpty)
        composeTestRule.onNodeWithText("Price Tracker").assertIsDisplayed()
    }

    @Test
    fun topBar_shows_Start_when_feed_is_not_running() {
        setContent(successEmpty.copy(isFeedRunning = false))
        composeTestRule.onNodeWithText("Start").assertIsDisplayed()
    }

    @Test
    fun topBar_shows_Stop_when_feed_is_running() {
        setContent(successEmpty.copy(isFeedRunning = true))
        composeTestRule.onNodeWithText("Stop").assertIsDisplayed()
    }

    @Test
    fun clicking_Start_invokes_onToggleFeed() {
        var toggled = false
        setContent(uiState = successEmpty.copy(isFeedRunning = false), onToggleFeed = { toggled = true })

        composeTestRule.onNodeWithText("Start").performClick()

        assertTrue(toggled)
    }

    @Test
    fun clicking_Stop_invokes_onToggleFeed() {
        var toggled = false
        setContent(uiState = successEmpty.copy(isFeedRunning = true), onToggleFeed = { toggled = true })

        composeTestRule.onNodeWithText("Stop").performClick()

        assertTrue(toggled)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Loading state
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun loading_state_does_not_show_empty_placeholder_or_error() {
        setContent(PriceFeedUiState.Loading)
        // In Loading, neither the empty-state placeholder nor an error is shown
        composeTestRule.onNodeWithText("No stock prices available.", substring = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("Error:", substring = true).assertDoesNotExist()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Success state
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun success_with_empty_list_shows_empty_placeholder() {
        setContent(successEmpty)
        composeTestRule.onNodeWithText("No stock prices available.", substring = true).assertIsDisplayed()
    }

    @Test
    fun success_with_prices_shows_stock_symbol() {
        val uiState = successEmpty.copy(
            stockPrices = listOf(StockPrice(symbol = "AAPL", price = 182.50))
        )
        setContent(uiState)

        composeTestRule.onNodeWithText("AAPL").assertIsDisplayed()
    }

    @Test
    fun success_with_prices_shows_formatted_price() {
        val uiState = successEmpty.copy(
            stockPrices = listOf(StockPrice(symbol = "AAPL", price = 182.50))
        )
        setContent(uiState)

        composeTestRule.onNodeWithText("$182.50").assertIsDisplayed()
    }

    @Test
    fun success_with_multiple_prices_shows_all_symbols() {
        val uiState = successEmpty.copy(
            stockPrices = listOf(
                StockPrice(symbol = "AAPL", price = 182.50),
                StockPrice(symbol = "TSLA", price = 301.00)
            )
        )
        setContent(uiState)

        composeTestRule.onNodeWithText("AAPL").assertIsDisplayed()
        composeTestRule.onNodeWithText("TSLA").assertIsDisplayed()
    }

    @Test
    fun clicking_stock_row_passes_correct_symbol_to_onSymbolClick() {
        var clickedSymbol = ""
        val uiState = successEmpty.copy(
            stockPrices = listOf(StockPrice(symbol = "NVDA", price = 890.00))
        )
        setContent(uiState = uiState, onSymbolClick = { clickedSymbol = it })

        composeTestRule.onNodeWithText("NVDA").performClick()

        assertEquals("NVDA", clickedSymbol)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Error state
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun error_state_shows_error_message() {
        setContent(PriceFeedUiState.Error(message = "Connection lost"))
        composeTestRule.onNodeWithText("Error: Connection lost").assertIsDisplayed()
    }
}
