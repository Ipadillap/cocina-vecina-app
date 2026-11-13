package cl.udec.cocinavecina.ui.offer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.udec.cocinavecina.data.Repository
import cl.udec.cocinavecina.data.network.NewPurchaseRequest
import cl.udec.cocinavecina.data.network.Price

@Composable
fun ProposePriceRoute(
    repository: Repository,
    target: ProposalTarget,
    onBack: () -> Unit,
    onRequestCompleted: (requestId: Long) -> Unit,
) {
    val viewModel: ProposePriceViewModel =
        viewModel(
            key = "propose-price-$target",
            factory =
                viewModelFactory {
                    initializer {
                        ProposePriceViewModel(
                            target = target,
                            loadOffer = { id -> repository.call { offer(id) } },
                            loadRequest = { id -> repository.call { purchaseRequest(id) } },
                            submitRequest = { id, portions, unitPrice ->
                                repository.call { submit(id, NewPurchaseRequest(portions, unitPrice)) }
                            },
                            sendProposal = { id, unitPrice -> repository.call { propose(id, Price(unitPrice)) } },
                        )
                    }
                },
        )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.completedRequestId) {
        state.completedRequestId?.let { requestId ->
            viewModel.onNavigationHandled()
            onRequestCompleted(requestId)
        }
    }
    ProposePriceScreen(
        state = state,
        onBack = onBack,
        onRetry = viewModel::onRetry,
        onPriceTextChange = viewModel::onPriceTextChange,
        onIncreasePrice = viewModel::onIncreasePrice,
        onDecreasePrice = viewModel::onDecreasePrice,
        onShortcutSelect = viewModel::onShortcutSelect,
        onPortionsChange = viewModel::onPortionsChange,
        onSubmitClick = viewModel::onSubmitClick,
        onBuyAtPublishedPriceClick = viewModel::onBuyAtPublishedPriceClick,
    )
}
