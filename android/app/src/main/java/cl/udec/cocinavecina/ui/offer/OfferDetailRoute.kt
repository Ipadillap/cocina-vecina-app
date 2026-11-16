package cl.udec.cocinavecina.ui.offer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.udec.cocinavecina.data.ApiError
import cl.udec.cocinavecina.data.Repository
import cl.udec.cocinavecina.data.network.NewPurchaseRequest
import cl.udec.cocinavecina.data.network.NewReport

@Composable
fun OfferDetailRoute(
    repository: Repository,
    offerId: Long,
    distanceMeters: Long?,
    onBack: () -> Unit,
    onProposePriceClick: (offerId: Long, portions: Int) -> Unit,
    onPurchaseRequested: (requestId: Long) -> Unit,
) {
    val viewModel: OfferDetailViewModel =
        viewModel(
            key = "offer-detail-$offerId",
            factory =
                viewModelFactory {
                    initializer {
                        OfferDetailViewModel(
                            offerId = offerId,
                            distanceMeters = distanceMeters,
                            loadOffer = { id -> repository.call { offer(id) } },
                            requestPurchase = { id, portions -> repository.call { submit(id, NewPurchaseRequest(portions)) } },
                            sendReport = { id, reason ->
                                repository.call { report(id, NewReport(reason)) }.mapCatching { response ->
                                    if (!response.isSuccessful) throw ApiError(response.code(), "Error ${response.code()}")
                                }
                            },
                            resolvePhotoUrl = repository::absoluteUrl,
                        )
                    }
                },
        )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.createdRequestId) {
        state.createdRequestId?.let { requestId ->
            viewModel.onPurchaseNavigationHandled()
            onPurchaseRequested(requestId)
        }
    }
    OfferDetailScreen(
        state = state,
        onBack = onBack,
        onRetry = viewModel::onRetry,
        onPortionsChange = viewModel::onPortionsChange,
        onProposePriceClick = { onProposePriceClick(offerId, state.portions) },
        onBuyClick = viewModel::onBuyClick,
        onReportClick = viewModel::onReportClick,
        onReportDismiss = viewModel::onReportDismiss,
        onReportConfirm = viewModel::onReportConfirm,
    )
}
