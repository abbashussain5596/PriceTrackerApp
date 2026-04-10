package com.abbas.pricetrackerapp.presentation.ui.feed.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.abbas.pricetrackerapp.presentation.state.ConnectionState

@Composable
fun ConnectionStatusIndicator(
    connectionState: ConnectionState,
    modifier: Modifier = Modifier
) {
    val color = when (connectionState) {
        ConnectionState.CONNECTED -> Color(0xFF4CAF50)
        ConnectionState.DISCONNECTED -> Color(0xFFF44336)
        ConnectionState.CONNECTING -> Color(0xFFFF9800)
    }

    Box(
        modifier = modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(color)
    )
}
