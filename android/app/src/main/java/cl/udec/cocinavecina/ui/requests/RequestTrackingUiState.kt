package cl.udec.cocinavecina.ui.requests

import cl.udec.cocinavecina.data.network.OfferView
import cl.udec.cocinavecina.data.network.PurchaseRequestView
import cl.udec.cocinavecina.ui.common.NegotiationRules
import java.time.Duration
import java.time.Instant

enum class RequestAction { ACCEPT, REJECT }

data class RequestTrackingUiState(
    val request: PurchaseRequestView? = null,
    val offer: OfferView? = null,
    val now: Instant = Instant.now(),
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val pendingAction: RequestAction? = null,
    val actionError: String? = null,
    val isRejectConfirmVisible: Boolean = false,
    val openOrderId: Long? = null,
) {
    val sellerName: String
        get() = offer?.seller?.trim()?.substringBefore(' ')?.ifEmpty { null } ?: "Quien vende"

    val minutesLeft: Long
        get() = offer?.let { Duration.between(now, Instant.parse(it.expiresAt)).toMinutes().coerceAtLeast(0) } ?: 0

    val isOpen: Boolean
        get() = request?.turn != null

    val isExpired: Boolean
        get() = isOpen && offer != null && minutesLeft == 0L

    val isBuyerTurn: Boolean
        get() = request?.turn == NegotiationRules.BUYER && !isExpired

    val isWaitingSeller: Boolean
        get() = request?.turn == NegotiationRules.SELLER && !isExpired

    val hasSellerCounteroffer: Boolean
        get() = isBuyerTurn && request?.currentParty == NegotiationRules.SELLER

    val canProposeAgain: Boolean
        get() = isBuyerTurn && (request?.rounds ?: NegotiationRules.MAX_ROUNDS) < NegotiationRules.MAX_ROUNDS

    val isActing: Boolean
        get() = pendingAction != null

    val total: Int
        get() = request?.let { it.currentPrice * it.portions } ?: 0
}
