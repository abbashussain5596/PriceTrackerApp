package com.abbas.pricetrackerapp.data.model

import kotlinx.serialization.Serializable

@Serializable
data class WebSocketMessage(
    val symbol: String,
    val price: Double,
    val timestamp: Long
)
