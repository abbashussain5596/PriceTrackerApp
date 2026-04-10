package com.abbas.pricetrackerapp.presentation.ui.feed.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.abbas.pricetrackerapp.domain.model.StockPrice
import kotlinx.coroutines.delay

@Composable
fun StockRow(
    stockPrice: StockPrice,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var previousPrice by remember(stockPrice.symbol) { mutableStateOf<Double?>(null) }
    var showFlash by remember { mutableStateOf(false) }
    var flashColor by remember { mutableStateOf<Color?>(null) }

    LaunchedEffect(stockPrice.price, stockPrice.symbol) {
        val prev = previousPrice
        if (prev != null && prev != stockPrice.price) {
            flashColor = if (stockPrice.price > prev) Color(0xFF4CAF50).copy(alpha = 0.3f)
            else Color(0xFFF44336).copy(alpha = 0.3f)
            showFlash = true
            delay(1000)
            showFlash = false
            flashColor = null
        }
        previousPrice = stockPrice.price
    }

    val animatedFlashColor by animateColorAsState(
        targetValue = if (showFlash && flashColor != null) flashColor!! else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "flashColor"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        AnimatedVisibility(visible = showFlash, modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize().background(animatedFlashColor))
        }

        Card(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stockPrice.symbol,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(16.dp))
                PriceChangeIndicator(
                    isPriceIncreased = stockPrice.isPriceIncreased.takeIf { stockPrice.previousPrice != null },
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "$${String.format("%.2f", stockPrice.price)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(100.dp)
                )
            }
        }
    }
}
