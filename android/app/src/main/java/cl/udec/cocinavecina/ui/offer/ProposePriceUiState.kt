package cl.udec.cocinavecina.ui.offer

import cl.udec.cocinavecina.data.network.OfferView
import cl.udec.cocinavecina.ui.common.Format
import cl.udec.cocinavecina.ui.common.NegotiationRules

data class ProposePriceUiState(
    val offer: OfferView? = null,
    val priceText: String = "",
    val portions: Int = 1,
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val isSubmitting: Boolean = false,
    val submitError: String? = null,
    val createdRequestId: Long? = null,
) {
    val round: Int
        get() = 1

    val price: Int?
        get() = priceText.toIntOrNull()

    val maxPortions: Int
        get() = minOf(offer?.availablePortions ?: 0, MAX_PORTIONS_PER_REQUEST)

    val priceError: String?
        get() {
            val offer = offer ?: return null
            val price = price ?: return EMPTY_PRICE
            return when {
                price < offer.minPrice -> "El mínimo de ${offer.seller} es ${Format.pesos(offer.minPrice)}"
                price > offer.basePrice -> "No puede superar el precio publicado (${Format.pesos(offer.basePrice)})"
                !NegotiationRules.isValidStep(price) -> "Usa montos en múltiplos de ${Format.pesos(NegotiationRules.PRICE_STEP)}"
                else -> null
            }
        }

    val savingsPerPortion: Int
        get() {
            val offer = offer ?: return 0
            val price = price ?: return 0
            return (offer.basePrice - price).coerceAtLeast(0)
        }

    val total: Int
        get() = (price ?: 0) * portions

    val publishedTotal: Int
        get() = (offer?.basePrice ?: 0) * portions

    val priceShortcuts: List<Int>
        get() {
            val offer = offer ?: return emptyList()
            val range = offer.basePrice - offer.minPrice
            if (range <= 0) return emptyList()
            return listOf(0.0, 1.0 / 3, 2.0 / 3)
                .map { NegotiationRules.roundToStep(offer.minPrice + range * it) }
                .distinct()
                .filter { it in offer.minPrice until offer.basePrice }
        }

    val canSubmit: Boolean
        get() = offer != null && priceError == null && !isSubmitting && maxPortions > 0

    companion object {
        const val EMPTY_PRICE = "Ingresa un precio por porción"
    }
}
