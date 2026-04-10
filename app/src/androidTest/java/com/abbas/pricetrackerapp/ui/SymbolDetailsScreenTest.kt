package com.abbas.pricetrackerapp.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abbas.pricetrackerapp.domain.model.StockPrice
import com.abbas.pricetrackerapp.presentation.state.ConnectionState
import com.abbas.pricetrackerapp.presentation.state.SymbolDetailsUiState
import com.abbas.pricetrackerapp.presentation.ui.details.SymbolDetailsContent
import com.abbas.pricetrackerapp.ui.theme.PriceTrackerAppTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SymbolDetailsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private fun setContent(
        symbol: String = "AAPL",
        uiState: SymbolDetailsUiState,
        onBackClick: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            PriceTrackerAppTheme {
                SymbolDetailsContent(
                    symbol = symbol,
                    uiState = uiState,
                    onBackClick = onBackClick
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun successState(
        symbol: String = "AAPL",
        stockPrice: StockPrice? = null,
        connectionState: ConnectionState = ConnectionState.DISCONNECTED
    ) = SymbolDetailsUiState.Success(
        symbol = symbol,
        stockPrice = stockPrice,
        connectionState = connectionState
    )

    // ──────────────────────────────────────────────────────────────────────────
    // Top bar
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun topBar_shows_symbol_name() {
        setContent(symbol = "TSLA", uiState = successState(symbol = "TSLA"))
        // "TSLA" appears in both the top bar and the price card — use onFirst()
        composeTestRule.onAllNodesWithText("TSLA").onFirst().assertIsDisplayed()
    }

    @Test
    fun clicking_back_button_invokes_onBackClick() {
        var backClicked = false
        setContent(uiState = successState(), onBackClick = { backClicked = true })

        composeTestRule.onNodeWithContentDescription("Back").performClick()

        assertTrue(backClicked)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Loading state
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun loading_state_does_not_show_stock_content() {
        setContent(uiState = SymbolDetailsUiState.Loading)
        composeTestRule.onNodeWithText("Price data loading...").assertDoesNotExist()
        composeTestRule.onNodeWithText("About", substring = true).assertDoesNotExist()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Success state — no price yet
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun success_shows_placeholder_when_no_price_available() {
        setContent(uiState = successState(stockPrice = null))
        composeTestRule.onNodeWithText("Price data loading...").assertIsDisplayed()
    }

    @Test
    fun success_shows_about_card_for_symbol() {
        setContent(uiState = successState())
        composeTestRule.onNodeWithText("About AAPL").assertIsDisplayed()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Success state — price available
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun success_shows_formatted_price_when_stock_price_available() {
        val price = StockPrice(symbol = "AAPL", price = 182.50)
        setContent(uiState = successState(stockPrice = price))

        composeTestRule.onNodeWithText("$182.50").assertIsDisplayed()
    }

    @Test
    fun success_shows_up_arrow_when_price_increased() {
        val price = StockPrice(symbol = "AAPL", price = 200.00, previousPrice = 150.00)
        setContent(uiState = successState(stockPrice = price))

        composeTestRule.onNodeWithText("↑").assertIsDisplayed()
    }

    @Test
    fun success_shows_down_arrow_when_price_decreased() {
        val price = StockPrice(symbol = "AAPL", price = 100.00, previousPrice = 150.00)
        setContent(uiState = successState(stockPrice = price))

        composeTestRule.onNodeWithText("↓").assertIsDisplayed()
    }

    @Test
    fun success_shows_positive_change_percentage_when_price_increased() {
        val price = StockPrice(symbol = "AAPL", price = 200.00, previousPrice = 100.00)
        setContent(uiState = successState(stockPrice = price))

        composeTestRule.onNodeWithText("+100.00%").assertIsDisplayed()
    }

    @Test
    fun success_shows_negative_change_percentage_when_price_decreased() {
        val price = StockPrice(symbol = "AAPL", price = 90.00, previousPrice = 100.00)
        setContent(uiState = successState(stockPrice = price))

        composeTestRule.onNodeWithText("-10.00%").assertIsDisplayed()
    }

    @Test
    fun success_hides_change_percentage_when_no_previous_price() {
        val price = StockPrice(symbol = "AAPL", price = 150.00, previousPrice = null)
        setContent(uiState = successState(stockPrice = price))

        composeTestRule.onNodeWithText("%", substring = true).assertDoesNotExist()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Error state
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun error_state_shows_error_message() {
        setContent(uiState = SymbolDetailsUiState.Error(message = "Feed unavailable"))
        composeTestRule.onNodeWithText("Error: Feed unavailable").assertIsDisplayed()
    }
}
