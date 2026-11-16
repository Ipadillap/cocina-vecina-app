package cl.udec.cocinavecina.ui.requests

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.udec.cocinavecina.data.Repository
import cl.udec.cocinavecina.data.network.PurchaseRequestView
import cl.udec.cocinavecina.data.network.UserEvent
import cl.udec.cocinavecina.ui.common.EmptyState
import cl.udec.cocinavecina.ui.common.ErrorMessage
import cl.udec.cocinavecina.ui.common.Format
import cl.udec.cocinavecina.ui.common.NegotiationRules
import kotlinx.coroutines.flow.Flow

/**
 * Solicitudes que yo envié como comprador. Responderlas (aceptar, rechazar, proponer otro precio)
 * se hace en [RequestTrackingRoute]; las que recibo como vendedor viven en la pestaña "Vender".
 */
@Composable
fun RequestsScreen(
    repository: Repository,
    events: Flow<UserEvent>,
    onOpenRequest: (Long) -> Unit,
    onOpenOrder: (Long) -> Unit,
) {
    var requests by remember { mutableStateOf<List<PurchaseRequestView>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var version by remember { mutableIntStateOf(0) }

    LaunchedEffect(version) {
        repository.call { myRequests() }.onSuccess { requests = it }.onFailure { error = it.message }
    }
    LaunchedEffect(Unit) {
        events.collect { if (it.entity == "purchase_request") version++ }
    }

    ErrorMessage(error)
    val list = requests ?: return
    if (list.isEmpty()) return EmptyState("No has enviado solicitudes todavía.")
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(list, key = { it.id }) { request ->
            Card(Modifier.fillMaxWidth().clickable { onOpenRequest(request.id) }) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Oferta ${request.offerId} · ${request.portions} porción(es)", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${Format.requestStatus(request.status)} · ${Format.pesos(request.currentPrice)} c/u · " +
                            "ronda ${request.rounds} de ${NegotiationRules.MAX_ROUNDS}",
                    )
                    request.closeReason?.let { Text("Motivo: $it", style = MaterialTheme.typography.bodySmall) }
                    if (request.turn == NegotiationRules.BUYER) {
                        Text("Te toca responder", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                    request.orderId?.let { id -> Button(onClick = { onOpenOrder(id) }) { Text("Ver pedido") } }
                }
            }
        }
    }
}
