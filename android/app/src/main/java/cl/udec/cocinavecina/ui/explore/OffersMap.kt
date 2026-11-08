package cl.udec.cocinavecina.ui.explore

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import cl.udec.cocinavecina.BuildConfig
import cl.udec.cocinavecina.data.network.NearbyOffer
import cl.udec.cocinavecina.location.Coordinate
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource

private const val SOURCE_ID = "offers"
private const val LAYER_ID = "offers-circles"

@Composable
fun OffersMap(
    center: Coordinate,
    offers: List<NearbyOffer>,
    onOfferClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val mapView = remember { MapView(context).apply { onCreate(null) } }
    var style by remember { mutableStateOf<Style?>(null) }
    val onClick by rememberUpdatedState(onOfferClick)

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onPause()
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) mapView.onStop()
            mapView.onDestroy()
        }
    }

    AndroidView(
        factory = {
            mapView.also { view ->
                view.getMapAsync { map ->
                    map.cameraPosition = CameraPosition.Builder()
                        .target(LatLng(center.lat, center.lon))
                        .zoom(14.0)
                        .build()
                    map.setStyle(BuildConfig.MAP_STYLE) { s ->
                        s.addSource(GeoJsonSource(SOURCE_ID, toGeoJson(offers)))
                        s.addLayer(
                            CircleLayer(LAYER_ID, SOURCE_ID).withProperties(
                                PropertyFactory.circleRadius(10f),
                                PropertyFactory.circleColor("#C62F1A"),
                                PropertyFactory.circleStrokeWidth(2f),
                                PropertyFactory.circleStrokeColor("#FFF3E0"),
                            ),
                        )
                        style = s
                    }
                    map.addOnMapClickListener { point ->
                        val screenPoint = map.projection.toScreenLocation(point)
                        val touched = map.queryRenderedFeatures(screenPoint, LAYER_ID)
                        touched.firstOrNull()?.getNumberProperty("id")?.toLong()?.let { onClick(it) }
                        touched.isNotEmpty()
                    }
                }
            }
        },
        modifier = modifier,
    )

    LaunchedEffect(offers, style) {
        (style?.getSource(SOURCE_ID) as? GeoJsonSource)?.setGeoJson(toGeoJson(offers))
    }
}

private fun toGeoJson(offers: List<NearbyOffer>): String = offers.joinToString(
    prefix = """{"type":"FeatureCollection","features":[""",
    postfix = "]}",
) { o ->
    """{"type":"Feature","geometry":{"type":"Point","coordinates":[${o.approxLongitude},${o.approxLatitude}]},""" +
        """"properties":{"id":${o.id}}}"""
}
