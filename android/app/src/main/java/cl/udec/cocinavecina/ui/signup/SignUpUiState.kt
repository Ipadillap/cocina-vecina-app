package cl.udec.cocinavecina.ui.signup

data class SignUpUiState(
    val displayName: String = "",
    val email: String = "",
    val password: String = "",
    val isAdult: Boolean = false,
    val acceptsTerms: Boolean = false,
    val locationEnabled: Boolean = false,
    val notificationsEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val displayNameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val consentError: String? = null,
    val formError: String? = null,
    val successMessage: String? = null,
) {
    val hasEmptyField: Boolean
        get() = displayName.isBlank() || email.isBlank() || password.isEmpty()

    val hasPendingConsent: Boolean
        get() = !isAdult || !acceptsTerms
}
