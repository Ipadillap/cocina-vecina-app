package cl.udec.cocinavecina.ui.orders

import cl.udec.cocinavecina.data.network.OfferView
import cl.udec.cocinavecina.data.network.OrderView
import cl.udec.cocinavecina.data.network.PickupDetailsView
import java.time.Duration
import java.time.Instant

const val ACCEPTED_STATUS = "ACCEPTED"
const val READY_STATUS = "READY_FOR_PICKUP"
const val DELIVERED_STATUS = "DELIVERED"
const val PICKUP_VISIBLE_AFTER_DELIVERY_MINUTES = 60L
const val DELIVERY_CODE_LENGTH = 6

enum class OrderStep(val status: String, val label: String) {
    ACCEPTED(ACCEPTED_STATUS, "Aceptado"),
    READY(READY_STATUS, "Listo para retiro"),
    DELIVERED(DELIVERED_STATUS, "Entregado"),
}

enum class OrderAction { MARK_READY, VALIDATE, CANCEL }

data class OrderDetailUiState(
    val userId: Long,
    val order: OrderView? = null,
    val offer: OfferView? = null,
    val code: String? = null,
    val codeInput: String = "",
    val pickup: PickupDetailsView? = null,
    val now: Instant = Instant.now(),
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val pendingAction: OrderAction? = null,
    val actionError: String? = null,
    val isCancelDialogVisible: Boolean = false,
) {
    val isBuyer: Boolean
        get() = order?.buyerId == userId

    val isInProgress: Boolean
        get() = order?.status == ACCEPTED_STATUS || order?.status == READY_STATUS

    val currentStep: Int
        get() = OrderStep.entries.indexOfFirst { it.status == order?.status }

    val total: Int
        get() = order?.let { it.agreedPrice * it.portions } ?: 0

    val minutesToPickupDeadline: Long
        get() = order?.let { Duration.between(now, Instant.parse(it.windowEnd)).toMinutes().coerceAtLeast(0) } ?: 0

    val isPickupUnlocked: Boolean
        get() {
            val order = order ?: return false
            if (isInProgress) return true
            val deliveredAt = order.deliveredAt?.let(Instant::parse) ?: return false
            return order.status == DELIVERED_STATUS && now.isBefore(deliveredAt.plusSeconds(PICKUP_VISIBLE_AFTER_DELIVERY_MINUTES * 60))
        }

    val sellerName: String
        get() = offer?.seller?.trim()?.substringBefore(' ')?.takeIf { it.isNotEmpty() } ?: "quien vende"

    val canCancel: Boolean
        get() = order?.status == ACCEPTED_STATUS && pendingAction == null

    val isActing: Boolean
        get() = pendingAction != null

    val canValidate: Boolean
        get() = codeInput.length == DELIVERY_CODE_LENGTH && order?.status == READY_STATUS && !isActing
}
