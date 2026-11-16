package cl.udec.cocinavecina.ui.requests

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.udec.cocinavecina.data.network.OfferView
import cl.udec.cocinavecina.data.network.ProposalView
import cl.udec.cocinavecina.data.network.PurchaseRequestView
import cl.udec.cocinavecina.ui.common.Format
import cl.udec.cocinavecina.ui.common.NegotiationRules
import cl.udec.cocinavecina.ui.components.PriceMark
import cl.udec.cocinavecina.ui.components.PriceScale
import cl.udec.cocinavecina.ui.theme.CocinaVecinaTheme
import java.time.Instant
import java.time.temporal.ChronoUnit

private val ScreenPadding = 16.dp

@Composable
fun RequestTrackingScreen(
    state: RequestTrackingUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onAcceptClick: () -> Unit,
    onProposeAgainClick: () -> Unit,
    onRejectClick: () -> Unit,
    onRejectDismiss: () -> Unit,
    onRejectConfirm: () -> Unit,
    onOpenOrderClick: () -> Unit,
) {
    val request = state.request
    val offer = state.offer
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        TopRow(state, onBack)
        when {
            request != null && offer != null -> {
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(start = ScreenPadding, end = ScreenPadding, bottom = ScreenPadding),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Tu solicitud", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
                        Text("${offer.title} · ${offer.seller}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    StatusCard(state, request)
                    NegotiationTimeline(state, request, offer)
                    RoundsNote(state, request, offer)
                }
                ActionBar(state, request, onAcceptClick, onProposeAgainClick, onRejectClick, onOpenOrderClick)
            }
            state.isLoading ->
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            else ->
                Column(
                    Modifier.weight(1f).fillMaxWidth().padding(ScreenPadding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(state.loadError ?: RequestTrackingViewModel.LOAD_ERROR, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    TextButton(onClick = onRetry) { Text("Reintentar", fontWeight = FontWeight.SemiBold) }
                }
        }
    }
    if (state.isRejectConfirmVisible) {
        RejectDialog(state, onRejectDismiss, onRejectConfirm)
    }
}

@Composable
private fun TopRow(
    state: RequestTrackingUiState,
    onBack: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = ScreenPadding, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = MaterialTheme.colorScheme.onBackground)
        }
        Box(Modifier.weight(1f))
        if (state.request != null && state.isOpen && !state.isExpired) {
            LiveBadge()
            Text(
                "quedan ${Format.duration(state.minutesLeft)}",
                modifier = Modifier.padding(start = 10.dp),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (state.minutesLeft < URGENT_MINUTES) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LiveBadge() {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiary),
        )
        Text("En vivo", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
private fun StatusCard(
    state: RequestTrackingUiState,
    request: PurchaseRequestView,
) {
    val portionsLabel = if (request.portions == 1) "1 porción" else "${request.portions} porciones"
    val (title, subtitle, highlighted) =
        when {
            state.hasSellerCounteroffer ->
                Triple(
                    "${state.sellerName} te contraoferta ${Format.pesos(request.currentPrice)} por porción",
                    "Total ${Format.pesos(state.total)} por $portionsLabel",
                    true,
                )
            state.isBuyerTurn ->
                Triple(
                    "Es tu turno: ${Format.pesos(request.currentPrice)} por porción",
                    "Total ${Format.pesos(state.total)} por $portionsLabel",
                    true,
                )
            state.isWaitingSeller ->
                Triple(
                    "Esperando respuesta de ${state.sellerName}",
                    "Propusiste ${Format.pesos(request.currentPrice)} por porción · Total ${Format.pesos(state.total)} por $portionsLabel",
                    false,
                )
            state.isExpired ->
                Triple("La solicitud expiró", "No hubo acuerdo antes del cierre de la venta.", false)
            request.status == "ACCEPTED" ->
                Triple(
                    "¡Trato cerrado a ${Format.pesos(request.currentPrice)} c/u!",
                    "Total ${Format.pesos(state.total)} por $portionsLabel. Pagas en persona al retirar.",
                    true,
                )
            else ->
                Triple(
                    "Solicitud ${Format.requestStatus(request.status).lowercase()}",
                    request.closeReason ?: "Esta negociación terminó sin acuerdo.",
                    false,
                )
        }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (highlighted) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        border = if (highlighted) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                title,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (highlighted) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                subtitle,
                fontSize = 14.sp,
                color = if (highlighted) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NegotiationTimeline(
    state: RequestTrackingUiState,
    request: PurchaseRequestView,
    offer: OfferView,
) {
    if (request.proposals.isEmpty()) return
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Recorrido del regateo", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(
                request.proposals.joinToString(" → ") { Format.pesos(it.unitPrice) },
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.secondary,
            )
            PriceScale(
                minPrice = offer.minPrice,
                maxPrice = offer.basePrice,
                minLabel = "Mínimo",
                maxLabel = "Publicado",
                marks = latestMarks(state, request.proposals),
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                request.proposals.forEach { proposal -> HistoryRow(state, proposal) }
            }
        }
    }
}

private fun latestMarks(
    state: RequestTrackingUiState,
    proposals: List<ProposalView>,
): List<PriceMark> {
    val lastIndex = proposals.lastIndex
    return listOf(NegotiationRules.BUYER, NegotiationRules.SELLER).mapNotNull { party ->
        val index = proposals.indexOfLast { it.party == party }
        if (index < 0) return@mapNotNull null
        PriceMark(
            price = proposals[index].unitPrice,
            label = if (party == NegotiationRules.BUYER) "tú" else state.sellerName,
            emphasized = index == lastIndex,
        )
    }
}

@Composable
private fun HistoryRow(
    state: RequestTrackingUiState,
    proposal: ProposalView,
) {
    val isBuyer = proposal.party == NegotiationRules.BUYER
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            Format.clockTime(proposal.createdAt),
            modifier = Modifier.width(44.dp),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (isBuyer) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary),
        )
        Text(
            if (isBuyer) "Tú propusiste ${Format.pesos(proposal.unitPrice)}" else "${state.sellerName} contraofertó ${Format.pesos(proposal.unitPrice)}",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun RoundsNote(
    state: RequestTrackingUiState,
    request: PurchaseRequestView,
    offer: OfferView,
) {
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Ronda ${request.rounds.coerceAtMost(NegotiationRules.MAX_ROUNDS)} de ${NegotiationRules.MAX_ROUNDS} usada",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (state.isOpen) {
                    Text(
                        "Si no hay acuerdo, la solicitud expira sola a las ${Format.clockTime(offer.expiresAt)}.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (state.isBuyerTurn && !state.canProposeAgain) {
                    Text(
                        "Ya usaste las ${NegotiationRules.MAX_ROUNDS} rondas: solo queda aceptar o rechazar.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionBar(
    state: RequestTrackingUiState,
    request: PurchaseRequestView,
    onAcceptClick: () -> Unit,
    onProposeAgainClick: () -> Unit,
    onRejectClick: () -> Unit,
    onOpenOrderClick: () -> Unit,
) {
    val showOrder = request.status == "ACCEPTED" && request.orderId != null
    if (!state.isBuyerTurn && !showOrder) return
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        shadowElevation = 8.dp,
    ) {
        Column(
            Modifier.padding(horizontal = ScreenPadding, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            state.actionError?.let {
                Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
            }
            if (showOrder) {
                PrimaryActionButton("Ver pedido", isBusy = false, enabled = true, onClick = onOpenOrderClick)
                return@Column
            }
            PrimaryActionButton(
                text = "Aceptar ${Format.pesos(request.currentPrice)} c/u",
                isBusy = state.pendingAction == RequestAction.ACCEPT,
                enabled = !state.isActing,
                onClick = onAcceptClick,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onProposeAgainClick,
                    enabled = state.canProposeAgain && !state.isActing,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (state.canProposeAgain) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Text("Proponer otro", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = onRejectClick,
                    enabled = !state.isActing,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
                ) {
                    if (state.pendingAction == RequestAction.REJECT) {
                        BusyIndicator(MaterialTheme.colorScheme.secondary, "Rechazando")
                    } else {
                        Text("Rechazar", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PrimaryActionButton(
    text: String,
    isBusy: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors =
            ButtonDefaults.buttonColors(
                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                disabledContentColor = MaterialTheme.colorScheme.onPrimary,
            ),
    ) {
        if (isBusy) {
            BusyIndicator(MaterialTheme.colorScheme.onPrimary, "Procesando")
        } else {
            Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun BusyIndicator(
    color: Color,
    description: String,
) {
    Box(Modifier.semantics { contentDescription = description }) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = color)
    }
}

@Composable
private fun RejectDialog(
    state: RequestTrackingUiState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿Rechazar la contraoferta?", fontWeight = FontWeight.Bold) },
        text = { Text("La solicitud se cerrará y ${state.sellerName} podrá vender estas porciones a otra persona.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Rechazar", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Seguir negociando") }
        },
    )
}

private const val URGENT_MINUTES = 60L

private fun previewState(): RequestTrackingUiState {
    val now = Instant.now()
    return RequestTrackingUiState(
        request =
            PurchaseRequestView(
                id = 41,
                offerId = 9,
                portions = 2,
                rounds = 1,
                currentPrice = 3300,
                currentParty = "SELLER",
                turn = "BUYER",
                status = "COUNTEROFFER",
                proposals =
                    listOf(
                        ProposalView("BUYER", 3000, now.minus(10, ChronoUnit.MINUTES).toString()),
                        ProposalView("SELLER", 3300, now.minus(5, ChronoUnit.MINUTES).toString()),
                    ),
            ),
        offer =
            OfferView(
                id = 9,
                sellerId = 2,
                seller = "Marta R.",
                title = "Charquicán con huevo frito",
                category = "MAIN_COURSE",
                basePrice = 3500,
                minPrice = 2800,
                availablePortions = 3,
                allergens = listOf("EGG"),
                preparedAt = now.minus(2, ChronoUnit.HOURS).toString(),
                expiresAt = now.plus(41, ChronoUnit.MINUTES).toString(),
                status = "ACTIVE",
                approxLatitude = -36.82,
                approxLongitude = -73.05,
            ),
        now = now,
        isLoading = false,
    )
}

@Preview(showBackground = true, heightDp = 900, widthDp = 390)
@Composable
private fun RequestTrackingScreenPreview() {
    CocinaVecinaTheme {
        RequestTrackingScreen(previewState(), {}, {}, {}, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, heightDp = 900, widthDp = 390)
@Composable
private fun RequestTrackingScreenAcceptedPreview() {
    val state = previewState()
    CocinaVecinaTheme {
        RequestTrackingScreen(
            state = state.copy(request = state.request?.copy(status = "ACCEPTED", turn = null, orderId = 7)),
            onBack = {},
            onRetry = {},
            onAcceptClick = {},
            onProposeAgainClick = {},
            onRejectClick = {},
            onRejectDismiss = {},
            onRejectConfirm = {},
            onOpenOrderClick = {},
        )
    }
}
