package cl.udec.cocinavecina.ui.offer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.udec.cocinavecina.data.ApiError
import cl.udec.cocinavecina.data.network.OfferView
import cl.udec.cocinavecina.data.network.PurchaseRequestView
import cl.udec.cocinavecina.ui.common.NegotiationRules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class ProposePriceViewModel(
    private val offerId: Long,
    initialPortions: Int,
    private val loadOffer: suspend (offerId: Long) -> Result<OfferView>,
    private val submitRequest: suspend (offerId: Long, portions: Int, unitPrice: Int?) -> Result<PurchaseRequestView>,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ProposePriceUiState(portions = initialPortions.coerceAtLeast(1)))
    val uiState: StateFlow<ProposePriceUiState> = mutableState.asStateFlow()

    init {
        load()
    }

    fun onRetry() = load()

    fun onPriceTextChange(value: String) {
        val digits = value.filter(Char::isDigit).take(MAX_PRICE_DIGITS).trimStart('0')
        mutableState.update { it.copy(priceText = digits, submitError = null) }
    }

    fun onIncreasePrice() = stepPrice(NegotiationRules.PRICE_STEP)

    fun onDecreasePrice() = stepPrice(-NegotiationRules.PRICE_STEP)

    fun onShortcutSelect(price: Int) {
        mutableState.update { it.copy(priceText = price.toString(), submitError = null) }
    }

    fun onPortionsChange(portions: Int) {
        if (portions !in 1..mutableState.value.maxPortions) return
        mutableState.update { it.copy(portions = portions, submitError = null) }
    }

    fun onSubmitClick() {
        val current = mutableState.value
        if (!current.canSubmit) return
        submit(current.portions, current.price)
    }

    fun onBuyAtPublishedPriceClick() {
        val current = mutableState.value
        if (current.offer == null || current.isSubmitting || current.maxPortions == 0) return
        submit(current.portions, null)
    }

    fun onNavigationHandled() {
        mutableState.update { it.copy(createdRequestId = null) }
    }

    private fun stepPrice(delta: Int) {
        val current = mutableState.value
        val offer = current.offer ?: return
        val base = current.price?.let { NegotiationRules.roundToStep(it.toDouble()) } ?: offer.basePrice
        val next = (base + delta).coerceIn(offer.minPrice, offer.basePrice)
        mutableState.update { it.copy(priceText = next.toString(), submitError = null) }
    }

    private fun submit(
        portions: Int,
        unitPrice: Int?,
    ) {
        mutableState.update { it.copy(isSubmitting = true, submitError = null) }
        viewModelScope.launch {
            val result = safely { submitRequest(offerId, portions, unitPrice) }
            mutableState.update { state ->
                result.fold(
                    onSuccess = { request -> state.copy(isSubmitting = false, createdRequestId = request.id) },
                    onFailure = { failure -> state.copy(isSubmitting = false, submitError = failure.toSubmitMessage()) },
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
                        val maxPortions = minOf(offer.availablePortions, MAX_PORTIONS_PER_REQUEST).coerceAtLeast(1)
                        state.copy(
                            isLoading = false,
                            offer = offer,
                            portions = state.portions.coerceIn(1, maxPortions),
                            priceText = state.priceText.ifEmpty { suggestedPrice(offer).toString() },
                        )
                    },
                    onFailure = { failure ->
                        val code = (failure as? ApiError)?.code
                        state.copy(isLoading = false, loadError = if (code == 0) NO_CONNECTION else LOAD_ERROR)
                    },
                )
            }
        }
    }

    private fun suggestedPrice(offer: OfferView): Int =
        NegotiationRules.roundToStep(offer.basePrice * SUGGESTED_DISCOUNT).coerceIn(offer.minPrice, offer.basePrice)

    private suspend fun <T> safely(block: suspend () -> Result<T>): Result<T> =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    private fun Throwable.toSubmitMessage(): String =
        when ((this as? ApiError)?.code) {
            0 -> NO_CONNECTION
            403 -> UNVERIFIED_ACCOUNT
            409, 422 -> (this as ApiError).message
            else -> SUBMIT_ERROR
        }

    companion object {
        const val NO_CONNECTION = "Sin conexión. Revisa tu internet e inténtalo de nuevo."
        const val LOAD_ERROR = "No pudimos cargar la oferta. Inténtalo de nuevo."
        const val UNVERIFIED_ACCOUNT = "Verifica tu correo para poder proponer un precio."
        const val SUBMIT_ERROR = "No pudimos enviar tu propuesta. Inténtalo de nuevo."
        private const val MAX_PRICE_DIGITS = 6
        private const val SUGGESTED_DISCOUNT = 0.9
    }
}
