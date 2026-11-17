package cl.udec.cocinavecina.ui.signup

data class SignUpUiState(
    val displayName: String = "",
    val email: String = "",
    val password: String = "",
    val wantsToSell: Boolean = false,
    val phone: String = "",
    val isAdult: Boolean = false,
    val acceptsTerms: Boolean = false,
    val locationEnabled: Boolean = false,
    val notificationsEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val displayNameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val phoneError: String? = null,
    val consentError: String? = null,
    val formError: String? = null,
    val successMessage: String? = null,
) {
    val hasEmptyField: Boolean
        get() = displayName.isBlank() || email.isBlank() || password.isEmpty() || (wantsToSell && phone.isBlank())

    val hasPendingConsent: Boolean
        get() = !isAdult || !acceptsTerms
}
