package cl.udec.cocinavecina.ui.orders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.udec.cocinavecina.data.network.OfferView
import cl.udec.cocinavecina.data.network.OrderView
import cl.udec.cocinavecina.data.network.PickupDetailsView
import cl.udec.cocinavecina.ui.common.Format
import cl.udec.cocinavecina.ui.theme.CocinaVecinaTheme
import java.time.Instant
import java.time.temporal.ChronoUnit

private val ScreenPadding = 16.dp
private const val MAX_REASON_LENGTH = 200

@Composable
fun OrderDetailScreen(
    state: OrderDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onCallClick: (phone: String) -> Unit,
    onMarkReadyClick: () -> Unit,
    onCodeInputChange: (code: String) -> Unit,
    onValidateClick: () -> Unit,
    onScanClick: () -> Unit,
    onCancelClick: () -> Unit,
    onCancelDismiss: () -> Unit,
    onCancelConfirm: (reason: String) -> Unit,
) {
    val order = state.order
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, top = 4.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = MaterialTheme.colorScheme.onBackground)
            }
        }
        when {
            order != null ->
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(start = ScreenPadding, end = ScreenPadding, bottom = ScreenPadding),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Header(state, order)
                    StepProgress(state)
                    if (state.isBuyer) {
                        BuyerContent(state, order, onCallClick)
                    } else {
                        SellerContent(state, order, onMarkReadyClick, onCodeInputChange, onValidateClick, onScanClick)
                    }
                    state.actionError?.takeIf { !state.isValidatingStep }?.let {
                        Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                    }
                    if (state.canCancel || state.pendingAction == OrderAction.CANCEL) {
                        CancelButton(state, onCancelClick)
                    }
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
                    Text(state.loadError ?: OrderDetailViewModel.LOAD_ERROR, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    TextButton(onClick = onRetry) { Text("Reintentar", fontWeight = FontWeight.SemiBold) }
                }
        }
    }
    if (state.isCancelDialogVisible) {
        CancelDialog(state, onCancelDismiss, onCancelConfirm)
    }
}

@Composable
private fun Header(
    state: OrderDetailUiState,
    order: OrderView,
) {
    val title =
        when (order.status) {
            ACCEPTED_STATUS -> "Pedido aceptado"
            READY_STATUS -> "Listo para retiro"
            DELIVERED_STATUS -> "Pedido entregado"
            else -> "Pedido ${Format.orderStatus(order.status).lowercase()}"
        }
    val portions = if (order.portions == 1) "1 porción" else "${order.portions} porciones"
    val details = listOfNotNull(state.offer?.title, portions, Format.pesos(state.total)).joinToString(" · ")
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
        Text(details, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StepProgress(state: OrderDetailUiState) {
    val current = state.currentStep
    val steps = OrderStep.entries
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                steps.forEachIndexed { index, _ ->
                    val reached = current >= index
                    Box(
                        Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (reached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (current > index || (reached && index == steps.lastIndex)) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text(
                                "${index + 1}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (reached) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (index < steps.lastIndex) {
                        Box(
                            Modifier
                                .weight(1f)
                                .padding(horizontal = 6.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (current > index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                        )
                    }
                }
            }
            Row {
                steps.forEachIndexed { index, step ->
                    Text(
                        step.label,
                        modifier = Modifier.weight(1f),
                        fontSize = 12.sp,
                        fontWeight = if (index == current) FontWeight.Bold else FontWeight.Normal,
                        color = if (current >= index) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign =
                            when (index) {
                                0 -> TextAlign.Start
                                steps.lastIndex -> TextAlign.End
                                else -> TextAlign.Center
                            },
                    )
                }
            }
            if (current < 0) {
                Text(
                    "Este pedido quedó ${Format.orderStatus(state.order?.status.orEmpty()).lowercase()}.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

@Composable
private fun BuyerContent(
    state: OrderDetailUiState,
    order: OrderView,
    onCallClick: (String) -> Unit,
) {
    if (state.isInProgress) {
        CodeCard(state, order)
    }
    if (order.status == DELIVERED_STATUS) {
        InfoStrip(Icons.Filled.CheckCircle, "¡Listo! Retiraste tu pedido. Que lo disfrutes.", highlighted = false)
    }
    state.pickup?.takeIf { state.isPickupUnlocked }?.let { pickup ->
        UnlockedCard(pickup, onCallClick)
    }
    if (state.isInProgress) {
        InfoStrip(
            Icons.Filled.Info,
            "Retira antes de las ${Format.clockTime(order.windowEnd)} · ${Format.duration(state.minutesToPickupDeadline)}",
            highlighted = true,
        )
        InfoStrip(Icons.Filled.ShoppingCart, "Pagas ${Format.pesos(state.total)} en persona, al retirar", highlighted = false)
    }
}

@Composable
private fun CodeCard(
    state: OrderDetailUiState,
    order: OrderView,
) {
    var showQr by rememberSaveable { mutableStateOf(false) }
    val code = state.code
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Código de entrega", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (code == null) {
                CircularProgressIndicator(Modifier.padding(12.dp).size(32.dp))
                return@Column
            }
            Text(
                code.chunked(3).joinToString(" "),
                fontSize = 44.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "Muéstraselo a ${state.sellerName} al retirar",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            if (showQr) {
                val qr = remember(code, order.id) { QrCode.image(QrCode.text(order.id, code)) }
                Image(qr, contentDescription = "Código QR de entrega", modifier = Modifier.padding(top = 4.dp).size(200.dp))
            }
            TextButton(onClick = { showQr = !showQr }) {
                Text(if (showQr) "Ocultar QR" else "¿Mejor escanear? Mostrar QR", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun UnlockedCard(
    pickup: PickupDetailsView,
    onCallClick: (String) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.tertiary)
                Text("Desbloqueado para ti", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            DetailRow(Icons.Filled.Place, "Dirección", pickup.address)
            pickup.instructions?.takeIf { it.isNotBlank() }?.let {
                DetailRow(Icons.Filled.Info, "Indicaciones", it)
            }
            pickup.phone?.takeIf { it.isNotBlank() }?.let { phone ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { DetailRow(Icons.Filled.Phone, "Teléfono", phone) }
                    OutlinedButton(
                        onClick = { onCallClick(phone) },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    ) {
                        Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Llamar", modifier = Modifier.padding(start = 6.dp), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                "Estos datos se ocultan de nuevo 1 h después de la entrega o si se cancela el pedido.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.padding(top = 2.dp).size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Column {
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun InfoStrip(
    icon: ImageVector,
    text: String,
    highlighted: Boolean,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (highlighted) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (highlighted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
            )
            Text(
                text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (highlighted) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun SellerContent(
    state: OrderDetailUiState,
    order: OrderView,
    onMarkReadyClick: () -> Unit,
    onCodeInputChange: (String) -> Unit,
    onValidateClick: () -> Unit,
    onScanClick: () -> Unit,
) {
    when (order.status) {
        ACCEPTED_STATUS ->
            SellerCard("Prepara el pedido", "Cuando esté listo, avisa para que pasen a retirarlo antes de las ${Format.clockTime(order.windowEnd)}.") {
                PrimaryActionButton(
                    text = "Marcar listo para retiro",
                    isBusy = state.pendingAction == OrderAction.MARK_READY,
                    enabled = !state.isActing,
                    onClick = onMarkReadyClick,
                )
            }
        READY_STATUS -> CodeValidationCard(state, onCodeInputChange, onValidateClick, onScanClick)
        DELIVERED_STATUS -> InfoStrip(Icons.Filled.CheckCircle, "Entrega confirmada. ¡Gracias por cocinar para el barrio!", highlighted = false)
        else -> Unit
    }
    if (state.isInProgress) {
        InfoStrip(Icons.Filled.ShoppingCart, "Recibes ${Format.pesos(state.total)} en persona, al entregar", highlighted = false)
    }
}

@Composable
private fun SellerCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}

@Composable
private fun CodeValidationCard(
    state: OrderDetailUiState,
    onCodeInputChange: (String) -> Unit,
    onValidateClick: () -> Unit,
    onScanClick: () -> Unit,
) {
    val error = state.actionError.takeIf { state.pendingAction == null }
    SellerCard("Valida la entrega", "Pide el código de 6 dígitos a quien retira, o escanea su QR.") {
        OutlinedTextField(
            value = state.codeInput,
            onValueChange = onCodeInputChange,
            label = { Text("Código de entrega") },
            isError = error != null,
            supportingText = error?.let { message -> { Text(message) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            textStyle = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace, letterSpacing = 4.sp),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        PrimaryActionButton(
            text = "Validar entrega",
            isBusy = state.pendingAction == OrderAction.VALIDATE,
            enabled = state.canValidate,
            onClick = onValidateClick,
        )
        OutlinedButton(
            onClick = onScanClick,
            enabled = !state.isActing,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        ) {
            Text("Escanear QR", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CancelButton(
    state: OrderDetailUiState,
    onCancelClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onCancelClick,
        enabled = state.canCancel,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
    ) {
        if (state.pendingAction == OrderAction.CANCEL) {
            BusyIndicator(MaterialTheme.colorScheme.secondary, "Cancelando")
        } else {
            Text("Cancelar pedido", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
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
private fun CancelDialog(
    state: OrderDetailUiState,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var reason by rememberSaveable { mutableStateOf("") }
    val other = if (state.isBuyer) state.sellerName else "quien compra"
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿Cancelar el pedido?", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Le avisaremos a $other. Cuéntale brevemente por qué.")
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it.take(MAX_REASON_LENGTH) },
                    label = { Text("Motivo") },
                    supportingText = { Text("${reason.length}/$MAX_REASON_LENGTH") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(reason) }, enabled = reason.isNotBlank()) {
                Text("Cancelar pedido", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Volver") }
        },
    )
}

private val OrderDetailUiState.isValidatingStep: Boolean
    get() = !isBuyer && order?.status == READY_STATUS

private fun previewState(
    userId: Long = 1,
    status: String = ACCEPTED_STATUS,
): OrderDetailUiState {
    val now = Instant.now()
    return OrderDetailUiState(
        userId = userId,
        order =
            OrderView(
                id = 5,
                offerId = 9,
                buyerId = 1,
                sellerId = 2,
                agreedPrice = 3300,
                portions = 2,
                status = status,
                acceptedAt = now.minus(5, ChronoUnit.MINUTES).toString(),
                windowEnd = now.plus(71, ChronoUnit.MINUTES).toString(),
            ),
        offer =
            OfferView(
                id = 9,
                sellerId = 2,
                seller = "Marta R.",
                title = "Charquicán",
                category = "MAIN_COURSE",
                basePrice = 3500,
                minPrice = 2800,
                availablePortions = 3,
                allergens = emptyList(),
                preparedAt = now.minus(2, ChronoUnit.HOURS).toString(),
                expiresAt = now.plus(71, ChronoUnit.MINUTES).toString(),
                status = "ACTIVE",
                approxLatitude = -36.82,
                approxLongitude = -73.05,
            ),
        code = "482913",
        pickup = PickupDetailsView("Pje. Los Copihues 482, Concepción", "Timbre del 2.º piso, reja lateral", -36.82, -73.05, "+56 9 8765 0142"),
        now = now,
        isLoading = false,
    )
}

@Preview(showBackground = true, heightDp = 1100, widthDp = 390)
@Composable
private fun OrderDetailBuyerPreview() {
    CocinaVecinaTheme {
        OrderDetailScreen(previewState(), {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, heightDp = 900, widthDp = 390)
@Composable
private fun OrderDetailSellerPreview() {
    CocinaVecinaTheme {
        OrderDetailScreen(previewState(userId = 2, status = READY_STATUS), {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
    }
}
