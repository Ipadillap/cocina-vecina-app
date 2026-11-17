package cl.udec.cocinavecina.ui.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.udec.cocinavecina.data.ApiError
import cl.udec.cocinavecina.data.network.NearbyOffer
import cl.udec.cocinavecina.location.Coordinate
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class ExploreViewModel(
    private val searchOffers: suspend (center: Coordinate, radiusMeters: Int, order: String) -> Result<List<NearbyOffer>>,
    private val resolvePhotoUrl: (String?) -> String?,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = mutableState.asStateFlow()

    private var searchJob: Job? = null
    private var started = false

    fun start(currentLocation: Coordinate?) {
        if (started) return
        started = true
        if (currentLocation != null) {
            mutableState.update { it.copy(place = ExplorePlace.currentLocation(currentLocation)) }
        }
        search()
    }

    fun onPlaceSelect(place: ExplorePlace) {
        mutableState.update { it.copy(place = place, locationNotice = null) }
        search()
    }

    fun onLocationPermissionDenied() {
        mutableState.update { it.copy(locationNotice = LOCATION_DENIED) }
    }

    fun onRadiusSelect(radius: SearchRadius) {
        if (radius == mutableState.value.radius) return
        mutableState.update { it.copy(radius = radius) }
        search()
    }

    fun onOrderSelect(order: OfferOrder) {
        if (order == mutableState.value.order) return
        mutableState.update { it.copy(order = order) }
        search()
    }

    fun onCategorySelect(category: String?) {
        mutableState.update { it.copy(category = category) }
    }

    fun onViewModeChange(viewMode: ExploreViewMode) {
        mutableState.update { it.copy(viewMode = viewMode) }
    }

    fun onRetry() = search()

    /** Al volver a la pestaña: actualiza sin tapar la lista actual. */
    fun refresh() {
        if (!started || mutableState.value.isLoading) return
        search(showLoading = false)
    }

    private fun search(showLoading: Boolean = true) {
        val current = mutableState.value
        searchJob?.cancel()
        if (showLoading) mutableState.update { it.copy(isLoading = true, error = null) }
        searchJob =
            viewModelScope.launch {
                val result =
                    try {
                        searchOffers(current.place.coordinate, current.radius.meters, current.order.apiValue)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Result.failure(e)
                    }
                mutableState.update { state ->
                    result.fold(
                        onSuccess = { offers ->
                            state.copy(isLoading = false, offers = offers.map { it.copy(photoUrl = resolvePhotoUrl(it.photoUrl)) })
                        },
                        onFailure = { failure -> state.copy(isLoading = false, error = failure.toMessage()) },
                    )
                }
            }
    }

    private fun Throwable.toMessage(): String = if ((this as? ApiError)?.code == 0) NO_CONNECTION else LOAD_ERROR

    companion object {
        const val NO_CONNECTION = "Sin conexión. Revisa tu internet e inténtalo de nuevo."
        const val LOAD_ERROR = "No pudimos cargar las ofertas. Inténtalo de nuevo."
        const val LOCATION_DENIED = "Sin permiso de ubicación. Elige un barrio para buscar."
    }
}
