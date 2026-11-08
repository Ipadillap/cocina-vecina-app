package cl.udec.cocinavecina.ui.orders

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.udec.cocinavecina.AppContainer
import cl.udec.cocinavecina.data.network.OrderView
import cl.udec.cocinavecina.data.session.SessionState
import cl.udec.cocinavecina.ui.common.PrimaryButton
import cl.udec.cocinavecina.ui.common.Loading
import cl.udec.cocinavecina.ui.common.Format
import cl.udec.cocinavecina.ui.common.ErrorMessage
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

private val TIMELINE = listOf("ACCEPTED", "READY_FOR_PICKUP", "DELIVERED")

@Composable
fun OrderScreen(container: AppContainer, session: SessionState.Active, orderId: Long) {
    val vm: OrderViewModel = viewModel(key = "order$orderId") { OrderViewModel(container, orderId) }
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val order = uiState.order ?: return Loading()
    val isBuyer = order.buyerId == session.userId

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Pedido ${order.id}", style = MaterialTheme.typography.headlineSmall)
        Text("${order.portions} porción(es) · ${Format.pesos(order.agreedPrice)} c/u · pago en persona")
        StatusTimeline(order)
        ErrorMessage(uiState.error)
        if (isBuyer) BuyerView(vm, uiState, order) else SellerView(vm, order)
    }
}

@Composable
private fun StatusTimeline(p: OrderView) {
    val current = TIMELINE.indexOf(p.status)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TIMELINE.forEachIndexed { i, e ->
            val color = if (i <= current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            Text(Format.orderStatus(e), color = color, fontWeight = if (i == current) FontWeight.Bold else FontWeight.Normal)
        }
    }
    if (current < 0) Text("Estado: ${Format.orderStatus(p.status)}", style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun BuyerView(vm: OrderViewModel, uiState: OrderViewModel.UiState, p: OrderView) {
    val inProgress = p.status == "ACCEPTED" || p.status == "READY_FOR_PICKUP"
    LaunchedEffect(p.id, inProgress) { if (inProgress) vm.loadCode() }
    uiState.code?.takeIf { inProgress }?.let { code ->
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Muestra este código al retirar")
                Text(code.chunked(3).joinToString(" "), fontSize = 44.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                val qr = remember(code) { QrCode.image(QrCode.text(p.id, code)) }
                Image(qr, contentDescription = "Código QR", modifier = Modifier.size(200.dp))
                Text("Válido hasta el fin de la ventana de retiro", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    if (inProgress) {
        val r = uiState.pickup
        if (r == null) {
            OutlinedButton(onClick = { vm.showPickupDetails() }) { Text("Ver dirección y teléfono") }
        } else {
            Text("Dirección: ${r.address}")
            r.instructions?.let { Text("Indicaciones: $it") }
            r.phone?.let { Text("Teléfono: $it") }
        }
    }
    if (p.status == "ACCEPTED") CancelSection(vm)
}

@Composable
private fun SellerView(vm: OrderViewModel, p: OrderView) {
    val context = LocalContext.current
    var code by remember { mutableStateOf("") }
    when (p.status) {
        "ACCEPTED" -> {
            PrimaryButton("Marcar listo para retiro", { vm.markReady() })
            CancelSection(vm)
        }
        "READY_FOR_PICKUP" -> {
            OutlinedTextField(
                value = code,
                onValueChange = { code = it.filter(Char::isDigit).take(6) },
                label = { Text("Código de 6 dígitos del comprador") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            PrimaryButton("Validar entrega", { vm.validate(code) }, enabled = code.length == 6)
            OutlinedButton(onClick = {
                val options = GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
                GmsBarcodeScanning.getClient(context, options).startScan()
                    .addOnSuccessListener { scanned -> QrCode.read(scanned.rawValue, p.id)?.let { vm.validate(it) } }
            }) { Text("Escanear QR") }
        }
        else -> Unit
    }
}

@Composable
private fun CancelSection(vm: OrderViewModel) {
    var reason by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    if (!expanded) {
        TextButton(onClick = { expanded = true }) { Text("Cancelar pedido") }
    } else {
        OutlinedTextField(reason, { reason = it.take(200) }, label = { Text("Motivo (obligatorio)") }, modifier = Modifier.fillMaxWidth())
        TextButton(onClick = { vm.cancel(reason) }, enabled = reason.isNotBlank()) { Text("Confirmar cancelación") }
    }
}
