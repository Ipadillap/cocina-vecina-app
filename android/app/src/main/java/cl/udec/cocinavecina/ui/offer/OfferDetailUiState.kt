package cl.udec.cocinavecina.ui.offer

import cl.udec.cocinavecina.data.network.OfferView
import cl.udec.cocinavecina.ui.common.Format

const val MAX_PORTIONS_PER_REQUEST = 5

data class OfferDetailUiState(
    val offer: OfferView? = null,
    val distanceMeters: Long? = null,
    val portions: Int = 1,
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val isBuying: Boolean = false,
    val buyError: String? = null,
    val createdRequestId: Long? = null,
    val isReportDialogVisible: Boolean = false,
    val isReporting: Boolean = false,
    val reportMessage: String? = null,
) {
    val maxPortions: Int
        get() = minOf(offer?.availablePortions ?: 0, MAX_PORTIONS_PER_REQUEST)

    val total: Int
        get() = (offer?.basePrice ?: 0) * portions

    val isOnSale: Boolean
        get() = offer != null && offer.status == "ACTIVE" && offer.availablePortions > 0 && Format.minutesLeft(offer.expiresAt) > 0
}
