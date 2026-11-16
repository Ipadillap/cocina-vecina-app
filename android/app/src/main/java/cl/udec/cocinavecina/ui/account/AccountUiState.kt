package cl.udec.cocinavecina.ui.account

import cl.udec.cocinavecina.data.network.AccountView
import cl.udec.cocinavecina.data.network.PickupPointView
import cl.udec.cocinavecina.ui.common.Format

const val BUYER_ROLE = "BUYER"
const val SELLER_ROLE = "SELLER"
const val MODERATOR_ROLE = "MODERATOR"

enum class AccountAction { EXPORT, DELETE }

data class AccountUiState(
    val account: AccountView? = null,
    val pickupPoints: List<PickupPointView>? = null,
    val isLocationGranted: Boolean = false,
    val areNotificationsEnabled: Boolean = false,
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val pendingAction: AccountAction? = null,
    val actionError: String? = null,
    val message: String? = null,
    val isExportPickerRequested: Boolean = false,
    val isDeleteDialogVisible: Boolean = false,
    val isTermsVisible: Boolean = false,
    val isAccountDeleted: Boolean = false,
) {
    val isSeller: Boolean
        get() = account?.role == SELLER_ROLE

    val isModerator: Boolean
        get() = account?.role == MODERATOR_ROLE

    val initial: String
        get() = account?.displayName?.trim()?.firstOrNull()?.uppercase() ?: "?"

    val maskedPhone: String?
        get() = account?.phone?.takeIf { it.isNotBlank() }?.let(Format::maskedPhone)

    val modeLabel: String
        get() =
            when (account?.role) {
                SELLER_ROLE -> "Compra y vende"
                MODERATOR_ROLE -> "Moderación"
                else -> "Solo compra"
            }

    val pickupPointsSummary: String?
        get() = pickupPoints?.takeIf { it.isNotEmpty() }?.let { points -> "${points.size} · ${points.joinToString(", ") { it.alias }}" }

    val termsVersionLabel: String?
        get() = account?.termsVersion?.let { "v$it" }

    val termsAcceptedLabel: String?
        get() = account?.termsAcceptedAt?.let { runCatching { Format.date(it) }.getOrNull() }

    val isActing: Boolean
        get() = pendingAction != null
}
