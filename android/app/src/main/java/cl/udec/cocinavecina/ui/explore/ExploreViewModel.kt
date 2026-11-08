package cl.udec.cocinavecina.ui.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.udec.cocinavecina.data.Repository
import cl.udec.cocinavecina.data.network.NearbyOffer
import cl.udec.cocinavecina.location.Coordinate
import cl.udec.cocinavecina.location.Locator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ExploreViewModel(private val repository: Repository) : ViewModel() {
    data class UiState(
        val center: Coordinate = Locator.CONCEPCION,
        val radius: Int = 1500,
        val order: String = "DISTANCE",
        val showMap: Boolean = true,
        val loading: Boolean = false,
        val offers: List<NearbyOffer> = emptyList(),
        val error: String? = null,
    )

    private val mutableUiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = mutableUiState.asStateFlow()

    fun setCenter(c: Coordinate) {
        mutableUiState.update { it.copy(center = c) }
        search()
    }

    fun setRadius(meters: Int) {
        mutableUiState.update { it.copy(radius = meters) }
        search()
    }

    fun setOrder(order: String) {
        mutableUiState.update { it.copy(order = order) }
        search()
    }

    fun toggleView() = mutableUiState.update { it.copy(showMap = !it.showMap) }

    fun search() {
        val e = mutableUiState.value
        mutableUiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            repository.call { nearby(e.center.lat, e.center.lon, e.radius, e.order) }
                .onSuccess { list -> mutableUiState.update { it.copy(loading = false, offers = list) } }
                .onFailure { err -> mutableUiState.update { it.copy(loading = false, error = err.message) } }
        }
    }
}
