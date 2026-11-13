package cl.udec.cocinavecina.ui.orders

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.udec.cocinavecina.data.Repository
import cl.udec.cocinavecina.data.network.Cancellation
import cl.udec.cocinavecina.data.network.CodeValidation
import cl.udec.cocinavecina.data.network.UserEvent
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter

private const val REFRESH_INTERVAL_MILLIS = 15_000L
private const val ORDER_ENTITY = "order"

@Composable
fun OrderDetailRoute(
    repository: Repository,
    events: Flow<UserEvent>,
    userId: Long,
    orderId: Long,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: OrderDetailViewModel =
        viewModel(
            key = "order-detail-$orderId",
            factory =
                viewModelFactory {
                    initializer {
                        OrderDetailViewModel(
                            orderId = orderId,
                            userId = userId,
                            loadOrder = { id -> repository.call { order(id) } },
                            loadOffer = { id -> repository.call { offer(id) } },
                            readStoredCode = { id -> repository.session.code(id) },
                            issueCode = { id -> repository.call { issueCode(id) } },
                            storeCode = { id, code -> repository.session.saveCode(id, code) },
                            loadPickup = { id -> repository.call { pickupDetails(id) } },
                            markReady = { id -> repository.call { markReady(id) } },
                            validateDelivery = { id, code -> repository.call { validateDelivery(id, CodeValidation(code)) } },
                            cancelOrder = { id, reason -> repository.call { cancel(id, Cancellation(reason)) } },
                        )
                    }
                },
        )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        if (viewModel.uiState.value.order != null) viewModel.refresh()
        while (true) {
            delay(REFRESH_INTERVAL_MILLIS)
            viewModel.onTick()
            viewModel.refresh()
        }
    }
    LaunchedEffect(Unit) {
        events
            .filter { it.entity == ORDER_ENTITY && it.entityId == orderId }
            .collect { viewModel.refresh() }
    }
    OrderDetailScreen(
        state = state,
        onBack = onBack,
        onRetry = viewModel::onRetry,
        onCallClick = { phone ->
            runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phone.filter { it.isDigit() || it == '+' }}"))) }
        },
        onMarkReadyClick = viewModel::onMarkReadyClick,
        onValidateCode = viewModel::onValidateCode,
        onScanClick = {
            val options = GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
            GmsBarcodeScanning.getClient(context, options).startScan()
                .addOnSuccessListener { scanned -> QrCode.read(scanned.rawValue, orderId)?.let(viewModel::onValidateCode) }
        },
        onCancelClick = viewModel::onCancelClick,
        onCancelDismiss = viewModel::onCancelDismiss,
        onCancelConfirm = viewModel::onCancelConfirm,
    )
}
