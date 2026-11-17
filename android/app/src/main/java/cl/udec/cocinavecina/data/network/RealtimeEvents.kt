package cl.udec.cocinavecina.data.network

import cl.udec.cocinavecina.BuildConfig
import cl.udec.cocinavecina.data.session.SessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

private const val RETRY_MILLIS = 5_000L

/** [client] debe ser el cliente autenticado: agrega el token y lo renueva si el servidor responde 401. */
class RealtimeEvents(
    private val client: OkHttpClient,
    private val session: SessionStore,
    private val json: Json,
) {
    private val flow = MutableSharedFlow<UserEvent>(extraBufferCapacity = 32)
    val events: SharedFlow<UserEvent> = flow

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var socket: WebSocket? = null

    @Volatile
    private var wanted = false
    private var retry: Job? = null

    @Synchronized
    fun connect() {
        wanted = true
        if (socket != null || session.currentToken == null) return
        val url = BuildConfig.API_URL.replaceFirst("http", "ws") + "/v1/events"
        val request = Request.Builder().url(url).build()
        socket =
            client.newWebSocket(
                request,
                object : WebSocketListener() {
                    override fun onMessage(
                        webSocket: WebSocket,
                        text: String,
                    ) {
                        runCatching { json.decodeFromString(UserEvent.serializer(), text) }
                            .onSuccess { flow.tryEmit(it) }
                    }

                    override fun onFailure(
                        webSocket: WebSocket,
                        t: Throwable,
                        response: Response?,
                    ) {
                        dropped(webSocket)
                    }

                    override fun onClosed(
                        webSocket: WebSocket,
                        code: Int,
                        reason: String,
                    ) {
                        dropped(webSocket)
                    }
                },
            )
    }

    @Synchronized
    fun disconnect() {
        wanted = false
        retry?.cancel()
        socket?.close(1000, null)
        socket = null
    }

    @Synchronized
    private fun dropped(closed: WebSocket) {
        if (socket !== closed) return
        socket = null
        if (!wanted) return
        retry?.cancel()
        retry =
            scope.launch {
                delay(RETRY_MILLIS)
                connect()
            }
    }
}
