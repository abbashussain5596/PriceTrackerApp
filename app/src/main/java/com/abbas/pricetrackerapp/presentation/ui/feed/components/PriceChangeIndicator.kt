package com.abbas.pricetrackerapp.presentation.ui.feed.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PriceChangeIndicator(
    isPriceIncreased: Boolean?,
    modifier: Modifier = Modifier
) {
    when (isPriceIncreased) {
        true -> Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
            Text(text = "↑", color = Color(0xFF4CAF50), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        false -> Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
            Text(text = "↓", color = Color(0xFFF44336), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        null -> Spacer(modifier = Modifier.width(16.dp))
    }
}
