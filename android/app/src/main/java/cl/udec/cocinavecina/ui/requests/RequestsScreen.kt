package cl.udec.cocinavecina.ui.requests

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
import androidx.compose.ui.unit.sp
import cl.udec.cocinavecina.AppContainer
import cl.udec.cocinavecina.data.network.OrderView
import cl.udec.cocinavecina.data.network.Price
import cl.udec.cocinavecina.data.network.PurchaseRequestView
import cl.udec.cocinavecina.data.session.SessionState
import cl.udec.cocinavecina.ui.common.ErrorMessage
import cl.udec.cocinavecina.ui.common.Format
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// MOCK DATA — quitar cuando el backend exponga la oferta activa del vendedor
// ---------------------------------------------------------------------------
private const val USE_MOCK_DATA = true

private data class ActiveOfferMock(
    val title: String,
    val remainingMinutes: Int,
    val closesAtLabel: String,
    val reservedPortions: Int,
    val availablePortions: Int,
    val totalPortions: Int,
)

private val mockActiveOffer = ActiveOfferMock(
    title = "Charquicán con huevo frito",
    remainingMinutes = 47,
    closesAtLabel = "hasta 14:10",
    reservedPortions = 1,
    availablePortions = 3,
    totalPortions = 4,
)

/** Datos de ejemplo solo para previsualizar tarjetas cuando la API aún no responde. */
private fun mockReceivedRequests(): List<PurchaseRequestView> = emptyList()
// Si PurchaseRequestView es una data class con constructor público, se pueden
// rellenar aquí. Por ahora la UI de oferta activa es lo principal del mock.
// ---------------------------------------------------------------------------

@Composable
fun RequestsScreen(
    container: AppContainer,
    session: SessionState.Active,
    onOpenOrder: (Long) -> Unit,
) {
    val repository = container.repository
    var receivedRequests by remember { mutableStateOf<List<PurchaseRequestView>>(emptyList()) }
    var ownRequests by remember { mutableStateOf<List<PurchaseRequestView>>(emptyList()) }
    var pendingDeliveries by remember { mutableStateOf<List<OrderView>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var refreshVersion by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    // MOCK: oferta activa visible para el vendedor
    val activeOffer: ActiveOfferMock? = if (USE_MOCK_DATA && session.isSeller) mockActiveOffer else null

    LaunchedEffect(refreshVersion) {
        if (session.isSeller) {
            repository.call { pendingRequests() }.onSuccess { receivedRequests = it }
            repository.call { myOrders() }.onSuccess { orders ->
                pendingDeliveries = orders.filter { (it.deliveredAt == null) && (it.status != "CANCELLED") }
            }
        }
        repository.call { myRequests() }.onSuccess { ownRequests = it }.onFailure { errorMessage = it.message }
    }

    LaunchedEffect(Unit) {
        container.events.events.collect { event ->
            if (event.entity == "purchase_request" || event.entity == "order") {
                refreshVersion++
            }
        }
    }

    fun executeAction(block: suspend () -> Result<Any?>) {
        scope.launch {
            block().onSuccess { errorMessage = null }.onFailure { errorMessage = it.message }
            refreshVersion++
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "Mi oferta y solicitudes",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }

        item {
            ErrorMessage(errorMessage)
        }

        // Oferta activa (mock o futura API)
        if (activeOffer != null) {
            item {
                ActiveOfferCard(
                    offer = activeOffer,
                    onCloseEarly = {
                        // TODO: conectar con endpoint de cerrar oferta
                    },
                )
            }
        }

        if (session.isSeller && pendingDeliveries.isNotEmpty()) {
            item {
                SectionTitle(text = "Por entregar · ${pendingDeliveries.size}")
            }
            items(pendingDeliveries, key = { "order_${it.id}" }) { order ->
                DeliveryCard(
                    order = order,
                    onOpenOrder = onOpenOrder,
                )
            }
        }

        // MOCK: tarjeta de entrega de ejemplo si no hay datos reales
        if (USE_MOCK_DATA && session.isSeller && pendingDeliveries.isEmpty()) {
            item {
                SectionTitle(text = "Por entregar · 1")
            }
            item {
                MockDeliveryCard(onOpenOrder = { onOpenOrder(0L) })
            }
        }

        if (session.isSeller) {
            item {
                SectionTitle(
                    text = if (receivedRequests.isEmpty() && !USE_MOCK_DATA) {
                        "Solicitudes recibidas"
                    } else {
                        val count = if (USE_MOCK_DATA && receivedRequests.isEmpty()) 2 else receivedRequests.size
                        "Solicitudes recibidas · $count"
                    },
                )
            }
            if (receivedRequests.isEmpty() && !USE_MOCK_DATA) {
                item {
                    EmptyHint(text = "Aún no tienes solicitudes pendientes")
                }
            } else if (receivedRequests.isEmpty() && USE_MOCK_DATA) {
                item {
                    MockRequestCard(
                        buyerName = "Camila",
                        portions = 2,
                        roundLabel = "Ronda 1 de 3 · hace 4 min",
                        unitPrice = 3000,
                        totalLabel = "Aceptar $6.000",
                        showCounteroffer = true,
                        onAccept = {},
                        onReject = {},
                    )
                }
                item {
                    MockRequestCard(
                        buyerName = "Tomás",
                        portions = 1,
                        roundLabel = "Compra al precio publicado · hace 1 min",
                        unitPrice = 3500,
                        totalLabel = "Aceptar $3.500",
                        showCounteroffer = false,
                        onAccept = {},
                        onReject = {},
                    )
                }
            } else {
                items(receivedRequests, key = { "received_${it.id}" }) { request ->
                    RequestCard(
                        request = request,
                        isSeller = true,
                        onAccept = { executeAction { repository.call { accept(request.id) } } },
                        onReject = { executeAction { repository.call { reject(request.id) } } },
                        onCounteroffer = { price ->
                            executeAction { repository.call { counteroffer(request.id, Price(price)) } }
                        },
                        onOpenOrder = onOpenOrder,
                    )
                }
            }
        }

        item {
            SectionTitle(
                text = if (ownRequests.isEmpty()) {
                    "Mis solicitudes"
                } else {
                    "Mis solicitudes · ${ownRequests.size}"
                },
            )
        }
        if (ownRequests.isEmpty()) {
            item {
                EmptyHint(text = "No has enviado solicitudes todavía")
            }
        } else {
            items(ownRequests, key = { "own_${it.id}" }) { request ->
                RequestCard(
                    request = request,
                    isSeller = false,
                    onAccept = { executeAction { repository.call { accept(request.id) } } },
                    onReject = { executeAction { repository.call { reject(request.id) } } },
                    onCounteroffer = { price ->
                        executeAction { repository.call { propose(request.id, Price(price)) } }
                    },
                    onOpenOrder = onOpenOrder,
                )
            }
        }

        // MOCK: resumen de historial del mes (solo visual)
        if (USE_MOCK_DATA && session.isSeller) {
            item {
                HistorySummaryCard(
                    monthLabel = "Octubre",
                    deliveredCount = 9,
                    totalAmountLabel = "$27.600 en montos acordados",
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

/** Tarjeta oscura de oferta activa, alineada al mockup. */
@Composable
private fun ActiveOfferCard(
    offer: ActiveOfferMock,
    onCloseEarly: () -> Unit,
) {
    val darkBg = Color(0xFF13241E)
    val accent = Color(0xFFF28B5E)
    val muted = Color(0xFFA9BDB3)
    val progress = 1f - (offer.remainingMinutes / 120f).coerceIn(0.05f, 0.95f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = darkBg),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ACTIVA",
                        style = MaterialTheme.typography.labelSmall,
                        color = muted,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = offer.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
                OutlinedButton(
                    onClick = onCloseEarly,
                    border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.45f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                ) {
                    Text("Cerrar antes", fontWeight = FontWeight.Bold)
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.18f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(3.dp))
                            .background(accent),
                    )
                }
                Text(
                    text = "${offer.remainingMinutes} min · ${offer.closesAtLabel}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFB08F),
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                repeat(offer.totalPortions) { index ->
                    val filled = index < offer.reservedPortions
                    Box(
                        modifier = Modifier
                            .width(26.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (filled) accent else Color.White.copy(alpha = 0.85f),
                            ),
                    )
                }
                Text(
                    text = "${offer.reservedPortions} reservada · ${offer.availablePortions} disponibles",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFC9D6CF),
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun MockRequestCard(
    buyerName: String,
    portions: Int,
    roundLabel: String,
    unitPrice: Int,
    totalLabel: String,
    showCounteroffer: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit,
) {
    val initial = buyerName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDCE7DF)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = initial,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E4034),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$buyerName · $portions porción${if (portions == 1) "" else "es"}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = roundLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = Format.pesos(unitPrice),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF9A3411),
                    )
                    if (showCounteroffer) {
                        Text(
                            text = "c/u · propone",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Button(
                    onClick = onAccept,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB8431A)),
                ) {
                    Text(totalLabel, fontWeight = FontWeight.Bold)
                }
                if (showCounteroffer) {
                    OutlinedButton(
                        onClick = { /* mock: solo visual */ },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        border = BorderStroke(2.dp, Color(0xFF1E4034)),
                    ) {
                        Text("Contraofertar", fontWeight = FontWeight.Bold, color = Color(0xFF1E4034))
                    }
                }
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.height(48.dp),
                ) {
                    Text("✕")
                }
            }
        }
    }
}

@Composable
private fun MockDeliveryCard(onOpenOrder: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, Color(0xFF1E4034)),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E4034)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "R", fontWeight = FontWeight.Bold, color = Color.White)
                }
                Column {
                    Text(
                        text = "Rosa · 1 porción",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Aceptado · acordaron $3.300",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                OutlinedButton(
                    onClick = { /* mock */ },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    border = BorderStroke(2.dp, Color(0xFF1E4034)),
                ) {
                    Text("Marcar listo", fontWeight = FontWeight.Bold, color = Color(0xFF1E4034))
                }
                Button(
                    onClick = onOpenOrder,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E4034)),
                ) {
                    Text("Validar código", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun HistorySummaryCard(
    monthLabel: String,
    deliveredCount: Int,
    totalAmountLabel: String,
) {
    OutlinedButton(
        onClick = { /* TODO: historial */ },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        border = BorderStroke(1.dp, Color(0xFF9FB0A6)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = "$monthLabel: $deliveredCount pedidos entregados",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "$totalAmountLabel · ver historial",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DeliveryCard(
    order: OrderView,
    onOpenOrder: (Long) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = 1.5.dp,
            color = MaterialTheme.colorScheme.primary,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Pedido #${order.id} · ${order.portions} porción(es)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Aceptado · acordaron ${Format.pesos(order.agreedPrice)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = { onOpenOrder(order.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text("Validar código de entrega")
            }
        }
    }
}

@Composable
private fun RequestCard(
    request: PurchaseRequestView,
    isSeller: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onCounteroffer: (Int) -> Unit,
    onOpenOrder: (Long) -> Unit,
) {
    val myTurn = request.turn == (if (isSeller) "SELLER" else "BUYER")
    var newPriceText by remember(request.id) { mutableStateOf("") }
    val totalPrice = request.currentPrice * request.portions
    val canCounteroffer = myTurn && request.rounds < 3

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Oferta #${request.offerId} · ${request.portions} porción(es)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${Format.requestStatus(request.status)} · Ronda ${request.rounds} de 3",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = Format.pesos(request.currentPrice),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "c/u",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            request.closeReason?.let { reason ->
                Text(
                    text = "Motivo: $reason",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (myTurn) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = onAccept,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                    ) {
                        Text("Aceptar ${Format.pesos(totalPrice)}")
                    }
                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier.height(48.dp),
                    ) {
                        Text("Rechazar")
                    }
                }

                if (canCounteroffer) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = newPriceText,
                            onValueChange = { newPriceText = it.filter(Char::isDigit) },
                            label = {
                                Text(if (isSeller) "Contraofertar (precio c/u)" else "Nuevo precio (c/u)")
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedButton(
                            onClick = { newPriceText.toIntOrNull()?.let(onCounteroffer) },
                            enabled = newPriceText.isNotBlank(),
                            modifier = Modifier.height(56.dp),
                        ) {
                            Text("Enviar")
                        }
                    }
                }
            } else if (request.turn != null) {
                Text(
                    text = "Esperando respuesta de la otra parte",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            request.orderId?.let { orderId ->
                Button(
                    onClick = { onOpenOrder(orderId) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                    ),
                ) {
                    Text("Ver pedido")
                }
            }
        }
    }
}
