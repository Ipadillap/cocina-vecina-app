package cl.udec.cocinavecina.ui.requests

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.udec.cocinavecina.data.Repository
import cl.udec.cocinavecina.data.network.UserEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter

private const val REFRESH_INTERVAL_MILLIS = 15_000L
private const val PURCHASE_REQUEST_ENTITY = "purchase_request"

@Composable
fun RequestTrackingRoute(
    repository: Repository,
    events: Flow<UserEvent>,
    requestId: Long,
    onBack: () -> Unit,
    onProposeAgain: (requestId: Long) -> Unit,
    onOpenOrder: (orderId: Long) -> Unit,
) {
    val viewModel: RequestTrackingViewModel =
        viewModel(
            key = "request-tracking-$requestId",
            factory =
                viewModelFactory {
                    initializer {
                        RequestTrackingViewModel(
                            requestId = requestId,
                            loadRequest = { id -> repository.call { purchaseRequest(id) } },
                            loadOffer = { id -> repository.call { offer(id) } },
                            acceptRequest = { id -> repository.call { accept(id) } },
                            rejectRequest = { id -> repository.call { reject(id) } },
                        )
                    }
                },
        )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        if (viewModel.uiState.value.request != null) viewModel.refresh()
        while (true) {
            delay(REFRESH_INTERVAL_MILLIS)
            viewModel.onTick()
            viewModel.refresh()
        }
    }
    LaunchedEffect(Unit) {
        events
            .filter { it.entity == PURCHASE_REQUEST_ENTITY && it.entityId == requestId }
            .collect { viewModel.refresh() }
    }
    LaunchedEffect(state.openOrderId) {
        state.openOrderId?.let { orderId ->
            viewModel.onOrderNavigationHandled()
            onOpenOrder(orderId)
        }
    }
    RequestTrackingScreen(
        state = state,
        onBack = onBack,
        onRetry = viewModel::onRetry,
        onAcceptClick = viewModel::onAcceptClick,
        onProposeAgainClick = { onProposeAgain(requestId) },
        onRejectClick = viewModel::onRejectClick,
        onRejectDismiss = viewModel::onRejectDismiss,
        onRejectConfirm = viewModel::onRejectConfirm,
        onOpenOrderClick = { state.request?.orderId?.let(onOpenOrder) },
    )
}
