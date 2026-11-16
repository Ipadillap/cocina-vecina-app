package cl.udec.cocinavecina.ui.account

import cl.udec.cocinavecina.data.ApiError
import cl.udec.cocinavecina.data.network.AccountView
import cl.udec.cocinavecina.data.network.PickupPointView
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
import java.io.IOException
import java.util.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {
    private var pickupLoads = 0
    private var deletions = 0
    private lateinit var defaultZone: TimeZone

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        defaultZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("America/Santiago"))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        TimeZone.setDefault(defaultZone)
    }

    private val seller =
        AccountView(
            id = 2,
            email = "marta@ejemplo.cl",
            displayName = "Marta R.",
            phone = "+56987650142",
            role = SELLER_ROLE,
            verified = true,
            termsVersion = "1.0",
            termsAcceptedAt = "2026-10-08T15:00:00Z",
        )

    private fun viewModel(
        account: () -> Result<AccountView> = { Result.success(seller) },
        export: () -> Result<String> = { Result.success("{\"account\":{}}") },
        delete: () -> Result<Unit> = { Result.success(Unit) },
    ) = AccountViewModel(
        loadAccount = { account() },
        loadPickupPoints = {
            pickupLoads++
            Result.success(listOf(PickupPointView(1, "Casa", -36.82, -73.05)))
        },
        exportData = { export() },
        deleteAccount = {
            deletions++
            delete()
        },
    )

    @Test
    fun sellerProfileIsSummarized() {
        val state = viewModel().uiState.value
        assertEquals("M", state.initial)
        assertEquals("+56 9 •••• 0142", state.maskedPhone)
        assertEquals("Compra y vende", state.modeLabel)
        assertEquals("1 · Casa", state.pickupPointsSummary)
        assertEquals("v1.0", state.termsVersionLabel)
        assertEquals("08-10-2026", state.termsAcceptedLabel)
        assertFalse(state.isLoading)
    }

    @Test
    fun buyerDoesNotLoadPickupPoints() {
        val state = viewModel(account = { Result.success(seller.copy(role = BUYER_ROLE, phone = null)) }).uiState.value
        assertEquals(0, pickupLoads)
        assertNull(state.pickupPointsSummary)
        assertNull(state.maskedPhone)
        assertEquals("Solo compra", state.modeLabel)
    }

    @Test
    fun loadErrorAndRetry() {
        var result: Result<AccountView> = Result.failure(ApiError(0, "x"))
        val viewModel = viewModel(account = { result })
        assertEquals(AccountViewModel.NO_CONNECTION, viewModel.uiState.value.loadError)
        result = Result.success(seller)
        viewModel.onRetry()
        assertNull(viewModel.uiState.value.loadError)
        assertEquals(seller, viewModel.uiState.value.account)
    }

    @Test
    fun exportAsksForFileThenSavesContent() {
        val viewModel = viewModel()
        viewModel.onExportClick()
        assertTrue(viewModel.uiState.value.isExportPickerRequested)
        viewModel.onExportPickerShown()
        assertFalse(viewModel.uiState.value.isExportPickerRequested)
        var saved: String? = null
        viewModel.onExportDestinationChosen { saved = it }
        assertEquals("{\"account\":{}}", saved)
        assertEquals(AccountViewModel.EXPORT_SAVED, viewModel.uiState.value.message)
        viewModel.onMessageShown()
        assertNull(viewModel.uiState.value.message)
    }

    @Test
    fun exportWriteFailureShowsError() {
        val viewModel = viewModel()
        viewModel.onExportDestinationChosen { throw IOException("disk") }
        val state = viewModel.uiState.value
        assertEquals(AccountViewModel.EXPORT_ERROR, state.actionError)
        assertNull(state.pendingAction)
    }

    @Test
    fun deleteRequiresConfirmation() {
        val viewModel = viewModel()
        viewModel.onDeleteClick()
        assertTrue(viewModel.uiState.value.isDeleteDialogVisible)
        viewModel.onDeleteDismiss()
        assertFalse(viewModel.uiState.value.isDeleteDialogVisible)
        assertEquals(0, deletions)
        viewModel.onDeleteClick()
        viewModel.onDeleteConfirm()
        assertEquals(1, deletions)
        assertTrue(viewModel.uiState.value.isAccountDeleted)
    }

    @Test
    fun deleteConflictShowsServerMessage() {
        val viewModel = viewModel(delete = { Result.failure(ApiError(409, "Tienes pedidos en curso.")) })
        viewModel.onDeleteClick()
        viewModel.onDeleteConfirm()
        val state = viewModel.uiState.value
        assertEquals("Tienes pedidos en curso.", state.actionError)
        assertFalse(state.isAccountDeleted)
    }

    @Test
    fun permissionsAreReflected() {
        val viewModel = viewModel()
        viewModel.onPermissionsChanged(isLocationGranted = true, areNotificationsEnabled = false)
        assertTrue(viewModel.uiState.value.isLocationGranted)
        assertFalse(viewModel.uiState.value.areNotificationsEnabled)
    }
}
