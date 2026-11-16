package cl.udec.cocinavecina.ui.login

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val keepSession: Boolean = true,
    val quickAccess: Boolean = true,
    val isLoading: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val formError: String? = null,
)
