package cl.udec.cocinavecina.ui.offer

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
import kotlin.coroutines.cancellation.CancellationException

class OfferDetailViewModel(
    private val offerId: Long,
    distanceMeters: Long?,
    private val loadOffer: suspend (offerId: Long) -> Result<OfferView>,
    private val requestPurchase: suspend (offerId: Long, portions: Int) -> Result<PurchaseRequestView>,
    private val sendReport: suspend (offerId: Long, reason: String) -> Result<Unit>,
    private val resolvePhotoUrl: (String?) -> String?,
) : ViewModel() {
    private val mutableState = MutableStateFlow(OfferDetailUiState(distanceMeters = distanceMeters))
    val uiState: StateFlow<OfferDetailUiState> = mutableState.asStateFlow()

    init {
        load()
    }

    fun onRetry() = load()

    fun onPortionsChange(portions: Int) {
        val max = mutableState.value.maxPortions
        if (portions !in 1..max) return
        mutableState.update { it.copy(portions = portions, buyError = null) }
    }

    fun onBuyClick() {
        val current = mutableState.value
        if (current.isBuying || !current.isOnSale) return
        mutableState.update { it.copy(isBuying = true, buyError = null) }
        viewModelScope.launch {
            val result = safely { requestPurchase(offerId, current.portions) }
            mutableState.update { state ->
                result.fold(
                    onSuccess = { request -> state.copy(isBuying = false, createdRequestId = request.id) },
                    onFailure = { failure -> state.copy(isBuying = false, buyError = failure.toBuyMessage()) },
                )
            }
        }
    }

    fun onPurchaseNavigationHandled() {
        mutableState.update { it.copy(createdRequestId = null) }
    }

    fun onReportClick() {
        mutableState.update { it.copy(isReportDialogVisible = true) }
    }

    fun onReportDismiss() {
        mutableState.update { it.copy(isReportDialogVisible = false) }
    }

    fun onReportConfirm(reason: String) {
        if (mutableState.value.isReporting) return
        mutableState.update { it.copy(isReporting = true) }
        viewModelScope.launch {
            val result = safely { sendReport(offerId, reason) }
            mutableState.update {
                it.copy(
                    isReporting = false,
                    isReportDialogVisible = false,
                    reportMessage = if (result.isSuccess) REPORT_SENT else REPORT_FAILED,
                )
            }
        }
    }

    private fun load() {
        mutableState.update { it.copy(isLoading = true, loadError = null) }
        viewModelScope.launch {
            val result = safely { loadOffer(offerId) }
            mutableState.update { state ->
                result.fold(
                    onSuccess = { offer ->
                        val resolved = offer.copy(photoUrl = resolvePhotoUrl(offer.photoUrl))
                        val maxPortions = minOf(resolved.availablePortions, MAX_PORTIONS_PER_REQUEST).coerceAtLeast(1)
                        state.copy(isLoading = false, offer = resolved, portions = state.portions.coerceIn(1, maxPortions))
                    },
                    onFailure = { failure -> state.copy(isLoading = false, loadError = failure.toLoadMessage()) },
                )
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
            404 -> OFFER_NOT_FOUND
            else -> LOAD_ERROR
        }

    private fun Throwable.toBuyMessage(): String =
        when ((this as? ApiError)?.code) {
            0 -> NO_CONNECTION
            403 -> UNVERIFIED_ACCOUNT
            409, 422 -> (this as ApiError).message
            else -> BUY_ERROR
        }

    companion object {
        const val NO_CONNECTION = "Sin conexión. Revisa tu internet e inténtalo de nuevo."
        const val OFFER_NOT_FOUND = "Esta oferta ya no existe."
        const val LOAD_ERROR = "No pudimos cargar la oferta. Inténtalo de nuevo."
        const val UNVERIFIED_ACCOUNT = "Verifica tu correo para poder comprar."
        const val BUY_ERROR = "No pudimos enviar tu solicitud. Inténtalo de nuevo."
        const val REPORT_SENT = "Reporte enviado. Gracias por avisar."
        const val REPORT_FAILED = "No pudimos enviar el reporte. Inténtalo más tarde."
    }
}
