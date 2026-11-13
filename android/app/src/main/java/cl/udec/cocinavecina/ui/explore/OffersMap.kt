package cl.udec.cocinavecina.ui.explore

import android.view.Gravity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import cl.udec.cocinavecina.BuildConfig
import cl.udec.cocinavecina.data.network.NearbyOffer
import cl.udec.cocinavecina.location.Coordinate
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import kotlin.math.cos
import kotlin.math.pow

private const val ZONES_SOURCE = "offer-zones"
private const val ZONES_LAYER = "offer-zones-circles"
private const val CENTER_SOURCE = "search-center"
private const val CENTER_LAYER = "search-center-dot"
private const val ZONE_RADIUS_METERS = 200.0
private const val METERS_PER_PIXEL_AT_ZOOM_ZERO = 78271.517
private const val MAX_ZOOM = 22

@Composable
fun OffersMap(
    center: Coordinate,
    zoom: Double,
    offers: List<NearbyOffer>,
    onOfferClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
) {
    if (LocalInspectionMode.current) {
        Box(modifier.background(MaterialTheme.colorScheme.primaryContainer))
        return
    }
    val context = LocalContext.current
    val mapView = remember { MapView(context).apply { onCreate(null) } }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var style by remember { mutableStateOf<Style?>(null) }
    val onClick by rememberUpdatedState(onOfferClick)
    val zoneColor = MaterialTheme.colorScheme.secondary.toArgb()
    val centerColor = MaterialTheme.colorScheme.primary.toArgb()
    val centerStrokeColor = MaterialTheme.colorScheme.onPrimary.toArgb()

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer =
            LifecycleEventObserver { _, event ->
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
                view.getMapAsync { loadedMap ->
                    loadedMap.uiSettings.isLogoEnabled = false
                    loadedMap.uiSettings.attributionGravity = Gravity.BOTTOM or Gravity.END
                    loadedMap.uiSettings.setAllGesturesEnabled(interactive)
                    loadedMap.cameraPosition =
                        CameraPosition.Builder()
                            .target(LatLng(center.lat, center.lon))
                            .zoom(zoom)
                            .build()
                    loadedMap.setStyle(BuildConfig.MAP_STYLE) { loadedStyle ->
                        loadedStyle.addSource(GeoJsonSource(ZONES_SOURCE, offersGeoJson(offers)))
                        loadedStyle.addSource(GeoJsonSource(CENTER_SOURCE, pointGeoJson(center)))
                        loadedStyle.addLayer(
                            CircleLayer(ZONES_LAYER, ZONES_SOURCE).withProperties(
                                PropertyFactory.circleRadius(zoneRadius(center.lat)),
                                PropertyFactory.circleColor(zoneColor),
                                PropertyFactory.circleOpacity(0.18f),
                                PropertyFactory.circleStrokeColor(zoneColor),
                                PropertyFactory.circleStrokeWidth(1.5f),
                                PropertyFactory.circleStrokeOpacity(0.8f),
                            ),
                        )
                        loadedStyle.addLayer(
                            CircleLayer(CENTER_LAYER, CENTER_SOURCE).withProperties(
                                PropertyFactory.circleRadius(6f),
                                PropertyFactory.circleColor(centerColor),
                                PropertyFactory.circleStrokeColor(centerStrokeColor),
                                PropertyFactory.circleStrokeWidth(2f),
                            ),
                        )
                        style = loadedStyle
                    }
                    if (interactive) {
                        loadedMap.addOnMapClickListener { point ->
                            val screenPoint = loadedMap.projection.toScreenLocation(point)
                            val touched = loadedMap.queryRenderedFeatures(screenPoint, ZONES_LAYER)
                            touched.firstOrNull()?.getNumberProperty("id")?.toLong()?.let { onClick(it) }
                            touched.isNotEmpty()
                        }
                    }
                    map = loadedMap
                }
            }
        },
        modifier = modifier,
    )

    LaunchedEffect(map, center, zoom) {
        map?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(center.lat, center.lon), zoom))
    }

    LaunchedEffect(style, offers) {
        (style?.getSource(ZONES_SOURCE) as? GeoJsonSource)?.setGeoJson(offersGeoJson(offers))
    }

    LaunchedEffect(style, center) {
        (style?.getSource(CENTER_SOURCE) as? GeoJsonSource)?.setGeoJson(pointGeoJson(center))
    }
}

private fun zoneRadius(latitude: Double): Expression {
    val radiusAtZoomZero = ZONE_RADIUS_METERS / (METERS_PER_PIXEL_AT_ZOOM_ZERO * cos(Math.toRadians(latitude)))
    return Expression.interpolate(
        Expression.exponential(2),
        Expression.zoom(),
        Expression.stop(0, radiusAtZoomZero),
        Expression.stop(MAX_ZOOM, radiusAtZoomZero * 2.0.pow(MAX_ZOOM)),
    )
}

private fun pointGeoJson(coordinate: Coordinate): String =
    """{"type":"Feature","geometry":{"type":"Point","coordinates":[${coordinate.lon},${coordinate.lat}]},"properties":{}}"""

private fun offersGeoJson(offers: List<NearbyOffer>): String =
    offers.joinToString(
        prefix = """{"type":"FeatureCollection","features":[""",
        postfix = "]}",
    ) { offer ->
        """{"type":"Feature","geometry":{"type":"Point","coordinates":[${offer.approxLongitude},${offer.approxLatitude}]},""" +
            """"properties":{"id":${offer.id}}}"""
    }
