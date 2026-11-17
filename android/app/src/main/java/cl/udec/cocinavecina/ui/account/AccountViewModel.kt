package cl.udec.cocinavecina.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.udec.cocinavecina.data.ApiError
import cl.udec.cocinavecina.data.network.AccountView
import cl.udec.cocinavecina.data.network.PickupPointView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class AccountViewModel(
    private val loadAccount: suspend () -> Result<AccountView>,
    private val loadPickupPoints: suspend () -> Result<List<PickupPointView>>,
    private val exportData: suspend () -> Result<String>,
    private val deleteAccount: suspend () -> Result<Unit>,
) : ViewModel() {
    private val mutableState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = mutableState.asStateFlow()

    init {
        load()
    }

    fun onRetry() = load()

    /** Recarga la cuenta sin spinner (p. ej. tras verificar el correo en el navegador). */
    fun refresh() {
        if (mutableState.value.isLoading) return
        viewModelScope.launch {
            val account = safely { loadAccount() }.getOrNull() ?: return@launch
            mutableState.update { it.copy(account = account) }
        }
    }

    fun onPermissionsChanged(
        isLocationGranted: Boolean,
        areNotificationsEnabled: Boolean,
    ) {
        mutableState.update { it.copy(isLocationGranted = isLocationGranted, areNotificationsEnabled = areNotificationsEnabled) }
    }

    fun onTermsClick() {
        mutableState.update { it.copy(isTermsVisible = true) }
    }

    fun onTermsDismiss() {
        mutableState.update { it.copy(isTermsVisible = false) }
    }

    fun onExportClick() {
        if (mutableState.value.isActing) return
        mutableState.update { it.copy(isExportPickerRequested = true, actionError = null, message = null) }
    }

    fun onExportPickerShown() {
        mutableState.update { it.copy(isExportPickerRequested = false) }
    }

    fun onExportDestinationChosen(save: suspend (content: String) -> Unit) {
        if (mutableState.value.isActing) return
        mutableState.update { it.copy(pendingAction = AccountAction.EXPORT, actionError = null, message = null) }
        viewModelScope.launch {
            val result = safely { exportData() }.mapCatching { content -> save(content) }
            mutableState.update { state ->
                result.fold(
                    onSuccess = { state.copy(pendingAction = null, message = EXPORT_SAVED) },
                    onFailure = { failure -> state.copy(pendingAction = null, actionError = failure.toMessage(EXPORT_ERROR)) },
                )
            }
        }
    }

    fun onDeleteClick() {
        if (mutableState.value.isActing) return
        mutableState.update { it.copy(isDeleteDialogVisible = true, actionError = null, message = null) }
    }

    fun onDeleteDismiss() {
        mutableState.update { it.copy(isDeleteDialogVisible = false) }
    }

    fun onDeleteConfirm() {
        if (mutableState.value.isActing) return
        mutableState.update { it.copy(isDeleteDialogVisible = false, pendingAction = AccountAction.DELETE) }
        viewModelScope.launch {
            val result = safely { deleteAccount() }
            mutableState.update { state ->
                result.fold(
                    onSuccess = { state.copy(pendingAction = null, isAccountDeleted = true) },
                    onFailure = { failure -> state.copy(pendingAction = null, actionError = failure.toMessage(DELETE_ERROR)) },
                )
            }
        }
    }

    fun onMessageShown() {
        mutableState.update { it.copy(message = null) }
    }

    private fun load() {
        mutableState.update { it.copy(isLoading = true, loadError = null) }
        viewModelScope.launch {
            val accountResult = safely { loadAccount() }
            val account = accountResult.getOrNull()
            val pickupPoints =
                if (account?.role == SELLER_ROLE) {
                    safely { loadPickupPoints() }.getOrNull()
                } else {
                    null
                }
            mutableState.update { state ->
                accountResult.fold(
                    onSuccess = { state.copy(isLoading = false, account = it, pickupPoints = pickupPoints) },
                    onFailure = { failure -> state.copy(isLoading = false, loadError = failure.toMessage(LOAD_ERROR)) },
                )
            }
        }
    }

    private suspend fun <T> safely(block: suspend () -> Result<T>): Result<T> =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    private fun Throwable.toMessage(fallback: String): String =
        when ((this as? ApiError)?.code) {
            0 -> NO_CONNECTION
            409, 422 -> (this as ApiError).message
            else -> fallback
        }

    companion object {
        const val NO_CONNECTION = "Sin conexión. Revisa tu internet e inténtalo de nuevo."
        const val LOAD_ERROR = "No pudimos cargar tu cuenta. Inténtalo de nuevo."
        const val EXPORT_ERROR = "No pudimos descargar tus datos. Inténtalo de nuevo."
        const val DELETE_ERROR = "No pudimos eliminar tu cuenta. Inténtalo de nuevo."
        const val EXPORT_SAVED = "Listo, guardamos tus datos en el archivo que elegiste."
    }
}
