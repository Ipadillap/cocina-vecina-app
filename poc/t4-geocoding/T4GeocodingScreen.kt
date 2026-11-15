package cl.udec.poc.t4

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.round
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

private val cache = mutableMapOf<String, Pair<Double, Double>>()
private var lastRequestMs = 0L

@Composable
fun T4GeocodingScreen() {
    val scope = rememberCoroutineScope()
    var address by remember { mutableStateOf("") }
    var resultCell by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var status by remember { mutableStateOf("T4: Ingresa una calle de Concepcion (max 1 req/s, con cache)") }

    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("T4: Geocodificacion Nominatim -> Celda 400 m", style = MaterialTheme.typography.titleMedium)
        Text(status, style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Direccion referencial (se descarta tras calcular)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = {
            scope.launch {
                val start = System.currentTimeMillis()
                val cell = geocodeToCell400m(address)
                val elapsed = System.currentTimeMillis() - start
                if (cell != null) {
                    resultCell = cell
                    address = ""
                    status = "T4 OK (${elapsed} ms): Centroide 400 m = (${cell.first}, ${cell.second})"
                } else {
                    status = "T4: Sin coincidencias en Concepcion"
                }
            }
        }) {
            Text("Obtener celda de 400 m")
        }
        resultCell?.let {
            Text("Solo este centroide se enviaria al backend: (${it.first}, ${it.second})")
        }
    }
}

private suspend fun geocodeToCell400m(raw: String): Pair<Double, Double>? = withContext(Dispatchers.IO) {
    val key = raw.trim().lowercase()
    if (key.isEmpty()) return@withContext null
    cache[key]?.let { return@withContext it }

    val wait = 1000L - (System.currentTimeMillis() - lastRequestMs)
    if (wait > 0) delay(wait)
    lastRequestMs = System.currentTimeMillis()

    val q = URLEncoder.encode("$raw, Concepcion, Chile", "UTF-8")
    val conn = (URL("https://nominatim.openstreetmap.org/search?q=$q&format=json&limit=1").openConnection() as HttpURLConnection).apply {
        setRequestProperty("User-Agent", "CocinaVecina-PoC-UdeC/1.0")
    }
    val arr = JSONArray(conn.inputStream.bufferedReader().readText())
    if (arr.length() == 0) return@withContext null
    val obj = arr.getJSONObject(0)
    cellCentroid400m(obj.getDouble("lat"), obj.getDouble("lon")).also { cache[key] = it }
}

private fun cellCentroid400m(lat: Double, lon: Double): Pair<Double, Double> {
    val mLat = 111_320.0
    val mLon = mLat * cos(Math.toRadians(lat))
    val cy = (floor((lat * mLat) / 400.0) + 0.5) * 400.0
    val cx = (floor((lon * mLon) / 400.0) + 0.5) * 400.0
    return (round((cy / mLat) * 1e6) / 1e6) to (round((cx / mLon) * 1e6) / 1e6)
}