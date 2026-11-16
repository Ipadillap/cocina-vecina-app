package cl.udec.cocinavecina.ui.publish

import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Reglas de publicación que valida la app antes de llamar a la API (el backend vuelve a validarlas).
 * Son funciones puras: no leen reloj ni estado; reciben `now` y las horas como parámetros.
 */
object PublishRules {
    const val MIN_SALE_MIN = 20L // mínimo a la venta
    const val MAX_SINCE_PREP_MIN = 210L // máximo 3 h 30 min desde que se preparó
    const val MAX_PREP_AGO_MIN = 180L // se puede declarar preparado hasta 3 h atrás
    const val TIME_STEP_MIN = 10L
    const val PICKUP_WINDOW_MIN = 30L // ventana de retiro después del término de la venta

    const val MIN_PRICE = 500
    const val MAX_PRICE = 20_000
    const val MAX_PORTIONS = 50

    /** El mínimo aceptado no puede bajar del 50 % del precio base, en múltiplos de $100. */
    fun lowestAcceptedPrice(basePrice: Int): Int = ((basePrice + 199) / 200) * 100

    fun maxSaleEnd(prepared: Instant): Instant = prepared.plus(MAX_SINCE_PREP_MIN, ChronoUnit.MINUTES)

    /** Hora de preparación permitida: entre 3 h atrás y ahora. */
    fun clampPrepared(
        prepared: Instant,
        now: Instant,
    ): Instant = prepared.coerceIn(now.minus(MAX_PREP_AGO_MIN, ChronoUnit.MINUTES), now)

    /** Fin de la venta permitido: al menos [MIN_SALE_MIN] desde ahora y como máximo [maxSaleEnd]. */
    fun clampSaleEnd(
        end: Instant,
        prepared: Instant,
        now: Instant,
    ): Instant = end.coerceAtLeast(now.plus(MIN_SALE_MIN, ChronoUnit.MINUTES)).coerceAtMost(maxSaleEnd(prepared))

    fun pickupEnd(saleEnd: Instant): Instant = saleEnd.plus(PICKUP_WINDOW_MIN, ChronoUnit.MINUTES)
}
