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
    private val target: ProposalTarget,
    private val loadOffer: suspend (offerId: Long) -> Result<OfferView>,
    private val loadRequest: suspend (requestId: Long) -> Result<PurchaseRequestView>,
    private val submitRequest: suspend (offerId: Long, portions: Int, unitPrice: Int?) -> Result<PurchaseRequestView>,
    private val sendProposal: suspend (requestId: Long, unitPrice: Int) -> Result<PurchaseRequestView>,
) : ViewModel() {
    private val mutableState =
        MutableStateFlow(
            ProposePriceUiState(portions = (target as? ProposalTarget.NewRequest)?.initialPortions?.coerceAtLeast(1) ?: 1),
        )
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
        val current = mutableState.value
        if (current.isCounterProposal || portions !in 1..current.maxPortions) return
        mutableState.update { it.copy(portions = portions, submitError = null) }
    }

    fun onSubmitClick() {
        val current = mutableState.value
        if (!current.canSubmit) return
        val price = current.price ?: return
        when (target) {
            is ProposalTarget.NewRequest -> submit { submitRequest(target.offerId, current.portions, price) }
            is ProposalTarget.ExistingRequest -> submit { sendProposal(target.requestId, price) }
        }
    }

    fun onBuyAtPublishedPriceClick() {
        val current = mutableState.value
        if (target !is ProposalTarget.NewRequest || current.offer == null || current.isSubmitting || current.maxPortions == 0) return
        submit { submitRequest(target.offerId, current.portions, null) }
    }

    fun onNavigationHandled() {
        mutableState.update { it.copy(completedRequestId = null) }
    }

    private fun stepPrice(delta: Int) {
        val current = mutableState.value
        val offer = current.offer ?: return
        val base = current.price?.let { NegotiationRules.roundToStep(it.toDouble()) } ?: current.maxPrice
        val next = (base + delta).coerceIn(offer.minPrice, current.maxPrice)
        mutableState.update { it.copy(priceText = next.toString(), submitError = null) }
    }

    private fun submit(block: suspend () -> Result<PurchaseRequestView>) {
        mutableState.update { it.copy(isSubmitting = true, submitError = null) }
        viewModelScope.launch {
            val result = safely(block)
            mutableState.update { state ->
                result.fold(
                    onSuccess = { request -> state.copy(isSubmitting = false, completedRequestId = request.id) },
                    onFailure = { failure -> state.copy(isSubmitting = false, submitError = failure.toSubmitMessage()) },
                )
            }
        }
    }

    private fun load() {
        mutableState.update { it.copy(isLoading = true, loadError = null) }
        viewModelScope.launch {
            val result =
                safely {
                    when (target) {
                        is ProposalTarget.NewRequest -> loadOffer(target.offerId).map { it to null }
                        is ProposalTarget.ExistingRequest ->
                            loadRequest(target.requestId).mapCatching { request ->
                                loadOffer(request.offerId).getOrThrow() to request
                            }
                    }
                }
            mutableState.update { state ->
                result.fold(
                    onSuccess = { (offer, request) -> loaded(state, offer, request) },
                    onFailure = { failure ->
                        val code = (failure as? ApiError)?.code
                        state.copy(isLoading = false, loadError = if (code == 0) NO_CONNECTION else LOAD_ERROR)
                    },
                )
            }
        }
    }

    private fun loaded(
        state: ProposePriceUiState,
        offer: OfferView,
        request: PurchaseRequestView?,
    ): ProposePriceUiState {
        val withData = state.copy(isLoading = false, offer = offer, request = request)
        val portions =
            request?.portions
                ?: state.portions.coerceIn(1, minOf(offer.availablePortions, MAX_PORTIONS_PER_REQUEST).coerceAtLeast(1))
        return withData.copy(
            portions = portions,
            priceText = state.priceText.ifEmpty { suggestedPrice(withData, offer, request).toString() },
        )
    }

    private fun suggestedPrice(
        state: ProposePriceUiState,
        offer: OfferView,
        request: PurchaseRequestView?,
    ): Int {
        val lastBuyerPrice = request?.proposals?.lastOrNull { it.party == NegotiationRules.BUYER }?.unitPrice
        val counter = state.sellerCounterPrice
        val suggested =
            if (lastBuyerPrice != null && counter != null) {
                NegotiationRules.roundToStep((lastBuyerPrice + counter) / 2.0)
            } else {
                NegotiationRules.roundToStep(state.maxPrice * SUGGESTED_DISCOUNT)
            }
        return suggested.coerceIn(offer.minPrice, state.maxPrice.coerceAtLeast(offer.minPrice))
    }

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
