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

@Composable
fun ProposePriceRoute(
    repository: Repository,
    offerId: Long,
    initialPortions: Int,
    onBack: () -> Unit,
    onRequestCreated: (requestId: Long) -> Unit,
) {
    val viewModel: ProposePriceViewModel =
        viewModel(
            key = "propose-price-$offerId",
            factory =
                viewModelFactory {
                    initializer {
                        ProposePriceViewModel(
                            offerId = offerId,
                            initialPortions = initialPortions,
                            loadOffer = { id -> repository.call { offer(id) } },
                            submitRequest = { id, portions, unitPrice ->
                                repository.call { submit(id, NewPurchaseRequest(portions, unitPrice)) }
                            },
                        )
                    }
                },
        )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.createdRequestId) {
        state.createdRequestId?.let { requestId ->
            viewModel.onNavigationHandled()
            onRequestCreated(requestId)
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
