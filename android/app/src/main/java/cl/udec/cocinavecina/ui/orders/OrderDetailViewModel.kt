package cl.udec.cocinavecina.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.udec.cocinavecina.data.ApiError
import cl.udec.cocinavecina.data.network.CodeView
import cl.udec.cocinavecina.data.network.OfferView
import cl.udec.cocinavecina.data.network.OrderView
import cl.udec.cocinavecina.data.network.PickupDetailsView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import kotlin.coroutines.cancellation.CancellationException

class OrderDetailViewModel(
    private val orderId: Long,
    userId: Long,
    private val loadOrder: suspend (orderId: Long) -> Result<OrderView>,
    private val loadOffer: suspend (offerId: Long) -> Result<OfferView>,
    private val readStoredCode: suspend (orderId: Long) -> String?,
    private val issueCode: suspend (orderId: Long) -> Result<CodeView>,
    private val storeCode: suspend (orderId: Long, code: String) -> Unit,
    private val loadPickup: suspend (orderId: Long) -> Result<PickupDetailsView>,
    private val markReady: suspend (orderId: Long) -> Result<OrderView>,
    private val validateDelivery: suspend (orderId: Long, code: String) -> Result<OrderView>,
    private val cancelOrder: suspend (orderId: Long, reason: String) -> Result<OrderView>,
    private val clock: () -> Instant = Instant::now,
) : ViewModel() {
    private val mutableState = MutableStateFlow(OrderDetailUiState(userId = userId, now = clock()))
    val uiState: StateFlow<OrderDetailUiState> = mutableState.asStateFlow()

    init {
        load(showLoading = true)
    }

    fun onRetry() = load(showLoading = true)

    fun refresh() = load(showLoading = false)

    fun onTick() {
        mutableState.update { it.copy(now = clock()) }
        syncBuyerData()
    }

    fun onMarkReadyClick() {
        val current = mutableState.value
        if (current.isBuyer || current.order?.status != ACCEPTED_STATUS || current.isActing) return
        act(OrderAction.MARK_READY) { markReady(orderId) }
    }

    fun onCodeInputChange(value: String) {
        val digits = value.filter(Char::isDigit).take(DELIVERY_CODE_LENGTH)
        mutableState.update { it.copy(codeInput = digits, actionError = null) }
    }

    fun onValidateClick() = onValidateCode(mutableState.value.codeInput)

    fun onValidateCode(code: String) {
        val current = mutableState.value
        val digits = code.filter(Char::isDigit)
        if (current.isBuyer || current.order?.status != READY_STATUS || current.isActing) return
        if (digits.length != DELIVERY_CODE_LENGTH) {
            mutableState.update { it.copy(actionError = INVALID_CODE) }
            return
        }
        mutableState.update { it.copy(codeInput = digits) }
        act(OrderAction.VALIDATE) { validateDelivery(orderId, digits) }
    }

    fun onCancelClick() {
        if (!mutableState.value.canCancel) return
        mutableState.update { it.copy(isCancelDialogVisible = true) }
    }

    fun onCancelDismiss() {
        mutableState.update { it.copy(isCancelDialogVisible = false) }
    }

    fun onCancelConfirm(reason: String) {
        val trimmed = reason.trim().take(MAX_REASON_LENGTH)
        if (trimmed.isEmpty() || !mutableState.value.canCancel) return
        mutableState.update { it.copy(isCancelDialogVisible = false) }
        act(OrderAction.CANCEL) { cancelOrder(orderId, trimmed) }
    }

    private fun act(
        action: OrderAction,
        block: suspend () -> Result<OrderView>,
    ) {
        mutableState.update { it.copy(pendingAction = action, actionError = null) }
        viewModelScope.launch {
            val result = safely(block)
            mutableState.update { state ->
                result.fold(
                    onSuccess = { order -> state.copy(pendingAction = null, order = order, now = clock()) },
                    onFailure = { failure -> state.copy(pendingAction = null, actionError = failure.toActionMessage()) },
                )
            }
            syncBuyerData()
        }
    }

    private fun load(showLoading: Boolean) {
        if (showLoading) mutableState.update { it.copy(isLoading = true, loadError = null) }
        viewModelScope.launch {
            val orderResult = safely { loadOrder(orderId) }
            val order = orderResult.getOrNull()
            val known = mutableState.value.offer
            val offer =
                when {
                    order == null -> null
                    known != null && known.id == order.offerId -> known
                    else -> safely { loadOffer(order.offerId) }.getOrNull()
                }
            mutableState.update { state ->
                val failure = orderResult.exceptionOrNull()
                when {
                    failure == null -> state.copy(isLoading = false, loadError = null, order = order, offer = offer, now = clock())
                    showLoading || state.order == null -> state.copy(isLoading = false, loadError = failure.toLoadMessage())
                    else -> state.copy(isLoading = false)
                }
            }
            syncBuyerData()
        }
    }

    private fun syncBuyerData() {
        val state = mutableState.value
        if (!state.isBuyer) return
        if (!state.isPickupUnlocked && state.pickup != null) {
            mutableState.update { it.copy(pickup = null) }
        }
        if (state.isInProgress && state.code == null) {
            viewModelScope.launch { fetchCode() }
        }
        if (state.isPickupUnlocked && state.pickup == null) {
            viewModelScope.launch {
                safely { loadPickup(orderId) }.onSuccess { pickup ->
                    mutableState.update { if (it.isPickupUnlocked) it.copy(pickup = pickup) else it }
                }
            }
        }
    }

    private suspend fun fetchCode() {
        val stored = runCatching { readStoredCode(orderId) }.getOrNull()
        if (stored != null) {
            mutableState.update { it.copy(code = stored) }
            return
        }
        safely { issueCode(orderId) }
            .onSuccess { issued ->
                runCatching { storeCode(orderId, issued.code) }
                mutableState.update { it.copy(code = issued.code) }
            }
            .onFailure { failure -> mutableState.update { it.copy(actionError = failure.toActionMessage()) } }
    }

    private suspend fun <T> safely(block: suspend () -> Result<T>): Result<T> =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    private fun Throwable.toLoadMessage(): String =
        when ((this as? ApiError)?.code) {
            0 -> NO_CONNECTION
            403, 404 -> ORDER_NOT_FOUND
            else -> LOAD_ERROR
        }

    private fun Throwable.toActionMessage(): String =
        when ((this as? ApiError)?.code) {
            0 -> NO_CONNECTION
            409, 422 -> (this as ApiError).message
            else -> ACTION_ERROR
        }

    companion object {
        const val NO_CONNECTION = "Sin conexión. Revisa tu internet e inténtalo de nuevo."
        const val ORDER_NOT_FOUND = "No encontramos este pedido."
        const val LOAD_ERROR = "No pudimos cargar el pedido. Inténtalo de nuevo."
        const val ACTION_ERROR = "No pudimos completar la acción. Inténtalo de nuevo."
        const val INVALID_CODE = "El código tiene 6 dígitos."
        private const val MAX_REASON_LENGTH = 200
    }
}
