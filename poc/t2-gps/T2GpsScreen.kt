package cl.udec.poc.t2

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlin.math.round

@Composable
fun T2GpsScreen() {
    val ctx = LocalContext.current
    var approxCoord by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var status by remember { mutableStateOf("T2: Esperando lectura puntual de GPS (no se persiste)") }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val raw = readGpsOnce(ctx)
            val rounded = round100m(raw.first) to round100m(raw.second)
            approxCoord = rounded
            status = "T2 OK: Coordenada redondeada a ~100 m = (${rounded.first}, ${rounded.second})"
        } else {
            status = "T2: Permiso denegado (se usarÃ­a centro de ConcepciÃ³n por defecto)"
        }
    }

    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("T2: GPS puntual redondeado a 100 m", style = MaterialTheme.typography.titleMedium)
        Text(status, style = MaterialTheme.typography.bodyMedium)
        Button(onClick = { launcher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) }) {
            Text("Solicitar permiso y leer GPS una vez")
        }
        approxCoord?.let {
            Text("Dato volÃ¡til en RAM (sin guardar en disco/BD): ${it.first}, ${it.second}")
        }
    }
}

private fun round100m(deg: Double): Double = round(deg * 1000.0) / 1000.0

@SuppressLint("MissingPermission")
private fun readGpsOnce(ctx: Context): Pair<Double, Double> {
    val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val loc = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
        .maxByOrNull { it.time }
    return if (loc != null) loc.latitude to loc.longitude else -36.8270 to -73.0503
}
