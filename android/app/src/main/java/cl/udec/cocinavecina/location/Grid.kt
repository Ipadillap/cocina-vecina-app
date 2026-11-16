package cl.udec.cocinavecina.location

import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToLong
import kotlin.math.sqrt

/**
 * Geometría de la grilla de privacidad. Replica `rules/Obfuscation.java` del backend para que el centro de celda
 * que calcula la app coincida con el que devuelve el servidor (celda de 400 m, latitud de referencia de Concepción).
 */
object Grid {
    const val CELL_METERS = 400.0
    const val BUYER_ROUNDING_METERS = 100.0

    private const val METERS_PER_DEGREE_LATITUDE = 111_320.0
    private const val REFERENCE_LATITUDE = -36.827

    /** T6: error máximo al mostrar el centro de la celda en vez del punto real (la media diagonal, ≈ 283 m). */
    val MAX_CELL_ERROR_METERS: Double = CELL_METERS * sqrt(2.0) / 2

    fun cellCenter(point: Coordinate): Coordinate {
        val latitudeStep = CELL_METERS / METERS_PER_DEGREE_LATITUDE
        val longitudeStep = CELL_METERS / (METERS_PER_DEGREE_LATITUDE * cos(Math.toRadians(REFERENCE_LATITUDE)))
        return Coordinate(
            lat = (floor(point.lat / latitudeStep) + 0.5) * latitudeStep,
            lon = (floor(point.lon / longitudeStep) + 0.5) * longitudeStep,
        )
    }

    /** T2: la posición del comprador se redondea a una grilla de 100 m antes de usarla. */
    fun roundTo100m(point: Coordinate): Coordinate {
        val latitudeStep = BUYER_ROUNDING_METERS / METERS_PER_DEGREE_LATITUDE
        val longitudeStep = BUYER_ROUNDING_METERS / (METERS_PER_DEGREE_LATITUDE * cos(Math.toRadians(point.lat)))
        return Coordinate(
            lat = (point.lat / latitudeStep).roundToLong() * latitudeStep,
            lon = (point.lon / longitudeStep).roundToLong() * longitudeStep,
        )
    }
}
