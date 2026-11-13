package cl.udec.cocinavecina.ui.common

import java.text.NumberFormat
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object Format {
    private val chile = Locale.forLanguageTag("es-CL")
    private val clock = DateTimeFormatter.ofPattern("HH:mm", chile)
    private val date = DateTimeFormatter.ofPattern("dd-MM-yyyy", chile)

    val CATEGORIES =
        listOf(
            "MAIN_COURSE" to "Plato de fondo",
            "EMPANADAS" to "Empanadas",
            "SOUPS_AND_CREAMS" to "Sopas y cremas",
            "SALADS" to "Ensaladas",
            "PASTRY" to "Repostería",
            "HOMEMADE_BREAD" to "Pan amasado",
            "OTHER" to "Otro",
        )

    val ALLERGENS =
        listOf(
            "GLUTEN" to "Gluten", "MILK" to "Leche", "EGG" to "Huevo", "TREE_NUTS" to "Frutos secos", "PEANUT" to "Maní",
            "SOY" to "Soya", "FISH" to "Pescado", "SHELLFISH" to "Mariscos", "SESAME" to "Sésamo",
        )

    val REPORT_REASONS =
        listOf(
            "HEALTH_RISK" to "Riesgo sanitario",
            "FORBIDDEN_CATEGORY" to "Categoría no permitida",
            "INAPPROPRIATE_CONTENT" to "Contenido inapropiado",
            "FRAUD" to "Fraude",
            "OTHER" to "Otro",
        )

    private val ORDER_STATUSES =
        mapOf(
            "ACCEPTED" to "Aceptado",
            "READY_FOR_PICKUP" to "Listo para retiro",
            "DELIVERED" to "Entregado",
            "CANCELLED" to "Cancelado",
            "NOT_PICKED_UP" to "No retirado",
        )

    private val REQUEST_STATUSES =
        mapOf(
            "OFFER_SENT" to "Oferta enviada",
            "COUNTEROFFER" to "Contraoferta",
            "ACCEPTED" to "Aceptada",
            "REJECTED" to "Rechazada",
            "CANCELLED" to "Cancelada",
            "EXPIRED" to "Expirada",
        )

    private val ALLERGEN_LABELS = ALLERGENS.toMap() + ("NONE_KNOWN" to "Ninguno conocido")
    private val REASON_LABELS = REPORT_REASONS.toMap()

    fun pesos(amount: Int): String = "$" + NumberFormat.getIntegerInstance(chile).format(amount)

    fun distance(meters: Long): String = if (meters < 1000) "$meters m" else String.format(chile, "%.1f km", meters / 1000.0)

    fun minutesLeft(
        isoUtc: String,
        now: Instant = Instant.now(),
    ): Long = Duration.between(now, Instant.parse(isoUtc)).toMinutes().coerceAtLeast(0)

    fun timeLeft(
        isoUtc: String,
        now: Instant = Instant.now(),
    ): String = "quedan ${duration(minutesLeft(isoUtc, now))}"

    fun duration(minutes: Long): String = if (minutes >= 60) "${minutes / 60} h ${minutes % 60} min" else "$minutes min"

    fun clockTime(
        isoUtc: String,
        zone: ZoneId = ZoneId.systemDefault(),
    ): String = clock.format(Instant.parse(isoUtc).atZone(zone))

    fun date(
        isoUtc: String,
        zone: ZoneId = ZoneId.systemDefault(),
    ): String = date.format(Instant.parse(isoUtc).atZone(zone))

    fun maskedPhone(phone: String): String {
        val digits = phone.filter(Char::isDigit)
        if (digits.length < 4) return "••••"
        val last = digits.takeLast(4)
        return if (digits.length == 11 && digits.startsWith("569")) "+56 9 •••• $last" else "•••• $last"
    }

    fun orderStatus(value: String): String = ORDER_STATUSES[value] ?: label(value)

    fun requestStatus(value: String): String = REQUEST_STATUSES[value] ?: label(value)

    fun allergen(value: String): String = ALLERGEN_LABELS[value] ?: label(value)

    fun reportReason(value: String): String = REASON_LABELS[value] ?: label(value)

    private fun label(value: String): String = value.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
}
