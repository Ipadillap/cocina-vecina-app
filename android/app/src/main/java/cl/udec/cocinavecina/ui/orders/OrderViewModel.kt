package cl.udec.cocinavecina.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.udec.cocinavecina.AppContainer
import cl.udec.cocinavecina.data.network.Cancellation
import cl.udec.cocinavecina.data.network.PickupDetailsView
import cl.udec.cocinavecina.data.network.OrderView
import cl.udec.cocinavecina.data.network.CodeValidation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class OrderViewModel(private val container: AppContainer, private val orderId: Long) : ViewModel() {
    data class UiState(
        val order: OrderView? = null,
        val code: String? = null,
        val pickup: PickupDetailsView? = null,
        val error: String? = null,
        val message: String? = null,
    )

    private val repository = container.repository
    private val mutableUiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                load()
                delay(15_000)
            }
        }
        viewModelScope.launch {
            container.events.events.collect { if (it.entity == "order" && it.entityId == orderId) load() }
        }
    }

    private suspend fun load() {
        repository.call { order(orderId) }
            .onSuccess { p -> mutableUiState.update { it.copy(order = p) } }
            .onFailure { e -> mutableUiState.update { it.copy(error = e.message) } }
    }

    fun loadCode() = viewModelScope.launch {
        val stored = repository.session.code(orderId)
        if (stored != null) {
            mutableUiState.update { it.copy(code = stored) }
            return@launch
        }
        repository.call { issueCode(orderId) }
            .onSuccess { c ->
                repository.session.saveCode(orderId, c.code)
                mutableUiState.update { it.copy(code = c.code) }
            }
            .onFailure { e -> mutableUiState.update { it.copy(error = e.message) } }
    }

    fun showPickupDetails() = viewModelScope.launch {
        repository.call { pickupDetails(orderId) }
            .onSuccess { d -> mutableUiState.update { it.copy(pickup = d, error = null) } }
            .onFailure { e -> mutableUiState.update { it.copy(error = e.message) } }
    }

    fun markReady() = act { markReady(orderId) }

    fun validate(code: String) = act { validateDelivery(orderId, CodeValidation(code)) }

    fun cancel(reason: String) = act { cancel(orderId, Cancellation(reason)) }

    private fun act(call: suspend cl.udec.cocinavecina.data.network.CocinaVecinaApi.() -> OrderView) =
        viewModelScope.launch {
            repository.call(call)
                .onSuccess { p -> mutableUiState.update { it.copy(order = p, error = null) } }
                .onFailure { e -> mutableUiState.update { it.copy(error = e.message) } }
        }
}
