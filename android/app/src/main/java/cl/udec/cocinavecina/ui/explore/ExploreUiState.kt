package cl.udec.cocinavecina.ui.explore

import cl.udec.cocinavecina.data.network.NearbyOffer
import cl.udec.cocinavecina.location.Coordinate
import cl.udec.cocinavecina.location.Locator

data class ExplorePlace(
    val id: String,
    val name: String,
    val searchLabel: String,
    val coordinate: Coordinate,
) {
    val isCurrentLocation: Boolean
        get() = id == CURRENT_LOCATION_ID

    companion object {
        const val CURRENT_LOCATION_ID = "current-location"

        val UNIVERSITY_DISTRICT =
            ExplorePlace("university-district", "Barrio Universitario", "Barrio Universitario, Concepción", Coordinate(-36.8297, -73.0365))
        val DOWNTOWN = ExplorePlace("downtown", "Centro", "Centro, Concepción", Locator.CONCEPCION)
        val STATION_DISTRICT = ExplorePlace("station-district", "Barrio Estación", "Barrio Estación, Concepción", Coordinate(-36.8240, -73.0610))
        val PEDRO_DE_VALDIVIA = ExplorePlace("pedro-de-valdivia", "Pedro de Valdivia", "Pedro de Valdivia, Concepción", Coordinate(-36.8420, -73.0560))

        val NEIGHBORHOODS = listOf(UNIVERSITY_DISTRICT, DOWNTOWN, STATION_DISTRICT, PEDRO_DE_VALDIVIA)

        fun currentLocation(coordinate: Coordinate) = ExplorePlace(CURRENT_LOCATION_ID, "Mi ubicación", "tu ubicación actual", coordinate)
    }
}

enum class SearchRadius(val meters: Int, val label: String, val mapZoom: Double) {
    SHORT(500, "500 m", 14.4),
    MEDIUM(1500, "1,5 km", 12.8),
    LONG(3000, "3 km", 11.8),
    EXTRA_LONG(5000, "5 km", 11.1),
}

enum class OfferOrder(val apiValue: String, val label: String) {
    DISTANCE("DISTANCE", "Más cerca"),
    TIME_LEFT("TIME_LEFT", "Por vencer"),
}

enum class ExploreViewMode { LIST, MAP }

data class ExploreUiState(
    val place: ExplorePlace = ExplorePlace.UNIVERSITY_DISTRICT,
    val radius: SearchRadius = SearchRadius.MEDIUM,
    val order: OfferOrder = OfferOrder.DISTANCE,
    val category: String? = null,
    val viewMode: ExploreViewMode = ExploreViewMode.LIST,
    val isLoading: Boolean = false,
    val offers: List<NearbyOffer> = emptyList(),
    val error: String? = null,
    val locationNotice: String? = null,
) {
    val visibleOffers: List<NearbyOffer>
        get() = if (category == null) offers else offers.filter { it.category == category }
}
