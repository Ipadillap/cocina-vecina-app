package cl.udec.cocinavecina.ui.requests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cl.udec.cocinavecina.AppContainer
import cl.udec.cocinavecina.data.network.Price
import cl.udec.cocinavecina.data.network.PurchaseRequestView
import cl.udec.cocinavecina.data.session.SessionState
import cl.udec.cocinavecina.ui.common.ErrorMessage
import cl.udec.cocinavecina.ui.common.Format
import kotlinx.coroutines.launch

@Composable
fun RequestsScreen(
    container: AppContainer,
    session: SessionState.Active,
    onOpenOrder: (Long) -> Unit,
) {
    val repository = container.repository
    var received by remember { mutableStateOf<List<PurchaseRequestView>>(emptyList()) }
    var own by remember { mutableStateOf<List<PurchaseRequestView>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var version by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(version) {
        if (session.isSeller) repository.call { pendingRequests() }.onSuccess { received = it }
        repository.call { myRequests() }.onSuccess { own = it }.onFailure { error = it.message }
    }
    LaunchedEffect(Unit) {
        container.events.events.collect { if (it.entity == "purchase_request") version++ }
    }

    fun action(block: suspend () -> Result<Any?>) {
        scope.launch {
            block().onSuccess { error = null }.onFailure { error = it.message }
            version++
        }
    }

    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { ErrorMessage(error) }
        if (session.isSeller) {
            item { Text("Recibidas", style = MaterialTheme.typography.titleMedium) }
            items(received, key = { "r${it.id}" }) { s ->
                RequestCard(
                    s = s,
                    isSeller = true,
                    onAccept = { action { repository.call { accept(s.id) } } },
                    onReject = { action { repository.call { reject(s.id) } } },
                    onCounteroffer = { p -> action { repository.call { counteroffer(s.id, Price(p)) } } },
                    onOpenOrder = onOpenOrder,
                )
            }
        }
        item { Text("Mis solicitudes", style = MaterialTheme.typography.titleMedium) }
        items(own, key = { "p${it.id}" }) { s ->
            RequestCard(
                s = s,
                isSeller = false,
                onAccept = { action { repository.call { accept(s.id) } } },
                onReject = { action { repository.call { reject(s.id) } } },
                onCounteroffer = { p -> action { repository.call { propose(s.id, Price(p)) } } },
                onOpenOrder = onOpenOrder,
            )
        }
    }
}

@Composable
private fun RequestCard(
    s: PurchaseRequestView,
    isSeller: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onCounteroffer: (Int) -> Unit,
    onOpenOrder: (Long) -> Unit,
) {
    val myTurn = s.turn == (if (isSeller) "SELLER" else "BUYER")
    var newPrice by remember(s.id) { mutableStateOf("") }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Oferta ${s.offerId} · ${s.portions} porción(es)", style = MaterialTheme.typography.titleSmall)
            Text("${Format.requestStatus(s.status)} · ${Format.pesos(s.currentPrice)} c/u · ronda ${s.rounds} de 3")
            s.closeReason?.let { Text("Motivo: $it", style = MaterialTheme.typography.bodySmall) }
            if (myTurn) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onAccept) { Text("Aceptar") }
                    OutlinedButton(onClick = onReject) { Text("Rechazar") }
                }
                if (s.rounds < 3) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newPrice,
                            onValueChange = { newPrice = it.filter(Char::isDigit) },
                            label = { Text(if (isSeller) "Contraoferta" else "Nuevo precio") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.width(180.dp),
                        )
                        OutlinedButton(onClick = { newPrice.toIntOrNull()?.let(onCounteroffer) }) { Text("Enviar") }
                    }
                }
            } else if (s.turn != null) {
                Text("Esperando respuesta de la otra parte", style = MaterialTheme.typography.bodySmall)
            }
            s.orderId?.let { id -> Button(onClick = { onOpenOrder(id) }) { Text("Ver pedido") } }
        }
    }
}
