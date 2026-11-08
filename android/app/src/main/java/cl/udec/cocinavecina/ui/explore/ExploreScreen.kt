package cl.udec.cocinavecina.ui.explore

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.udec.cocinavecina.data.Repository
import cl.udec.cocinavecina.data.network.NearbyOffer
import cl.udec.cocinavecina.location.Locator
import cl.udec.cocinavecina.ui.common.Format
import cl.udec.cocinavecina.ui.common.ErrorMessage
import cl.udec.cocinavecina.ui.common.EmptyState

private val RADII = listOf(500 to "0,5 km", 1500 to "1,5 km", 3000 to "3 km", 5000 to "5 km")

@Composable
fun ExploreScreen(repository: Repository, onOpenOffer: (Long) -> Unit) {
    val vm: ExploreViewModel = viewModel { ExploreViewModel(repository) }
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val requestPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        vm.setCenter(Locator.lastKnown(context))
    }
    LaunchedEffect(Unit) {
        if (Locator.hasPermission(context)) {
            vm.setCenter(Locator.lastKnown(context))
        } else {
            requestPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            RADII.forEach { (meters, label) ->
                FilterChip(uiState.radius == meters, { vm.setRadius(meters) }, label = { Text(label) })
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = vm::toggleView) { Text(if (uiState.showMap) "Lista" else "Mapa") }
        }
        if (uiState.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        ErrorMessage(uiState.error)
        when {
            uiState.showMap -> OffersMap(uiState.center, uiState.offers, onOpenOffer, Modifier.fillMaxSize())
            uiState.offers.isEmpty() && !uiState.loading -> EmptyState("No hay ofertas activas en este radio.")
            else -> OfferList(uiState.offers, onOpenOffer)
        }
    }
}

@Composable
private fun OfferList(offers: List<NearbyOffer>, onOpen: (Long) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(offers, key = { it.id }) { o ->
            Card(Modifier.fillMaxWidth().clickable { onOpen(o.id) }) {
                Column(Modifier.padding(16.dp)) {
                    Text(o.title, style = MaterialTheme.typography.titleMedium)
                    Text("${Format.pesos(o.basePrice)} · ${o.availablePortions} porciones")
                    Text(
                        "a ${Format.distance(o.distanceMeters)} · ${Format.timeLeft(o.expiresAt)}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
