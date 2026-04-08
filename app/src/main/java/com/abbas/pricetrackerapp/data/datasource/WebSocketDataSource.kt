package com.abbas.pricetrackerapp.data.datasource

import com.abbas.pricetrackerapp.data.model.PriceUpdate
import kotlinx.coroutines.flow.Flow

interface WebSocketDataSource {
    fun connect(): Flow<Boolean>
    suspend fun disconnect()
    suspend fun sendPriceUpdate(priceUpdate: PriceUpdate)
    fun observePriceUpdates(): Flow<PriceUpdate>
    fun observeConnectionState(): Flow<Boolean>
}
