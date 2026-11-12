package cl.udec.cocinavecina.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.udec.cocinavecina.data.ApiError
import cl.udec.cocinavecina.data.network.TokenResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class LoginViewModel(
    private val authenticate: suspend (email: String, password: String) -> Result<TokenResponse>,
    private val saveSession: suspend (TokenResponse) -> Unit,
) : ViewModel() {
    private val mutableState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = mutableState.asStateFlow()

    fun onEmailChange(value: String) {
        mutableState.update { it.copy(email = value, emailError = null, formError = null) }
    }

    fun onPasswordChange(value: String) {
        mutableState.update { it.copy(password = value, passwordError = null, formError = null) }
    }

    fun onKeepSessionChange(value: Boolean) {
        mutableState.update { it.copy(keepSession = value) }
    }

    fun onQuickAccessChange(value: Boolean) {
        mutableState.update { it.copy(quickAccess = value) }
    }

    fun onLoginClick() {
        val current = mutableState.value
        if (current.isLoading) return
        val email = current.email.trim()
        val emailError = if (isValidEmail(email)) null else INVALID_EMAIL
        val passwordError = if (current.password.isEmpty()) EMPTY_PASSWORD else null
        if (emailError != null || passwordError != null) {
            mutableState.update { it.copy(emailError = emailError, passwordError = passwordError, formError = null) }
            return
        }
        mutableState.update { it.copy(isLoading = true, emailError = null, passwordError = null, formError = null) }
        viewModelScope.launch {
            val failure =
                try {
                    authenticate(email, current.password).map { saveSession(it) }.exceptionOrNull()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    e
                }
            mutableState.update { it.copy(isLoading = false, formError = failure?.toFormError()) }
        }
    }

    private fun Throwable.toFormError(): String =
        when ((this as? ApiError)?.code) {
            0 -> NO_CONNECTION
            400, 401 -> INVALID_CREDENTIALS
            403 -> UNVERIFIED_ACCOUNT
            else -> SERVER_ERROR
        }

    companion object {
        const val INVALID_EMAIL = "Ingresa un correo válido"
        const val EMPTY_PASSWORD = "Ingresa tu contraseña"
        const val INVALID_CREDENTIALS = "Correo o contraseña incorrectos."
        const val UNVERIFIED_ACCOUNT = "Tu cuenta aún no está verificada. Abre el enlace que te enviamos al correo."
        const val NO_CONNECTION = "Sin conexión. Revisa tu internet e inténtalo de nuevo."
        const val SERVER_ERROR = "Algo salió mal de nuestro lado. Inténtalo más tarde."

        private val EMAIL_PATTERN = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$")

        fun isValidEmail(value: String): Boolean = EMAIL_PATTERN.matches(value)
    }
}
