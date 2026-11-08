package cl.udec.cocinavecina.data.network

import cl.udec.cocinavecina.BuildConfig
import cl.udec.cocinavecina.data.session.SessionStore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class RealtimeEvents(
    private val client: OkHttpClient,
    private val session: SessionStore,
    private val json: Json,
) {
    private val flow = MutableSharedFlow<UserEvent>(extraBufferCapacity = 32)
    val events: SharedFlow<UserEvent> = flow

    @Volatile
    private var socket: WebSocket? = null

    fun connect() {
        val token = session.currentToken ?: return
        if (socket != null) return
        val url = BuildConfig.API_URL.replaceFirst("http", "ws") + "/v1/events"
        val request = Request.Builder().url(url).header("Authorization", "Bearer $token").build()
        socket = client.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onMessage(webSocket: WebSocket, text: String) {
                    runCatching { json.decodeFromString(UserEvent.serializer(), text) }
                        .onSuccess { flow.tryEmit(it) }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    socket = null
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    socket = null
                }
            },
        )
    }

    fun disconnect() {
        socket?.close(1000, null)
        socket = null
    }
}
