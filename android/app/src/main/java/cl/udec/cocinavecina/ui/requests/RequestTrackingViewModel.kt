package cl.udec.cocinavecina.ui.requests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.udec.cocinavecina.data.ApiError
import cl.udec.cocinavecina.data.network.OfferView
import cl.udec.cocinavecina.data.network.PurchaseRequestView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import kotlin.coroutines.cancellation.CancellationException

class RequestTrackingViewModel(
    private val requestId: Long,
    private val loadRequest: suspend (requestId: Long) -> Result<PurchaseRequestView>,
    private val loadOffer: suspend (offerId: Long) -> Result<OfferView>,
    private val acceptRequest: suspend (requestId: Long) -> Result<PurchaseRequestView>,
    private val rejectRequest: suspend (requestId: Long) -> Result<PurchaseRequestView>,
    private val clock: () -> Instant = Instant::now,
) : ViewModel() {
    private val mutableState = MutableStateFlow(RequestTrackingUiState(now = clock()))
    val uiState: StateFlow<RequestTrackingUiState> = mutableState.asStateFlow()

    init {
        load(showLoading = true)
    }

    fun onRetry() = load(showLoading = true)

    fun refresh() = load(showLoading = false)

    fun onTick() {
        mutableState.update { it.copy(now = clock()) }
    }

    fun onAcceptClick() {
        val current = mutableState.value
        if (!current.isBuyerTurn || current.isActing) return
        act(RequestAction.ACCEPT) { acceptRequest(requestId) }
    }

    fun onRejectClick() {
        val current = mutableState.value
        if (!current.isBuyerTurn || current.isActing) return
        mutableState.update { it.copy(isRejectConfirmVisible = true) }
    }

    fun onRejectDismiss() {
        mutableState.update { it.copy(isRejectConfirmVisible = false) }
    }

    fun onRejectConfirm() {
        mutableState.update { it.copy(isRejectConfirmVisible = false) }
        if (mutableState.value.isActing) return
        act(RequestAction.REJECT) { rejectRequest(requestId) }
    }

    fun onOrderNavigationHandled() {
        mutableState.update { it.copy(openOrderId = null) }
    }

    private fun act(
        action: RequestAction,
        block: suspend () -> Result<PurchaseRequestView>,
    ) {
        mutableState.update { it.copy(pendingAction = action, actionError = null) }
        viewModelScope.launch {
            val result = safely(block)
            mutableState.update { state ->
                result.fold(
                    onSuccess = { request ->
                        state.copy(
                            pendingAction = null,
                            request = request,
                            now = clock(),
                            openOrderId = if (action == RequestAction.ACCEPT) request.orderId else null,
                        )
                    },
                    onFailure = { failure -> state.copy(pendingAction = null, actionError = failure.toActionMessage()) },
                )
            }
        }
    }

    private fun load(showLoading: Boolean) {
        if (showLoading) mutableState.update { it.copy(isLoading = true, loadError = null) }
        viewModelScope.launch {
            val requestResult = safely { loadRequest(requestId) }
            val request = requestResult.getOrNull()
            val known = mutableState.value.offer
            val offerResult =
                when {
                    request == null -> null
                    known != null && known.id == request.offerId -> Result.success(known)
                    else -> safely { loadOffer(request.offerId) }
                }
            mutableState.update { state ->
                val failure = requestResult.exceptionOrNull() ?: offerResult?.exceptionOrNull()
                when {
                    failure == null -> state.copy(isLoading = false, loadError = null, request = request, offer = offerResult?.getOrNull(), now = clock())
                    showLoading || state.request == null -> state.copy(isLoading = false, loadError = failure.toLoadMessage())
                    else -> state.copy(isLoading = false)
                }
            }
        }
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
            403, 404 -> REQUEST_NOT_FOUND
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
        const val REQUEST_NOT_FOUND = "No encontramos esta solicitud."
        const val LOAD_ERROR = "No pudimos cargar la solicitud. Inténtalo de nuevo."
        const val ACTION_ERROR = "No pudimos completar la acción. Inténtalo de nuevo."
    }
}
