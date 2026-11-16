package cl.udec.cocinavecina.ui.common

import cl.udec.cocinavecina.ui.orders.QrCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class FormatTest {
    @Test
    fun amountsInChileanPesosWithoutDecimals() {
        assertEquals("$3.500", Format.pesos(3500))
    }

    @Test
    fun distancesInMetersAndKilometers() {
        assertEquals("800 m", Format.distance(800))
        assertEquals("1,5 km", Format.distance(1500))
    }

    @Test
    fun remainingTimeIsNeverNegative() {
        val now = Instant.parse("2026-11-10T16:00:00Z")
        assertEquals(90, Format.minutesLeft("2026-11-10T17:30:00Z", now))
        assertEquals(0, Format.minutesLeft("2026-11-10T15:00:00Z", now))
    }

    @Test
    fun enumValuesAreShownInSpanish() {
        assertEquals("Listo para retiro", Format.orderStatus("READY_FOR_PICKUP"))
        assertEquals("Aceptada", Format.requestStatus("ACCEPTED"))
        assertEquals("Frutos secos", Format.allergen("TREE_NUTS"))
        assertEquals("Fraude", Format.reportReason("FRAUD"))
        assertEquals("Some value", Format.orderStatus("SOME_VALUE"))
    }

    @Test
    fun qrOnlyWorksForItsOrder() {
        assertEquals("123456", QrCode.read(QrCode.text(7, "123456"), 7))
        assertNull(QrCode.read(QrCode.text(7, "123456"), 8))
        assertNull(QrCode.read("otra cosa", 7))
    }
}
