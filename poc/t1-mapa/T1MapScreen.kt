package cl.udec.poc.t1

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView

private const val STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"

@Composable
fun T1MapScreen() {
    var status by remember { mutableStateOf("T1: Cargando mapa OpenFreeMap...") }

    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("T1: Mapa OpenFreeMap (sin API key)", style = MaterialTheme.typography.titleMedium)
        Text(status, style = MaterialTheme.typography.bodySmall)
        AndroidView(
            modifier = Modifier.fillMaxWidth().weight(1f),
            factory = { ctx ->
                MapView(ctx).apply {
                    onCreate(null); onStart(); onResume()
                    getMapAsync { map ->
                        map.cameraPosition = CameraPosition.Builder().target(LatLng(-36.827, -73.045)).zoom(13.5).build()
                        map.setStyle(STYLE_URL) {
                            status = "T1 OK: Mapa cargado correctamente desde OpenFreeMap"
                        }
                    }
                }
            }
        )
    }
}
