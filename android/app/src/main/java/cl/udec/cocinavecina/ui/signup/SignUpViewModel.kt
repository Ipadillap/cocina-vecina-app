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
    private val signIn: suspend (email: String, password: String) -> Result<Unit>,
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
        mutableState.update { it.copy(password = value, passwordError = null, confirmPasswordError = null, formError = null) }
    }

    fun onConfirmPasswordChange(value: String) {
        mutableState.update { it.copy(confirmPassword = value, confirmPasswordError = null, formError = null) }
    }

    fun onWantsToSellChange(value: Boolean) {
        mutableState.update { it.copy(wantsToSell = value, phoneError = null, formError = null) }
    }

    fun onPhoneChange(value: String) {
        mutableState.update { it.copy(phone = value, phoneError = null, formError = null) }
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

    fun reset() {
        mutableState.value = SignUpUiState()
    }

    fun onSignUpClick() {
        val current = mutableState.value
        if (current.isLoading || current.successMessage != null) return
        val displayName = current.displayName.trim()
        val email = current.email.trim()
        val displayNameError = if (displayName.isEmpty()) EMPTY_NAME else null
        val emailError = if (LoginViewModel.isValidEmail(email)) null else LoginViewModel.INVALID_EMAIL
        val phone = current.phone.trim()
        val passwordError = if (isStrongPassword(current.password)) null else WEAK_PASSWORD
        val confirmPasswordError = if (current.confirmPassword == current.password) null else PASSWORDS_DIFFER
        val phoneError =
            when {
                !current.wantsToSell -> null
                phone.isEmpty() -> PHONE_REQUIRED
                !isValidPhone(phone) -> INVALID_PHONE
                else -> null
            }
        val consentError = if (current.hasPendingConsent) CONSENT_REQUIRED else null
        if (listOfNotNull(displayNameError, emailError, passwordError, confirmPasswordError, phoneError, consentError).isNotEmpty()) {
            mutableState.update {
                it.copy(
                    displayNameError = displayNameError,
                    emailError = emailError,
                    passwordError = passwordError,
                    confirmPasswordError = confirmPasswordError,
                    phoneError = phoneError,
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
                confirmPasswordError = null,
                phoneError = null,
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
                    phone = phone.takeIf { current.wantsToSell },
                    role = if (current.wantsToSell) SELLER_ROLE else BUYER_ROLE,
                    adult = current.isAdult,
                    acceptsTerms = current.acceptsTerms,
                    acceptsLocation = current.locationEnabled,
                    acceptsNotifications = current.notificationsEnabled,
                )
            val result =
                try {
                    register(request)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Result.failure(e)
                }
            if (result.isSuccess) {
                val signedIn =
                    try {
                        signIn(email, current.password)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Result.failure(e)
                    }
                // Con la sesión iniciada esta pantalla desaparece, por eso el formulario (y la clave) se vacían.
                mutableState.value = if (signedIn.isSuccess) SignUpUiState() else SignUpUiState(isLoading = false, successMessage = ACCOUNT_CREATED)
                return@launch
            }
            val error = result.exceptionOrNull()
            val rejected = (error as? ApiError)?.takeIf { it.code in REJECTED_CODES }
            mutableState.update {
                it.copy(
                    isLoading = false,
                    displayNameError = rejected?.fieldError(DISPLAY_NAME_FIELD),
                    emailError = rejected?.fieldError(EMAIL_FIELD),
                    passwordError = rejected?.fieldError(PASSWORD_FIELD),
                    phoneError = rejected?.fieldError(PHONE_FIELD),
                    formError = error?.toFormError(),
                )
            }
        }
    }

    private fun Throwable.toFormError(): String {
        val error = this as? ApiError ?: return SERVER_ERROR
        if (error.code == 0) return NO_CONNECTION
        if (error.code !in REJECTED_CODES) return SERVER_ERROR
        val others = error.otherErrors(DISPLAY_NAME_FIELD, EMAIL_FIELD, PASSWORD_FIELD, PHONE_FIELD)
        return when {
            others.isNotEmpty() -> others.joinToString(" ")
            error.details.isEmpty() && error.code == 409 -> EMAIL_TAKEN
            else -> INVALID_DATA
        }
    }

    companion object {
        const val EMPTY_NAME = "Ingresa un nombre visible"
        const val WEAK_PASSWORD = "Usa 10 o más caracteres con mayúscula, minúscula, número y un símbolo (@#$%^&+=!)"
        const val PHONE_REQUIRED = "Para vender necesitamos un celular de contacto"
        const val INVALID_PHONE = "Ingresa un celular chileno, por ejemplo +56 9 1234 5678"
        const val CONSENT_REQUIRED = "Debes aceptar ambas casillas para crear tu cuenta."
        const val EMAIL_TAKEN = "Ya existe una cuenta con ese correo."
        const val INVALID_DATA = "Revisa los datos ingresados e inténtalo de nuevo."
        const val NO_CONNECTION = "Sin conexión. Revisa tu internet e inténtalo de nuevo."
        const val SERVER_ERROR = "Algo salió mal de nuestro lado. Inténtalo más tarde."
        const val ACCOUNT_CREATED = "Cuenta creada. Inicia sesión con tu correo y contraseña."
        const val PASSWORDS_DIFFER = "Las contraseñas no coinciden"
        const val MIN_PASSWORD_LENGTH = 10
        private const val BUYER_ROLE = "BUYER"
        private const val SELLER_ROLE = "SELLER"
        private const val DISPLAY_NAME_FIELD = "displayName"
        private const val EMAIL_FIELD = "email"
        private const val PASSWORD_FIELD = "password"
        private const val PHONE_FIELD = "phone"
        private val REJECTED_CODES = setOf(400, 409, 422)
        private val PASSWORD_PATTERN = Regex("""^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\S+$).{10,128}$""")
        private val PHONE_PATTERN = Regex("""^(\+?56)?\s?9\s?\d{4}\s?\d{4}$""")

        fun isStrongPassword(value: String): Boolean = PASSWORD_PATTERN.matches(value)

        fun isValidPhone(value: String): Boolean = PHONE_PATTERN.matches(value)
    }
}
