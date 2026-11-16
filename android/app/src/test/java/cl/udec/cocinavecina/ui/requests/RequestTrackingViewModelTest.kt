package cl.udec.cocinavecina.ui.requests

import cl.udec.cocinavecina.data.ApiError
import cl.udec.cocinavecina.data.network.OfferView
import cl.udec.cocinavecina.data.network.ProposalView
import cl.udec.cocinavecina.data.network.PurchaseRequestView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalCoroutinesApi::class)
class RequestTrackingViewModelTest {
    private val start = Instant.parse("2026-11-13T16:29:00Z")
    private var now = start
    private var requestLoads = 0
    private var offerLoads = 0

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val offer =
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
            preparedAt = start.minus(2, ChronoUnit.HOURS).toString(),
            expiresAt = start.plus(41, ChronoUnit.MINUTES).toString(),
            status = "ACTIVE",
            approxLatitude = -36.82,
            approxLongitude = -73.05,
        )

    private val counteroffer =
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
                    ProposalView("BUYER", 3000, start.minus(10, ChronoUnit.MINUTES).toString()),
                    ProposalView("SELLER", 3300, start.minus(5, ChronoUnit.MINUTES).toString()),
                ),
        )

    private fun viewModel(
        request: PurchaseRequestView = counteroffer,
        accepted: Result<PurchaseRequestView> = Result.success(counteroffer.copy(status = "ACCEPTED", turn = null, orderId = 77)),
        rejected: Result<PurchaseRequestView> = Result.success(counteroffer.copy(status = "REJECTED", turn = null)),
        loadedRequest: () -> Result<PurchaseRequestView> = { Result.success(request) },
    ) = RequestTrackingViewModel(
        requestId = 41,
        loadRequest = {
            requestLoads++
            loadedRequest()
        },
        loadOffer = {
            offerLoads++
            Result.success(offer)
        },
        acceptRequest = { accepted },
        rejectRequest = { rejected },
        clock = { now },
    )

    @Test
    fun loadsCounterofferWithSellerAndCountdown() {
        val state = viewModel().uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.hasSellerCounteroffer)
        assertEquals("Marta", state.sellerName)
        assertEquals(41L, state.minutesLeft)
        assertEquals(6600, state.total)
        assertTrue(state.canProposeAgain)
    }

    @Test
    fun tickUpdatesCountdownAndExpires() {
        val viewModel = viewModel()
        now = start.plus(41, ChronoUnit.MINUTES)
        viewModel.onTick()
        assertTrue(viewModel.uiState.value.isExpired)
        assertFalse(viewModel.uiState.value.isBuyerTurn)
    }

    @Test
    fun acceptingOpensOrder() {
        val viewModel = viewModel()
        viewModel.onAcceptClick()
        assertEquals(77L, viewModel.uiState.value.openOrderId)
        assertEquals("ACCEPTED", viewModel.uiState.value.request?.status)
        viewModel.onOrderNavigationHandled()
        assertNull(viewModel.uiState.value.openOrderId)
    }

    @Test
    fun rejectingNeedsConfirmation() {
        val viewModel = viewModel()
        viewModel.onRejectClick()
        assertTrue(viewModel.uiState.value.isRejectConfirmVisible)
        assertEquals("COUNTEROFFER", viewModel.uiState.value.request?.status)
        viewModel.onRejectConfirm()
        assertFalse(viewModel.uiState.value.isRejectConfirmVisible)
        assertEquals("REJECTED", viewModel.uiState.value.request?.status)
    }

    @Test
    fun actionsAreIgnoredWhileWaitingForSeller() {
        val viewModel = viewModel(request = counteroffer.copy(currentParty = "BUYER", turn = "SELLER", status = "OFFER_SENT"))
        viewModel.onAcceptClick()
        viewModel.onRejectClick()
        assertTrue(viewModel.uiState.value.isWaitingSeller)
        assertFalse(viewModel.uiState.value.isRejectConfirmVisible)
        assertNull(viewModel.uiState.value.openOrderId)
    }

    @Test
    fun lastRoundOnlyAllowsAcceptOrReject() {
        val state = viewModel(request = counteroffer.copy(rounds = 3)).uiState.value
        assertTrue(state.isBuyerTurn)
        assertFalse(state.canProposeAgain)
    }

    @Test
    fun refreshReusesLoadedOffer() {
        val viewModel = viewModel()
        viewModel.refresh()
        assertEquals(2, requestLoads)
        assertEquals(1, offerLoads)
    }

    @Test
    fun failedRefreshKeepsShownRequest() {
        var fail = false
        val viewModel = viewModel(loadedRequest = { if (fail) Result.failure(ApiError(0, "x")) else Result.success(counteroffer) })
        fail = true
        viewModel.refresh()
        assertNull(viewModel.uiState.value.loadError)
        assertEquals(41L, viewModel.uiState.value.request?.id)
    }

    @Test
    fun actionErrorShowsServerRule() {
        val viewModel = viewModel(accepted = Result.failure(ApiError(422, "No quedan suficientes porciones.")))
        viewModel.onAcceptClick()
        assertEquals("No quedan suficientes porciones.", viewModel.uiState.value.actionError)
        assertNull(viewModel.uiState.value.pendingAction)
    }
}
