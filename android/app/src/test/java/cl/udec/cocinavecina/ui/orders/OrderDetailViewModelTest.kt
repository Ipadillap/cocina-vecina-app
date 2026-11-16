package cl.udec.cocinavecina.ui.orders

import cl.udec.cocinavecina.data.ApiError
import cl.udec.cocinavecina.data.network.CodeView
import cl.udec.cocinavecina.data.network.OfferView
import cl.udec.cocinavecina.data.network.OrderView
import cl.udec.cocinavecina.data.network.PickupDetailsView
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
class OrderDetailViewModelTest {
    private val start = Instant.parse("2026-11-13T16:29:00Z")
    private var now = start
    private var storedCode: String? = null
    private var issuedCodes = 0
    private var pickupLoads = 0
    private var cancelReason: String? = null
    private var validatedCode: String? = null

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
            title = "Charquicán",
            category = "MAIN_COURSE",
            basePrice = 3500,
            minPrice = 2800,
            availablePortions = 3,
            allergens = emptyList(),
            preparedAt = start.minus(2, ChronoUnit.HOURS).toString(),
            expiresAt = start.plus(71, ChronoUnit.MINUTES).toString(),
            status = "ACTIVE",
            approxLatitude = -36.82,
            approxLongitude = -73.05,
        )

    private val accepted =
        OrderView(
            id = 5,
            offerId = 9,
            buyerId = 1,
            sellerId = 2,
            agreedPrice = 3300,
            portions = 2,
            status = ACCEPTED_STATUS,
            acceptedAt = start.minus(5, ChronoUnit.MINUTES).toString(),
            windowEnd = start.plus(71, ChronoUnit.MINUTES).toString(),
        )

    private val pickup = PickupDetailsView("Pje. Los Copihues 482, Concepción", "Timbre del 2.º piso", -36.82, -73.05, "+56 9 1234 0142")

    private fun viewModel(
        userId: Long = 1,
        order: OrderView = accepted,
        loadedOrder: () -> Result<OrderView> = { Result.success(order) },
        actionResult: (String) -> Result<OrderView> = { status -> Result.success(order.copy(status = status)) },
    ) = OrderDetailViewModel(
        orderId = 5,
        userId = userId,
        loadOrder = { loadedOrder() },
        loadOffer = { Result.success(offer) },
        readStoredCode = { storedCode },
        issueCode = {
            issuedCodes++
            Result.success(CodeView(5, "482913", order.windowEnd))
        },
        storeCode = { _, code -> storedCode = code },
        loadPickup = {
            pickupLoads++
            Result.success(pickup)
        },
        markReady = { actionResult(READY_STATUS) },
        validateDelivery = { _, code ->
            validatedCode = code
            actionResult(DELIVERED_STATUS)
        },
        cancelOrder = { _, reason ->
            cancelReason = reason
            actionResult("CANCELLED")
        },
        clock = { now },
    )

    @Test
    fun buyerLoadsOrderCodeAndPickup() {
        val state = viewModel().uiState.value
        assertTrue(state.isBuyer)
        assertEquals("482913", state.code)
        assertEquals("482913", storedCode)
        assertEquals(pickup, state.pickup)
        assertEquals(6600, state.total)
        assertEquals("Marta", state.sellerName)
        assertEquals(0, state.currentStep)
        assertEquals(71, state.minutesToPickupDeadline)
    }

    @Test
    fun storedCodeIsReusedWithoutIssuingAgain() {
        storedCode = "111222"
        val state = viewModel().uiState.value
        assertEquals("111222", state.code)
        assertEquals(0, issuedCodes)
    }

    @Test
    fun sellerDoesNotRequestCodeOrPickup() {
        val state = viewModel(userId = 2).uiState.value
        assertFalse(state.isBuyer)
        assertNull(state.code)
        assertNull(state.pickup)
        assertEquals(0, pickupLoads)
    }

    @Test
    fun pickupHidesOneHourAfterDelivery() {
        val delivered = accepted.copy(status = DELIVERED_STATUS, deliveredAt = start.toString())
        val viewModel = viewModel(order = delivered)
        assertEquals(pickup, viewModel.uiState.value.pickup)
        assertEquals(2, viewModel.uiState.value.currentStep)
        now = start.plus(61, ChronoUnit.MINUTES)
        viewModel.onTick()
        assertFalse(viewModel.uiState.value.isPickupUnlocked)
        assertNull(viewModel.uiState.value.pickup)
    }

    @Test
    fun cancelledOrderHasNoPickupAndCannotCancel() {
        val state = viewModel(order = accepted.copy(status = "CANCELLED")).uiState.value
        assertNull(state.pickup)
        assertNull(state.code)
        assertFalse(state.canCancel)
        assertEquals(-1, state.currentStep)
    }

    @Test
    fun cancelAsksForReasonAndCancels() {
        val viewModel = viewModel()
        viewModel.onCancelClick()
        assertTrue(viewModel.uiState.value.isCancelDialogVisible)
        viewModel.onCancelConfirm("   ")
        assertTrue(viewModel.uiState.value.isCancelDialogVisible)
        viewModel.onCancelConfirm("  Me surgió un imprevisto ")
        val state = viewModel.uiState.value
        assertEquals("Me surgió un imprevisto", cancelReason)
        assertFalse(state.isCancelDialogVisible)
        assertEquals("CANCELLED", state.order?.status)
        assertNull(state.pickup)
    }

    @Test
    fun sellerMarksReadyAndValidatesCode() {
        val viewModel = viewModel(userId = 2)
        viewModel.onMarkReadyClick()
        assertEquals(READY_STATUS, viewModel.uiState.value.order?.status)
        viewModel.onValidateCode("482 913")
        assertEquals("482913", validatedCode)
        assertEquals(DELIVERED_STATUS, viewModel.uiState.value.order?.status)
    }

    @Test
    fun shortCodeShowsErrorWithoutCalling() {
        val viewModel = viewModel(userId = 2, order = accepted.copy(status = READY_STATUS))
        viewModel.onValidateCode("4829")
        assertEquals(OrderDetailViewModel.INVALID_CODE, viewModel.uiState.value.actionError)
        assertNull(validatedCode)
    }

    @Test
    fun wrongCodeShowsServerMessage() {
        val viewModel =
            viewModel(
                userId = 2,
                order = accepted.copy(status = READY_STATUS),
                actionResult = { Result.failure(ApiError(422, "El código no coincide.")) },
            )
        viewModel.onValidateCode("000000")
        val state = viewModel.uiState.value
        assertEquals("El código no coincide.", state.actionError)
        assertEquals(READY_STATUS, state.order?.status)
        assertNull(state.pendingAction)
    }

    @Test
    fun typedCodeIsFilteredValidatedAndErrorClearsOnEdit() {
        val viewModel =
            viewModel(
                userId = 2,
                order = accepted.copy(status = READY_STATUS),
                actionResult = { Result.failure(ApiError(422, "El código no coincide.")) },
            )
        viewModel.onCodeInputChange("48a2 9137")
        assertEquals("482913", viewModel.uiState.value.codeInput)
        assertTrue(viewModel.uiState.value.canValidate)
        viewModel.onValidateClick()
        assertEquals("482913", validatedCode)
        assertEquals("El código no coincide.", viewModel.uiState.value.actionError)
        viewModel.onCodeInputChange("48291")
        assertNull(viewModel.uiState.value.actionError)
        assertFalse(viewModel.uiState.value.canValidate)
    }

    @Test
    fun loadErrorMapsConnectionAndRetryRecovers() {
        var result: Result<OrderView> = Result.failure(ApiError(0, "x"))
        val viewModel = viewModel(loadedOrder = { result })
        assertEquals(OrderDetailViewModel.NO_CONNECTION, viewModel.uiState.value.loadError)
        result = Result.success(accepted)
        viewModel.onRetry()
        assertNull(viewModel.uiState.value.loadError)
        assertEquals(accepted, viewModel.uiState.value.order)
    }

    @Test
    fun silentRefreshKeepsOrderOnFailure() {
        var result: Result<OrderView> = Result.success(accepted)
        val viewModel = viewModel(loadedOrder = { result })
        result = Result.failure(ApiError(0, "x"))
        viewModel.refresh()
        assertNull(viewModel.uiState.value.loadError)
        assertEquals(accepted, viewModel.uiState.value.order)
    }
}
