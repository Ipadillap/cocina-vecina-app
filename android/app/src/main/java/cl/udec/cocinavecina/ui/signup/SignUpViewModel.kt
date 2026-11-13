package cl.udec.cocinavecina.ui.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.udec.cocinavecina.data.ApiError
import cl.udec.cocinavecina.data.network.SignUpRequest
import cl.udec.cocinavecina.data.network.SignUpResponse
import cl.udec.cocinavecina.ui.login.LoginViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class SignUpViewModel(
    private val register: suspend (SignUpRequest) -> Result<SignUpResponse>,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = mutableState.asStateFlow()

    fun onDisplayNameChange(value: String) {
        mutableState.update { it.copy(displayName = value, displayNameError = null, formError = null) }
    }

    fun onEmailChange(value: String) {
        mutableState.update { it.copy(email = value, emailError = null, formError = null) }
    }

    fun onPasswordChange(value: String) {
        mutableState.update { it.copy(password = value, passwordError = null, formError = null) }
    }

    fun onAdultChange(value: Boolean) {
        mutableState.update { it.copy(isAdult = value, consentError = null, formError = null) }
    }

    fun onTermsChange(value: Boolean) {
        mutableState.update { it.copy(acceptsTerms = value, consentError = null, formError = null) }
    }

    fun onLocationChange(value: Boolean) {
        mutableState.update { it.copy(locationEnabled = value) }
    }

    fun onNotificationsChange(value: Boolean) {
        mutableState.update { it.copy(notificationsEnabled = value) }
    }

    fun onSignUpClick() {
        val current = mutableState.value
        if (current.isLoading || current.successMessage != null) return
        val displayName = current.displayName.trim()
        val email = current.email.trim()
        val displayNameError = if (displayName.isEmpty()) EMPTY_NAME else null
        val emailError = if (LoginViewModel.isValidEmail(email)) null else LoginViewModel.INVALID_EMAIL
        val passwordError = if (current.password.length >= MIN_PASSWORD_LENGTH) null else SHORT_PASSWORD
        val consentError = if (current.hasPendingConsent) CONSENT_REQUIRED else null
        if (displayNameError != null || emailError != null || passwordError != null || consentError != null) {
            mutableState.update {
                it.copy(
                    displayNameError = displayNameError,
                    emailError = emailError,
                    passwordError = passwordError,
                    consentError = consentError,
                    formError = null,
                )
            }
            return
        }
        mutableState.update {
            it.copy(
                isLoading = true,
                displayNameError = null,
                emailError = null,
                passwordError = null,
                consentError = null,
                formError = null,
            )
        }
        viewModelScope.launch {
            val request =
                SignUpRequest(
                    email = email,
                    displayName = displayName,
                    password = current.password,
                    phone = null,
                    role = BUYER_ROLE,
                    adult = current.isAdult,
                    acceptsTerms = current.acceptsTerms,
                )
            val result =
                try {
                    register(request)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Result.failure(e)
                }
            mutableState.update {
                it.copy(
                    isLoading = false,
                    successMessage = result.getOrNull()?.message ?: if (result.isSuccess) VERIFY_EMAIL else null,
                    formError = result.exceptionOrNull()?.toFormError(),
                )
            }
        }
    }

    private fun Throwable.toFormError(): String =
        when ((this as? ApiError)?.code) {
            0 -> NO_CONNECTION
            409 -> EMAIL_TAKEN
            400, 422 -> INVALID_DATA
            else -> SERVER_ERROR
        }

    companion object {
        const val EMPTY_NAME = "Ingresa un nombre visible"
        const val SHORT_PASSWORD = "Usa al menos 8 caracteres"
        const val CONSENT_REQUIRED = "Debes aceptar ambas casillas para crear tu cuenta."
        const val EMAIL_TAKEN = "Ya existe una cuenta con ese correo."
        const val INVALID_DATA = "Revisa los datos ingresados e inténtalo de nuevo."
        const val NO_CONNECTION = "Sin conexión. Revisa tu internet e inténtalo de nuevo."
        const val SERVER_ERROR = "Algo salió mal de nuestro lado. Inténtalo más tarde."
        const val VERIFY_EMAIL = "Te enviamos un enlace para verificar tu correo."
        const val MIN_PASSWORD_LENGTH = 8
        private const val BUYER_ROLE = "BUYER"
    }
}
