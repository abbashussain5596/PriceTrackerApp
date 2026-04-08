package com.abbas.pricetrackerapp.data.datasource

import com.abbas.pricetrackerapp.data.model.PriceUpdate
import com.abbas.pricetrackerapp.data.model.WebSocketMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.concurrent.TimeUnit

sealed class WebSocketEvent {
    data class ConnectionChanged(val isConnected: Boolean) : WebSocketEvent()
    data class PriceUpdateReceived(val priceUpdate: PriceUpdate) : WebSocketEvent()
}

class WebSocketDataSourceImpl : WebSocketDataSource {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val request = Request.Builder()
        .url("wss://ws.postman-echo.com/raw")
        .build()

    private var webSocket: WebSocket? = null
    private val json = Json { ignoreUnknownKeys = true }

    // Single channel aggregates both connection and price events
    private val eventsChannel = Channel<WebSocketEvent>(Channel.UNLIMITED)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // SharedFlow broadcasts events to multiple independent collectors
    private val events: SharedFlow<WebSocketEvent> = eventsChannel
        .receiveAsFlow()
        .shareIn(scope, SharingStarted.Eagerly, replay = 0)

    override fun connect(): Flow<Boolean> {
        if (webSocket == null) {
            webSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    eventsChannel.trySend(WebSocketEvent.ConnectionChanged(true))
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        val message = json.decodeFromString<WebSocketMessage>(text)
                        eventsChannel.trySend(
                            WebSocketEvent.PriceUpdateReceived(
                                PriceUpdate(message.symbol, message.price, message.timestamp)
                            )
                        )
                    } catch (e: Exception) {
                        // Ignore malformed messages
                    }
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {}

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    eventsChannel.trySend(WebSocketEvent.ConnectionChanged(false))
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    eventsChannel.trySend(WebSocketEvent.ConnectionChanged(false))
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    eventsChannel.trySend(WebSocketEvent.ConnectionChanged(false))
                    reconnect()
                }
            })
        }

        return observeConnectionState()
    }

    override fun observePriceUpdates(): Flow<PriceUpdate> = events
        .filterIsInstance<WebSocketEvent.PriceUpdateReceived>()
        .map { it.priceUpdate }

    override fun observeConnectionState(): Flow<Boolean> = events
        .filterIsInstance<WebSocketEvent.ConnectionChanged>()
        .map { it.isConnected }

    override suspend fun sendPriceUpdate(priceUpdate: PriceUpdate) {
        val message = WebSocketMessage(
            symbol = priceUpdate.symbol,
            price = priceUpdate.price,
            timestamp = priceUpdate.timestamp
        )
        val jsonMessage = json.encodeToString(WebSocketMessage.serializer(), message)
        webSocket?.send(jsonMessage)
    }

    override suspend fun disconnect() {
        cleanupConnection()
    }

    private fun cleanupConnection() {
        webSocket?.close(1000, "Normal closure")
        webSocket = null
    }

    private fun reconnect() {
        // State is already emitted via ConnectionChanged(false) in onFailure
    }
}
