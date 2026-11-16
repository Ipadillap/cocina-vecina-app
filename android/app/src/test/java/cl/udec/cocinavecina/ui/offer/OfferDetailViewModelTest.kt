package cl.udec.cocinavecina.ui.offer

import cl.udec.cocinavecina.data.ApiError
import cl.udec.cocinavecina.data.network.OfferView
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
class OfferDetailViewModelTest {
    private val purchases = mutableListOf<Pair<Long, Int>>()
    private val reports = mutableListOf<String>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun offer(
        availablePortions: Int = 3,
        expiresAt: Instant = Instant.now().plus(1, ChronoUnit.HOURS),
    ) = OfferView(
        id = 9,
        sellerId = 2,
        seller = "Marta R.",
        title = "Charquicán con huevo frito",
        category = "MAIN_COURSE",
        basePrice = 3500,
        minPrice = 2800,
        availablePortions = availablePortions,
        allergens = listOf("EGG"),
        preparedAt = Instant.now().minus(1, ChronoUnit.HOURS).toString(),
        expiresAt = expiresAt.toString(),
        status = "ACTIVE",
        approxLatitude = -36.82,
        approxLongitude = -73.05,
        totalPortions = 4,
    )

    private fun request(id: Long) =
        PurchaseRequestView(id, 9, 1, 1, 3500, "BUYER", "SELLER", "OFFER_SENT")

    private fun viewModel(
        loaded: Result<OfferView> = Result.success(offer()),
        purchase: Result<PurchaseRequestView> = Result.success(request(41)),
    ) = OfferDetailViewModel(
        offerId = 9,
        distanceMeters = 400,
        loadOffer = { loaded },
        requestPurchase = { offerId, portions ->
            purchases += offerId to portions
            purchase
        },
        sendReport = { _, reason ->
            reports += reason
            Result.success(Unit)
        },
        resolvePhotoUrl = { it },
    )

    @Test
    fun loadsOfferAndKeepsDistance() {
        val state = viewModel().uiState.value
        assertFalse(state.isLoading)
        assertEquals("Charquicán con huevo frito", state.offer?.title)
        assertEquals(400L, state.distanceMeters)
        assertTrue(state.isOnSale)
    }

    @Test
    fun limitsPortionsToAvailable() {
        val viewModel = viewModel()
        viewModel.onPortionsChange(3)
        viewModel.onPortionsChange(4)
        assertEquals(3, viewModel.uiState.value.portions)
        assertEquals(10500, viewModel.uiState.value.total)
    }

    @Test
    fun limitsPortionsToRequestMaximum() {
        val viewModel = viewModel(loaded = Result.success(offer(availablePortions = 8)))
        assertEquals(MAX_PORTIONS_PER_REQUEST, viewModel.uiState.value.maxPortions)
    }

    @Test
    fun buyingSendsPortionsAndExposesRequestId() {
        val viewModel = viewModel()
        viewModel.onPortionsChange(2)
        viewModel.onBuyClick()
        assertEquals(listOf(9L to 2), purchases)
        assertEquals(41L, viewModel.uiState.value.createdRequestId)
        viewModel.onPurchaseNavigationHandled()
        assertNull(viewModel.uiState.value.createdRequestId)
    }

    @Test
    fun buyingWithUnverifiedAccountShowsMessage() {
        val viewModel = viewModel(purchase = Result.failure(ApiError(403, "Forbidden")))
        viewModel.onBuyClick()
        assertEquals(OfferDetailViewModel.UNVERIFIED_ACCOUNT, viewModel.uiState.value.buyError)
        assertFalse(viewModel.uiState.value.isBuying)
    }

    @Test
    fun expiredOfferCannotBeBought() {
        val viewModel = viewModel(loaded = Result.success(offer(expiresAt = Instant.now().minus(1, ChronoUnit.MINUTES))))
        viewModel.onBuyClick()
        assertFalse(viewModel.uiState.value.isOnSale)
        assertTrue(purchases.isEmpty())
    }

    @Test
    fun missingOfferShowsNotFound() {
        val viewModel = viewModel(loaded = Result.failure(ApiError(404, "Not found")))
        assertEquals(OfferDetailViewModel.OFFER_NOT_FOUND, viewModel.uiState.value.loadError)
    }

    @Test
    fun reportClosesDialogAndConfirms() {
        val viewModel = viewModel()
        viewModel.onReportClick()
        viewModel.onReportConfirm("HEALTH_RISK")
        assertEquals(listOf("HEALTH_RISK"), reports)
        assertFalse(viewModel.uiState.value.isReportDialogVisible)
        assertEquals(OfferDetailViewModel.REPORT_SENT, viewModel.uiState.value.reportMessage)
    }
}
