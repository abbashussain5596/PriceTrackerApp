package com.abbas.pricetrackerapp.presentation.ui.feed

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abbas.pricetrackerapp.presentation.state.ConnectionState
import com.abbas.pricetrackerapp.presentation.state.PriceFeedUiState
import com.abbas.pricetrackerapp.presentation.ui.feed.components.ConnectionStatusIndicator
import com.abbas.pricetrackerapp.presentation.ui.feed.components.StockRow
import com.abbas.pricetrackerapp.presentation.viewmodel.PriceFeedViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun PriceFeedScreen(
    onSymbolClick: (String) -> Unit,
    viewModel: PriceFeedViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val connectionState = (uiState as? PriceFeedUiState.Success)?.connectionState
        ?: ConnectionState.DISCONNECTED
    val isFeedRunning = (uiState as? PriceFeedUiState.Success)?.isFeedRunning ?: false

    Scaffold(
        topBar = {
            PriceFeedTopBar(
                connectionState = connectionState,
                isFeedRunning = isFeedRunning,
                onToggleFeed = { viewModel.togglePriceFeed() }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is PriceFeedUiState.Loading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )

                is PriceFeedUiState.Success -> {
                    if (state.stockPrices.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "No stock prices available.\nTap Start to begin.",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(items = state.stockPrices, key = { it.symbol }) { stockPrice ->
                                StockRow(
                                    stockPrice = stockPrice,
                                    onClick = { onSymbolClick(stockPrice.symbol) }
                                )
                            }
                        }
                    }
                }

                is PriceFeedUiState.Error -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Error: ${state.message}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PriceFeedTopBar(
    connectionState: ConnectionState,
    isFeedRunning: Boolean,
    onToggleFeed: () -> Unit
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ConnectionStatusIndicator(
                    connectionState = connectionState,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Price Tracker")
            }
        },
        actions = {
            TextButton(
                onClick = onToggleFeed,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(text = if (isFeedRunning) "Stop" else "Start")
            }
        }
    )
}
