package cl.udec.cocinavecina.location

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray

/**
 * T4: geocodificación con Nominatim (≤ 1 consulta por segundo y con caché), reutilizada del PoC `poc/t4-geocoding`.
 *
 * Solo sirve para ubicar el punto de retiro del vendedor: el resultado se usa para obtener la celda de 400 m y la
 * dirección nunca se muestra a los compradores. Si no hay coincidencia o no hay red, devuelve null y la app envía
 * solo la dirección (el backend también geocodifica con el mismo límite).
 */
object AddressGeocoder {
    private const val MIN_INTERVAL_MS = 1_000L
    private const val ENDPOINT = "https://nominatim.openstreetmap.org/search"
    private const val USER_AGENT = "CocinaVecina/0.1 (UdeC)"

    private val cache = mutableMapOf<String, Coordinate>()
    private val turn = Mutex()
    private var lastRequestMs = 0L

    suspend fun locate(address: String): Coordinate? {
        val query = address.trim()
        if (query.isEmpty()) return null
        val key = query.lowercase()
        return turn.withLock {
            cache[key] ?: fetch(query)?.also { cache[key] = it }
        }
    }

    private suspend fun fetch(query: String): Coordinate? {
        val wait = MIN_INTERVAL_MS - (System.currentTimeMillis() - lastRequestMs)
        if (wait > 0) delay(wait)
        lastRequestMs = System.currentTimeMillis()
        return withContext(Dispatchers.IO) {
            try {
                val q = URLEncoder.encode("$query, Concepción, Chile", "UTF-8")
                val connection =
                    (URL("$ENDPOINT?q=$q&format=jsonv2&limit=1&countrycodes=cl").openConnection() as HttpURLConnection).apply {
                        connectTimeout = 8_000
                        readTimeout = 8_000
                        setRequestProperty("User-Agent", USER_AGENT)
                    }
                try {
                    val results = JSONArray(connection.inputStream.bufferedReader().readText())
                    if (results.length() == 0) {
                        null
                    } else {
                        results.getJSONObject(0).let { Coordinate(it.getDouble("lat"), it.getDouble("lon")) }
                    }
                } finally {
                    connection.disconnect()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
        }
    }
}
