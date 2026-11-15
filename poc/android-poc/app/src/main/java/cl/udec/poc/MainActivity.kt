package cl.udec.poc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.udec.poc.t1.T1MapScreen
import cl.udec.poc.t2.T2GpsScreen
import cl.udec.poc.t3.T3PhotosScreen
import cl.udec.poc.t4.T4GeocodingScreen
import cl.udec.poc.t5.T5CellsScreen
import org.maplibre.android.MapLibre

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(this)
        setContent {
            MaterialTheme {
                var selected by remember { mutableIntStateOf(0) }
                Column(Modifier.fillMaxSize()) {
                    if (selected != 0) {
                        TextButton(onClick = { selected = 0 }, modifier = Modifier.padding(4.dp)) {
                            Text("<- Volver al menú T1–T5")
                        }
                    }
                    when (selected) {
                        0 -> Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("PoC CocinaVecina — Módulos T1 a T5", style = MaterialTheme.typography.titleMedium)
                            Button(onClick = { selected = 1 }, modifier = Modifier.fillMaxWidth()) { Text("T1: Mapa OpenFreeMap") }
                            Button(onClick = { selected = 2 }, modifier = Modifier.fillMaxWidth()) { Text("T2: GPS redondeado a 100 m") }
                            Button(onClick = { selected = 3 }, modifier = Modifier.fillMaxWidth()) { Text("T3: Fotos <= 0,3 MB + Nginx") }
                            Button(onClick = { selected = 4 }, modifier = Modifier.fillMaxWidth()) { Text("T4: Geocoding Nominatim -> Celda 400 m") }
                            Button(onClick = { selected = 5 }, modifier = Modifier.fillMaxWidth()) { Text("T5: Ofertas en celdas de 400 m") }
                        }
                        1 -> T1MapScreen()
                        2 -> T2GpsScreen()
                        3 -> T3PhotosScreen()
                        4 -> T4GeocodingScreen()
                        5 -> T5CellsScreen()
                    }
                }
            }
        }
    }
}