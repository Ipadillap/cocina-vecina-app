package cl.udec.cocinavecina.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat

data class Coordinate(val lat: Double, val lon: Double)

/**
 * T2: permiso y lectura de ubicación (GPS).
 *
 * - [lastKnown] es lo que usa el comprador al buscar: se redondea a 100 m antes de salir de este objeto
 *   y nunca se guarda (ni en disco ni en la sesión): solo viaja como parámetro de la búsqueda.
 * - [lastKnownExact] es solo para que el vendedor fije su punto de retiro; el servidor lo reduce a la celda de 400 m.
 */
object Locator {
    val CONCEPCION = Coordinate(-36.8270, -73.0503)

    fun hasPermission(context: Context) =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    fun lastKnown(context: Context): Coordinate = Grid.roundTo100m(lastKnownExact(context))

    @SuppressLint("MissingPermission")
    fun lastKnownExact(context: Context): Coordinate {
        if (!hasPermission(context)) return CONCEPCION
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val best =
            listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.PASSIVE_PROVIDER)
                .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
                .maxByOrNull { it.time }
        return best?.let { Coordinate(it.latitude, it.longitude) } ?: CONCEPCION
    }
}
