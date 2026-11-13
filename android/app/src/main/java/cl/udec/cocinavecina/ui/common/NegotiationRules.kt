package cl.udec.cocinavecina.ui.common

import kotlin.math.roundToInt

object NegotiationRules {
    const val MAX_ROUNDS = 3
    const val PRICE_STEP = 100

    fun roundToStep(price: Double): Int = (price / PRICE_STEP).roundToInt() * PRICE_STEP

    fun isValidStep(price: Int): Boolean = price % PRICE_STEP == 0
}
