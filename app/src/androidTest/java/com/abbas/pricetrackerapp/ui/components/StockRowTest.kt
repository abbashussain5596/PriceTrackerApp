package com.abbas.pricetrackerapp.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abbas.pricetrackerapp.domain.model.StockPrice
import com.abbas.pricetrackerapp.presentation.ui.feed.components.StockRow
import com.abbas.pricetrackerapp.ui.theme.PriceTrackerAppTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StockRowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setRow(stockPrice: StockPrice, onClick: () -> Unit = {}) {
        composeTestRule.setContent {
            PriceTrackerAppTheme {
                StockRow(stockPrice = stockPrice, onClick = onClick)
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun shows_symbol_name() {
        setRow(StockPrice(symbol = "MSFT", price = 420.00))
        composeTestRule.onNodeWithText("MSFT").assertIsDisplayed()
    }

    @Test
    fun shows_formatted_price() {
        setRow(StockPrice(symbol = "MSFT", price = 420.00))
        composeTestRule.onNodeWithText("$420.00").assertIsDisplayed()
    }

    @Test
    fun clicking_the_row_triggers_onClick_callback() {
        var clicked = false
        setRow(StockPrice(symbol = "MSFT", price = 420.00), onClick = { clicked = true })

        composeTestRule.onNodeWithText("MSFT").performClick()

        assertTrue(clicked)
    }

    @Test
    fun shows_up_arrow_when_price_increased() {
        setRow(StockPrice(symbol = "GOOG", price = 200.00, previousPrice = 150.00))
        composeTestRule.onNodeWithText("↑").assertIsDisplayed()
    }

    @Test
    fun shows_down_arrow_when_price_decreased() {
        setRow(StockPrice(symbol = "GOOG", price = 100.00, previousPrice = 150.00))
        composeTestRule.onNodeWithText("↓").assertIsDisplayed()
    }

    @Test
    fun shows_no_arrow_when_no_previous_price() {
        setRow(StockPrice(symbol = "GOOG", price = 100.00, previousPrice = null))
        composeTestRule.onNodeWithText("↑").assertDoesNotExist()
        composeTestRule.onNodeWithText("↓").assertDoesNotExist()
    }
}
