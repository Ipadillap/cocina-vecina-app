package cl.udec.cocinavecina.ui.orders

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.udec.cocinavecina.AppContainer
import cl.udec.cocinavecina.data.network.OrderView
import cl.udec.cocinavecina.data.session.SessionState
import cl.udec.cocinavecina.ui.common.ErrorMessage
import cl.udec.cocinavecina.ui.common.Format
import cl.udec.cocinavecina.ui.common.Loading
import cl.udec.cocinavecina.ui.common.PrimaryButton
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

// ---------------------------------------------------------------------------
// MOCK DATA — poner en false cuando el backend devuelva el pedido real
// ---------------------------------------------------------------------------
private const val USE_MOCK_DATA = true

/** Código correcto en modo mock (cualquier otro muestra error). */
private const val MOCK_VALID_CODE = "123456"

private data class MockOrder(
    val id: Long,
    val portions: Int,
    val agreedPrice: Int,
    val status: String,
    val buyerId: Long,
    val buyerName: String = "Rosa",
)

private fun mockOrder(orderId: Long) = MockOrder(
    id = if (orderId > 0) orderId else 42L,
    portions = 1,
    agreedPrice = 3300,
    status = "READY_FOR_PICKUP",
    buyerId = 99L,
    buyerName = "Rosa",
)
// ---------------------------------------------------------------------------

private val TIMELINE = listOf("ACCEPTED", "READY_FOR_PICKUP", "DELIVERED")

private val SuccessGreen = Color(0xFF1E4034)
private val ErrorBg = Color(0xFFF6E3D9)
private val ErrorFg = Color(0xFF7E2A0D)
private val DigitBorder = Color(0xFFD5DCD3)
private val DigitBorderFocus = Color(0xFF1E4034)

@Composable
fun OrderScreen(
    container: AppContainer,
    session: SessionState.Active,
    orderId: Long,
    onBack: () -> Unit = {},
) {
    if (USE_MOCK_DATA) {
        MockOrderScreen(session = session, orderId = orderId, onBack = onBack)
        return
    }

    val vm: OrderViewModel = viewModel(key = "order$orderId") { OrderViewModel(container, orderId) }
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val order = uiState.order ?: return Loading()
    val isBuyer = order.buyerId == session.userId

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader(
            title = "Pedido ${order.id}",
            onBack = onBack,
        )
        Text("${order.portions} porción(es) · ${Format.pesos(order.agreedPrice)} c/u · pago en persona")
        StatusTimeline(status = order.status)
        ErrorMessage(uiState.error)
        if (isBuyer) {
            BuyerView(vm, uiState, order)
        } else {
            SellerView(vm, uiState, order)
        }
    }
}

@Composable
private fun ScreenHeader(title: String, onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TextButton(onClick = onBack) {
            Text("← Volver")
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Pantalla completa con datos ficticios (no llama al ViewModel ni a la red). */
@Composable
private fun MockOrderScreen(
    session: SessionState.Active,
    orderId: Long,
    onBack: () -> Unit,
) {
    var order by remember { mutableStateOf(mockOrder(orderId)) }
    var showValidateDialog by remember { mutableStateOf(false) }
    var validateError by remember { mutableStateOf<String?>(null) }
    val isBuyer = order.buyerId == session.userId

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader(
            title = "Pedido ${order.id} (mock)",
            onBack = onBack,
        )
        Text(
            text = "${order.portions} porción(es) · ${Format.pesos(order.agreedPrice)} c/u · pago en persona",
        )
        StatusTimeline(status = order.status)

        if (isBuyer) {
            MockBuyerView(order = order)
        } else {
            when (order.status) {
                "ACCEPTED" -> {
                    PrimaryButton("Marcar listo para retiro", {
                        order = order.copy(status = "READY_FOR_PICKUP")
                    })
                }
                "READY_FOR_PICKUP" -> {
                    Text(
                        text = "Confirmar entrega",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Pídele a ${order.buyerName} su código de 6 números y escríbelo aquí.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Código de prueba: $MOCK_VALID_CODE",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )

                    PrimaryButton("Validar código de entrega", {
                        validateError = null
                        showValidateDialog = true
                    })

                    OutlinedButton(
                        onClick = { /* mock: sin scanner real */ },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Escanear QR (mock)")
                    }
                }
                "DELIVERED" -> {
                    DeliveryConfirmedCard(
                        orderId = order.id,
                        agreedPrice = order.agreedPrice,
                    )
                }
            }
        }
    }

    if (showValidateDialog) {
        ValidateCodeDialog(
            orderId = order.id,
            portions = order.portions,
            agreedPrice = order.agreedPrice,
            buyerName = order.buyerName,
            errorMessage = validateError,
            isDelivered = order.status == "DELIVERED",
            onDismiss = {
                showValidateDialog = false
                validateError = null
            },
            onConfirm = { code ->
                if (code == MOCK_VALID_CODE) {
                    order = order.copy(status = "DELIVERED")
                    validateError = null
                } else {
                    validateError =
                        "Ese código no coincide. Pídele a ${order.buyerName} que te lo muestre de nuevo."
                }
            },
        )
    }
}

@Composable
private fun MockBuyerView(order: MockOrder) {
    val inProgress = order.status == "ACCEPTED" || order.status == "READY_FOR_PICKUP"
    if (inProgress) {
        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Muestra este código al retirar")
                Text(
                    text = MOCK_VALID_CODE.chunked(3).joinToString(" "),
                    fontSize = 44.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Válido hasta el fin de la ventana de retiro",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Text("Dirección: Av. Ejemplo 123, Concepción")
        Text("Teléfono: +56 9 1234 5678")
    }
    if (order.status == "DELIVERED") {
        DeliveryConfirmedCard(orderId = order.id, agreedPrice = order.agreedPrice)
    }
}

@Composable
private fun StatusTimeline(status: String) {
    val current = TIMELINE.indexOf(status)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TIMELINE.forEachIndexed { i, e ->
            val color =
                if (i <= current) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline
            Text(
                Format.orderStatus(e),
                color = color,
                fontWeight = if (i == current) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
    if (current < 0) {
        Text("Estado: ${Format.orderStatus(status)}", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun BuyerView(vm: OrderViewModel, uiState: OrderViewModel.UiState, p: OrderView) {
    val inProgress = p.status == "ACCEPTED" || p.status == "READY_FOR_PICKUP"
    LaunchedEffect(p.id, inProgress) {
        if (inProgress) vm.loadCode()
    }
    uiState.code?.takeIf { inProgress }?.let { code ->
        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Muestra este código al retirar")
                Text(
                    code.chunked(3).joinToString(" "),
                    fontSize = 44.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                )
                val qr = remember(code) { QrCode.image(QrCode.text(p.id, code)) }
                Image(qr, contentDescription = "Código QR", modifier = Modifier.size(200.dp))
                Text(
                    "Válido hasta el fin de la ventana de retiro",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
    if (inProgress) {
        val r = uiState.pickup
        if (r == null) {
            OutlinedButton(onClick = { vm.showPickupDetails() }) {
                Text("Ver dirección y teléfono")
            }
        } else {
            Text("Dirección: ${r.address}")
            r.instructions?.let { Text("Indicaciones: $it") }
            r.phone?.let { Text("Teléfono: $it") }
        }
    }
    if (p.status == "ACCEPTED") CancelSection(vm)
}

@Composable
private fun SellerView(vm: OrderViewModel, uiState: OrderViewModel.UiState, p: OrderView) {
    val context = LocalContext.current
    var showValidateDialog by remember { mutableStateOf(false) }

    when (p.status) {
        "ACCEPTED" -> {
            PrimaryButton("Marcar listo para retiro", { vm.markReady() })
            CancelSection(vm)
        }
        "READY_FOR_PICKUP" -> {
            Text(
                text = "Confirmar entrega",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Pídele al comprador su código de 6 números y escríbelo aquí, o escanea el QR.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            PrimaryButton("Validar código de entrega", { showValidateDialog = true })

            OutlinedButton(
                onClick = {
                    val options = GmsBarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                        .build()
                    GmsBarcodeScanning.getClient(context, options).startScan()
                        .addOnSuccessListener { scanned ->
                            QrCode.read(scanned.rawValue, p.id)?.let { vm.validate(it) }
                        }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Escanear QR")
            }

            if (showValidateDialog && p.status != "DELIVERED") {
                ValidateCodeDialog(
                    orderId = p.id,
                    portions = p.portions,
                    agreedPrice = p.agreedPrice,
                    buyerName = null,
                    errorMessage = uiState.error,
                    isDelivered = false,
                    onDismiss = { showValidateDialog = false },
                    onConfirm = { code -> vm.validate(code) },
                )
            }
        }
        "DELIVERED" -> {
            DeliveryConfirmedCard(orderId = p.id, agreedPrice = p.agreedPrice)
        }
        else -> Unit
    }

    if (showValidateDialog && p.status == "DELIVERED") {
        ValidateCodeDialog(
            orderId = p.id,
            portions = p.portions,
            agreedPrice = p.agreedPrice,
            buyerName = null,
            errorMessage = null,
            isDelivered = true,
            onDismiss = { showValidateDialog = false },
            onConfirm = {},
        )
    }
}

@Composable
private fun ValidateCodeDialog(
    orderId: Long,
    portions: Int,
    agreedPrice: Int,
    buyerName: String?,
    errorMessage: String?,
    isDelivered: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var code by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val who = buyerName ?: "el comprador"

    LaunchedEffect(Unit) {
        if (!isDelivered) focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Confirmar entrega",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Pedido #$orderId · $portions porción(es) · ${Format.pesos(agreedPrice)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (isDelivered) {
                    DeliverySuccessBanner(buyerName = buyerName)
                } else {
                    Text(
                        text = "Pídele a $who su código de 6 números y escríbelo aquí.",
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    Box {
                        BasicTextField(
                            value = code,
                            onValueChange = { input ->
                                code = input.filter(Char::isDigit).take(6)
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword,
                            ),
                            singleLine = true,
                            textStyle = TextStyle(color = Color.Transparent, fontSize = 1.sp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .focusRequester(focusRequester),
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { focusRequester.requestFocus() },
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            for (i in 0 until 3) {
                                DigitBox(
                                    char = code.getOrNull(i)?.toString() ?: "",
                                    focused = code.length == i,
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .width(10.dp)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color(0xFF9FB0A6)),
                            )
                            for (i in 3 until 6) {
                                DigitBox(
                                    char = code.getOrNull(i)?.toString() ?: "",
                                    focused = code.length == i,
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it.filter(Char::isDigit).take(6) },
                        label = { Text("Código de 6 dígitos del comprador") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    errorMessage?.let { msg ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(ErrorBg)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = msg,
                                color = ErrorFg,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isDelivered) {
                TextButton(onClick = onDismiss) { Text("Listo") }
            } else {
                TextButton(
                    onClick = { onConfirm(code) },
                    enabled = code.length == 6,
                ) {
                    Text("Confirmar")
                }
            }
        },
        dismissButton = {
            if (!isDelivered) {
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        },
    )
}

@Composable
private fun DigitBox(char: String, focused: Boolean) {
    Box(
        modifier = Modifier
            .size(width = 44.dp, height = 56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(
                width = 2.dp,
                color = if (focused) DigitBorderFocus else DigitBorder,
                shape = RoundedCornerShape(14.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = char,
            fontFamily = FontFamily.Monospace,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DeliverySuccessBanner(buyerName: String?) {
    val who = buyerName ?: "el comprador"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SuccessGreen)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Text("✓", color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Column {
            Text(
                text = "Entrega confirmada",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = "El pedido de $who quedó como Entregado.",
                color = Color(0xFFDCE7DF),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun DeliveryConfirmedCard(orderId: Long, agreedPrice: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SuccessGreen),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Text("✓", color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            Column {
                Text(
                    text = "Entrega confirmada",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Pedido #$orderId · ${Format.pesos(agreedPrice)}",
                    color = Color(0xFFDCE7DF),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun CancelSection(vm: OrderViewModel) {
    var reason by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    if (!expanded) {
        TextButton(onClick = { expanded = true }) {
            Text("Cancelar pedido")
        }
    } else {
        OutlinedTextField(
            value = reason,
            onValueChange = { reason = it.take(200) },
            label = { Text("Motivo (obligatorio)") },
            modifier = Modifier.fillMaxWidth(),
        )
        TextButton(
            onClick = { vm.cancel(reason) },
            enabled = reason.isNotBlank(),
        ) {
            Text("Confirmar cancelación")
        }
    }
}
