package cl.udec.poc.t5

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.round
import org.json.JSONObject
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource

private const val STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"

private data class DemoT7Data(
    val buyerCenter: LatLng,
    val offerCells: List<Pair<Double, Double>>
)

private fun loadDemoT7(context: Context): DemoT7Data {
    val raw = context.assets.open("demo_data.json").bufferedReader().use { it.readText() }
    val root = JSONObject(raw)

    val buyer = root.getJSONObject("buyer_reference")
    val center = LatLng(buyer.getDouble("approxLatitude"), buyer.getDouble("approxLongitude"))

    val offersArr = root.getJSONArray("nearby_offers")
    val cells = (0 until offersArr.length()).map { i ->
        val item = offersArr.getJSONObject(i)
        cellCentroid400m(item.getDouble("approxLatitude"), item.getDouble("approxLongitude"))
    }
    return DemoT7Data(center, cells)
}

@Composable
fun T5CellsScreen() {
    val context = LocalContext.current
    val demo = remember { loadDemoT7(context) }
    var status by remember { mutableStateOf("T5 OK: ${demo.offerCells.size} ofertas de demo_data.json cargadas") }
    var styleRef by remember { mutableStateOf<Style?>(null) }

    val cells = remember {
        mutableStateListOf<Pair<Double, Double>>().apply { addAll(demo.offerCells) }
    }

    fun renderGeoJson(s: Style?) {
        val json = cells.joinToString(",", """{"type":"FeatureCollection","features":[""", "]}") { (lat, lon) ->
            """{"type":"Feature","geometry":{"type":"Point","coordinates":[$lon,$lat]}}"""
        }
        (s?.getSource("cells") as? GeoJsonSource)?.setGeoJson(json)
    }

    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("T5: Ofertas en centroides de celdas (400 m)", style = MaterialTheme.typography.titleMedium)
        Text(status, style = MaterialTheme.typography.bodySmall)
        AndroidView(
            modifier = Modifier.fillMaxWidth().weight(1f),
            factory = { ctx ->
                MapView(ctx).apply {
                    onCreate(null); onStart(); onResume()
                    getMapAsync { map ->
                        map.cameraPosition = CameraPosition.Builder().target(demo.buyerCenter).zoom(13.5).build()
                        map.setStyle(STYLE_URL) { s ->
                            s.addSource(GeoJsonSource("cells"))
                            s.addLayer(CircleLayer("cells-layer", "cells").withProperties(
                                PropertyFactory.circleRadius(10f),
                                PropertyFactory.circleColor("#C62F1A"),
                                PropertyFactory.circleStrokeWidth(2f),
                                PropertyFactory.circleStrokeColor("#FFFFFF"),
                            ))
                            styleRef = s
                            renderGeoJson(s)
                        }
                        map.addOnMapClickListener { latLng ->
                            val centroid = cellCentroid400m(latLng.latitude, latLng.longitude)
                            cells.add(centroid)
                            renderGeoJson(styleRef)
                            status = "T5 OK: Nueva celda 400 m fijada en (${centroid.first}, ${centroid.second})"
                            true
                        }
                    }
                }
            }
        )
    }
}

private fun cellCentroid400m(lat: Double, lon: Double): Pair<Double, Double> {
    val mLat = 111_320.0
    val mLon = mLat * cos(Math.toRadians(lat))
    val cy = (floor((lat * mLat) / 400.0) + 0.5) * 400.0
    val cx = (floor((lon * mLon) / 400.0) + 0.5) * 400.0
    return (round((cy / mLat) * 1e6) / 1e6) to (round((cx / mLon) * 1e6) / 1e6)
}