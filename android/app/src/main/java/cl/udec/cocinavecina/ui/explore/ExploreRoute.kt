package cl.udec.cocinavecina.ui.explore

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.udec.cocinavecina.data.Repository
import cl.udec.cocinavecina.location.Locator

@Composable
fun ExploreRoute(
    repository: Repository,
    onOfferClick: (offerId: Long, distanceMeters: Long?) -> Unit,
) {
    val viewModel: ExploreViewModel =
        viewModel(
            factory =
                viewModelFactory {
                    initializer {
                        ExploreViewModel(
                            searchOffers = { center, radiusMeters, order ->
                                repository.call { nearby(center.lat, center.lon, radiusMeters, order) }
                            },
                            resolvePhotoUrl = repository::absoluteUrl,
                        )
                    }
                },
        )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val locationLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                viewModel.onPlaceSelect(ExplorePlace.currentLocation(Locator.lastKnown(context)))
            } else {
                viewModel.onLocationPermissionDenied()
            }
        }
    LaunchedEffect(Unit) {
        viewModel.start(if (Locator.hasPermission(context)) Locator.lastKnown(context) else null)
    }
    ExploreScreen(
        state = state,
        onPlaceSelect = viewModel::onPlaceSelect,
        onCurrentLocationSelect = {
            if (Locator.hasPermission(context)) {
                viewModel.onPlaceSelect(ExplorePlace.currentLocation(Locator.lastKnown(context)))
            } else {
                locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        },
        onRadiusSelect = viewModel::onRadiusSelect,
        onOrderSelect = viewModel::onOrderSelect,
        onCategorySelect = viewModel::onCategorySelect,
        onViewModeChange = viewModel::onViewModeChange,
        onRetry = viewModel::onRetry,
        onOfferClick = { offerId -> onOfferClick(offerId, state.offers.firstOrNull { it.id == offerId }?.distanceMeters) },
    )
}
