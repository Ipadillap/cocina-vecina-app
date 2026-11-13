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
class ProposePriceViewModelTest {
    private data class Submission(val offerId: Long, val portions: Int, val unitPrice: Int?)

    private val submissions = mutableListOf<Submission>()

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
            preparedAt = Instant.now().toString(),
            expiresAt = Instant.now().plus(1, ChronoUnit.HOURS).toString(),
            status = "ACTIVE",
            approxLatitude = -36.82,
            approxLongitude = -73.05,
        )

    private fun viewModel(
        initialPortions: Int = 2,
        result: Result<PurchaseRequestView> = Result.success(PurchaseRequestView(41, 9, 2, 1, 3000, "BUYER", "SELLER", "OFFER_SENT")),
    ) = ProposePriceViewModel(
        offerId = 9,
        initialPortions = initialPortions,
        loadOffer = { Result.success(offer) },
        submitRequest = { offerId, portions, unitPrice ->
            submissions += Submission(offerId, portions, unitPrice)
            result
        },
    )

    @Test
    fun startsWithSuggestedPriceAndInitialPortions() {
        val state = viewModel().uiState.value
        assertEquals(3200, state.price)
        assertEquals(2, state.portions)
        assertEquals(1, state.round)
        assertNull(state.priceError)
    }

    @Test
    fun clampsInitialPortionsToAvailable() {
        assertEquals(3, viewModel(initialPortions = 5).uiState.value.portions)
    }

    @Test
    fun computesSavingsAndTotal() {
        val viewModel = viewModel()
        viewModel.onShortcutSelect(3000)
        val state = viewModel.uiState.value
        assertEquals(500, state.savingsPerPortion)
        assertEquals(6000, state.total)
        assertEquals(7000, state.publishedTotal)
    }

    @Test
    fun stepsStayInsideAllowedRange() {
        val viewModel = viewModel()
        viewModel.onShortcutSelect(2800)
        viewModel.onDecreasePrice()
        assertEquals(2800, viewModel.uiState.value.price)
        viewModel.onShortcutSelect(3500)
        viewModel.onIncreasePrice()
        assertEquals(3500, viewModel.uiState.value.price)
    }

    @Test
    fun rejectsPricesOutsideRangeOrStep() {
        val viewModel = viewModel()
        viewModel.onPriceTextChange("2500")
        assertEquals("El mínimo de Marta R. es $2.800", viewModel.uiState.value.priceError)
        viewModel.onPriceTextChange("3600")
        assertFalse(viewModel.uiState.value.canSubmit)
        viewModel.onPriceTextChange("3050")
        assertEquals("Usa montos en múltiplos de $100", viewModel.uiState.value.priceError)
        viewModel.onPriceTextChange("")
        assertEquals(ProposePriceUiState.EMPTY_PRICE, viewModel.uiState.value.priceError)
    }

    @Test
    fun keepsOnlyDigitsInPriceText() {
        val viewModel = viewModel()
        viewModel.onPriceTextChange("$3.100")
        assertEquals("3100", viewModel.uiState.value.priceText)
    }

    @Test
    fun offersShortcutsBetweenMinimumAndPublishedPrice() {
        assertEquals(listOf(2800, 3000, 3300), viewModel().uiState.value.priceShortcuts)
    }

    @Test
    fun submitsProposalWithPrice() {
        val viewModel = viewModel()
        viewModel.onShortcutSelect(3000)
        viewModel.onSubmitClick()
        assertEquals(listOf(Submission(9, 2, 3000)), submissions)
        assertEquals(41L, viewModel.uiState.value.createdRequestId)
    }

    @Test
    fun buysAtPublishedPriceWithoutUnitPrice() {
        val viewModel = viewModel()
        viewModel.onBuyAtPublishedPriceClick()
        assertEquals(listOf(Submission(9, 2, null)), submissions)
    }

    @Test
    fun doesNotSubmitInvalidPrice() {
        val viewModel = viewModel()
        viewModel.onPriceTextChange("100")
        viewModel.onSubmitClick()
        assertTrue(submissions.isEmpty())
    }

    @Test
    fun showsServerRuleMessage() {
        val viewModel = viewModel(result = Result.failure(ApiError(422, "La oferta ya no está disponible.")))
        viewModel.onSubmitClick()
        assertEquals("La oferta ya no está disponible.", viewModel.uiState.value.submitError)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }
}
