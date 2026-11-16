package cl.udec.cocinavecina.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class GridTest {
    private val somewhereInConcepcion = Coordinate(-36.8268, -73.0497)

    private fun meters(
        a: Coordinate,
        b: Coordinate,
    ): Double {
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLon = Math.toRadians(b.lon - a.lon)
        val h = sin(dLat / 2) * sin(dLat / 2) + cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * sin(dLon / 2) * sin(dLon / 2)
        return 6_371_000.0 * 2 * atan2(sqrt(h), sqrt(1 - h))
    }

    @Test
    fun maxCellErrorIsHalfTheDiagonal() {
        assertEquals(282.8, Grid.MAX_CELL_ERROR_METERS, 0.1)
    }

    @Test
    fun cellCenterStaysWithinTheMaximumError() {
        val center = Grid.cellCenter(somewhereInConcepcion)
        assertTrue(meters(somewhereInConcepcion, center) <= Grid.MAX_CELL_ERROR_METERS + 5)
    }

    @Test
    fun pointsInTheSameCellShareTheSameCenter() {
        val near = Coordinate(somewhereInConcepcion.lat + 0.00001, somewhereInConcepcion.lon + 0.00001)
        assertEquals(Grid.cellCenter(somewhereInConcepcion), Grid.cellCenter(near))
    }

    @Test
    fun buyerPositionIsRoundedToAbout100Meters() {
        val rounded = Grid.roundTo100m(somewhereInConcepcion)
        assertTrue(meters(somewhereInConcepcion, rounded) <= 71)
    }
}
