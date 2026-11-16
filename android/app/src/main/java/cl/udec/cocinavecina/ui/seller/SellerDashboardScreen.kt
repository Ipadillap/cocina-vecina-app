package cl.udec.cocinavecina.ui.seller

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cl.udec.cocinavecina.data.Repository
import cl.udec.cocinavecina.data.network.OfferView
import cl.udec.cocinavecina.data.network.OrderView
import cl.udec.cocinavecina.data.network.Price
import cl.udec.cocinavecina.data.network.PurchaseRequestView
import cl.udec.cocinavecina.data.network.UserEvent
import cl.udec.cocinavecina.ui.common.ErrorMessage
import cl.udec.cocinavecina.ui.common.Format
import cl.udec.cocinavecina.ui.common.NegotiationRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

private val Ink = Color(0xFF13241E)
private val Forest = Color(0xFF1E4034)
private val Terracotta = Color(0xFFB8431A)
private val Peach = Color(0xFFF28B5E)

private const val SELLER = "SELLER"
private const val ACCEPTED = "ACCEPTED"
private const val READY = "READY_FOR_PICKUP"
private const val ACTIVE = "ACTIVE"

/**
 * Pestaña "Vender": mi oferta activa, solicitudes que esperan mi respuesta y pedidos por entregar.
 * Todo viene de la API (`offers/mine`, `purchase-requests/pending`, `orders`); no hay datos de ejemplo aquí.
 */
@Composable
fun SellerDashboardScreen(
    repository: Repository,
    events: Flow<UserEvent>,
    sellerId: Long,
    onPublish: () -> Unit,
    onOpenOrder: (Long) -> Unit,
) {
    var offer by remember { mutableStateOf<OfferView?>(null) }
    var requests by remember { mutableStateOf<List<PurchaseRequestView>>(emptyList()) }
    var deliveries by remember { mutableStateOf<List<OrderView>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var version by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(version) {
        repository.call { myOffers() }.onSuccess { list -> offer = list.firstOrNull { it.status == ACTIVE } }
            .onFailure { error = it.message }
        repository.call { pendingRequests() }.onSuccess { list -> requests = list.filter { it.turn == SELLER } }
        repository.call { myOrders() }.onSuccess { list ->
            deliveries = list.filter { it.sellerId == sellerId && (it.status == ACCEPTED || it.status == READY) }
        }
    }
    LaunchedEffect(Unit) {
        events.collect { if (it.entity == "purchase_request" || it.entity == "order") version++ }
    }

    fun act(block: suspend () -> Result<Any?>) {
        scope.launch {
            block().onSuccess { error = null }.onFailure { error = it.message }
            version++
        }
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Mi oferta y solicitudes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        item { ErrorMessage(error) }
        item {
            val current = offer
            if (current == null) {
                Hint("No tienes una oferta activa.")
            } else {
                ActiveOfferCard(current, onCloseEarly = { act { repository.call { closeOffer(current.id) } } })
            }
        }
        item { OutlinedButton(onClick = onPublish, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Publicar plato") } }

        item { SectionTitle(if (requests.isEmpty()) "Solicitudes nuevas" else "Solicitudes nuevas · ${requests.size}") }
        if (requests.isEmpty()) item { Hint("Aún no tienes solicitudes pendientes.") }
        items(requests, key = { "request-${it.id}" }) { request ->
            RequestCard(
                request = request,
                onAccept = { act { repository.call { accept(request.id) } } },
                onReject = { act { repository.call { reject(request.id) } } },
                onCounteroffer = { price -> act { repository.call { counteroffer(request.id, Price(price)) } } },
            )
        }

        item { SectionTitle(if (deliveries.isEmpty()) "Por entregar" else "Por entregar · ${deliveries.size}") }
        if (deliveries.isEmpty()) item { Hint("No hay pedidos por entregar.") }
        items(deliveries, key = { "order-${it.id}" }) { order ->
            DeliveryCard(
                order = order,
                onMarkReady = { act { repository.call { markReady(order.id) } } },
                onValidate = { onOpenOrder(order.id) },
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Avatar(name: String, background: Color, foreground: Color) {
    Box(Modifier.size(36.dp).clip(CircleShape).background(background), contentAlignment = Alignment.Center) {
        Text(name.first().uppercase(), fontWeight = FontWeight.Bold, color = foreground)
    }
}

private fun portionsLabel(portions: Int) = "$portions porción${if (portions == 1) "" else "es"}"

private fun buyerOf(name: String?) = name?.takeIf { it.isNotBlank() } ?: "Vecino"

/** Tarjeta oscura de la oferta en curso: tiempo restante y porciones reservadas. */
@Composable
private fun ActiveOfferCard(offer: OfferView, onCloseEarly: () -> Unit) {
    val minutesLeft = Format.minutesLeft(offer.expiresAt)
    val total = offer.totalPortions ?: offer.availablePortions
    val reserved = (total - offer.availablePortions).coerceAtLeast(0)
    val progress = 1f - (minutesLeft / 120f).coerceIn(0.05f, 0.95f)
    Card(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), CardDefaults.cardColors(containerColor = Ink)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("ACTIVA", style = MaterialTheme.typography.labelSmall, color = Color(0xFFA9BDB3))
                    Text(offer.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                }
                OutlinedButton(
                    onClick = onCloseEarly,
                    border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.45f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                ) { Text("Cerrar antes", fontWeight = FontWeight.Bold) }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color.White.copy(alpha = 0.18f))) {
                    Box(Modifier.fillMaxWidth(progress).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(Peach))
                }
                Text(
                    "${Format.duration(minutesLeft)} · hasta ${Format.clockTime(offer.expiresAt)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFB08F),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(total) { index ->
                    Box(
                        Modifier.width(26.dp).height(10.dp).clip(RoundedCornerShape(3.dp))
                            .background(if (index < reserved) Peach else Color.White.copy(alpha = 0.85f)),
                    )
                }
                Text(
                    "$reserved reservada${if (reserved == 1) "" else "s"} · ${offer.availablePortions} disponibles",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFC9D6CF),
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun RequestCard(
    request: PurchaseRequestView,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onCounteroffer: (Int) -> Unit,
) {
    val name = buyerOf(request.buyerName)
    val canCounter = request.rounds < NegotiationRules.MAX_ROUNDS
    var countering by remember(request.id) { mutableStateOf(false) }
    var newPrice by remember(request.id) { mutableStateOf("") }
    val parsedPrice = newPrice.toIntOrNull()?.takeIf(NegotiationRules::isValidStep)

    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Avatar(name, Color(0xFFDCE7DF), Forest)
                Column(Modifier.weight(1f)) {
                    Text("$name · ${portionsLabel(request.portions)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "Ronda ${request.rounds} de ${NegotiationRules.MAX_ROUNDS}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(Format.pesos(request.currentPrice), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF9A3411))
                    Text("c/u", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Terracotta),
                ) { Text("Aceptar ${Format.pesos(request.currentPrice * request.portions)}", fontWeight = FontWeight.Bold) }
                if (canCounter) {
                    OutlinedButton(
                        onClick = { countering = !countering },
                        modifier = Modifier.weight(1f).height(48.dp),
                        border = BorderStroke(2.dp, Forest),
                    ) { Text("Contraofertar", fontWeight = FontWeight.Bold, color = Forest) }
                }
                OutlinedButton(onClick = onReject, modifier = Modifier.height(48.dp)) { Text("✕") }
            }
            if (countering && canCounter) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newPrice,
                        onValueChange = { newPrice = it.filter(Char::isDigit) },
                        label = { Text("Precio por porción (múltiplo de $100)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedButton(
                        onClick = { parsedPrice?.let(onCounteroffer) },
                        enabled = parsedPrice != null,
                        modifier = Modifier.height(56.dp),
                    ) { Text("Enviar") }
                }
            }
        }
    }
}

@Composable
private fun DeliveryCard(order: OrderView, onMarkReady: () -> Unit, onValidate: () -> Unit) {
    val name = buyerOf(order.buyerName)
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, Forest),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Avatar(name, Forest, Color.White)
                Column {
                    Text("$name · ${portionsLabel(order.portions)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "${Format.orderStatus(order.status)} · acordaron ${Format.pesos(order.agreedPrice)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (order.status == ACCEPTED) {
                    OutlinedButton(onClick = onMarkReady, modifier = Modifier.weight(1f).height(48.dp), border = BorderStroke(2.dp, Forest)) {
                        Text("Marcar listo", fontWeight = FontWeight.Bold, color = Forest)
                    }
                }
                Button(
                    onClick = onValidate,
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Forest),
                ) { Text("Validar código", fontWeight = FontWeight.Bold) }
            }
        }
    }
}
