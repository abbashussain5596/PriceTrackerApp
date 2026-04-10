package com.abbas.pricetrackerapp.presentation.ui.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abbas.pricetrackerapp.presentation.state.ConnectionState
import com.abbas.pricetrackerapp.presentation.state.SymbolDetailsUiState
import com.abbas.pricetrackerapp.presentation.ui.feed.components.ConnectionStatusIndicator
import com.abbas.pricetrackerapp.presentation.ui.feed.components.PriceChangeIndicator
import com.abbas.pricetrackerapp.presentation.viewmodel.SymbolDetailsViewModel
import org.koin.androidx.compose.koinViewModel

/** Stateful entry-point — wires the ViewModel to [SymbolDetailsContent]. */
@Composable
fun SymbolDetailsScreen(
    onBackClick: () -> Unit,
    viewModel: SymbolDetailsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SymbolDetailsContent(
        symbol = viewModel.symbol,
        uiState = uiState,
        onBackClick = onBackClick
    )
}

/**
 * Stateless content composable.  Receives plain state — no ViewModel, no Koin.
 * This is the composable that Compose UI tests target directly.
 */
@Composable
fun SymbolDetailsContent(
    symbol: String,
    uiState: SymbolDetailsUiState,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            SymbolDetailsTopBar(
                symbol = symbol,
                connectionState = (uiState as? SymbolDetailsUiState.Success)?.connectionState
                    ?: ConnectionState.DISCONNECTED,
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is SymbolDetailsUiState.Loading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )

                is SymbolDetailsUiState.Success -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = state.symbol,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.padding(16.dp))
                            state.stockPrice?.let { stockPrice ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    PriceChangeIndicator(
                                        isPriceIncreased = stockPrice.isPriceIncreased.takeIf {
                                            stockPrice.previousPrice != null
                                        },
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Text(
                                        text = "$${String.format("%.2f", stockPrice.price)}",
                                        style = MaterialTheme.typography.displayMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (stockPrice.previousPrice != null) {
                                    Spacer(modifier = Modifier.padding(8.dp))
                                    val changePercent = stockPrice.changePercentage
                                    val changeColor = if (changePercent >= 0) Color(0xFF4CAF50)
                                    else Color(0xFFF44336)
                                    Text(
                                        text = "${if (changePercent >= 0) "+" else ""}${
                                            String.format("%.2f", changePercent)
                                        }%",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = changeColor,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            } ?: Text(
                                text = "Price data loading...",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    Spacer(modifier = Modifier.padding(24.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "About ${state.symbol}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = getSymbolDescription(state.symbol),
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Justify
                            )
                        }
                    }
                }

                is SymbolDetailsUiState.Error -> Box(
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
private fun SymbolDetailsTopBar(
    symbol: String,
    connectionState: ConnectionState,
    onBackClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ConnectionStatusIndicator(
                    connectionState = connectionState,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(symbol)
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        }
    )
}

private fun getSymbolDescription(symbol: String): String = when (symbol) {
    "AAPL" -> "Apple Inc. designs, manufactures, and markets smartphones, personal computers, tablets, wearables, and accessories worldwide."
    "GOOG" -> "Alphabet Inc. provides various products and platforms in the United States, Europe, the Middle East, Africa, the Asia-Pacific, Canada, and Latin America."
    "TSLA" -> "Tesla, Inc. designs, develops, manufactures, leases, and sells electric vehicles, and energy generation and storage systems."
    "AMZN" -> "Amazon.com, Inc. engages in the retail sale of consumer products and subscriptions in North America and internationally."
    "MSFT" -> "Microsoft Corporation develops, licenses, and supports software, services, devices, and solutions worldwide."
    "NVDA" -> "NVIDIA Corporation provides graphics and compute and networking solutions in the United States, Taiwan, China, and internationally."
    "META" -> "Meta Platforms, Inc. engages in building products that help people connect and share with friends and family through mobile devices, personal computers, virtual reality headsets, and wearables worldwide."
    "NFLX" -> "Netflix, Inc. provides entertainment services. It offers TV series, documentaries, feature films, and mobile games across various genres and languages."
    else -> "$symbol is a publicly traded company. This is a real-time price tracking application that displays live stock price updates."
}
