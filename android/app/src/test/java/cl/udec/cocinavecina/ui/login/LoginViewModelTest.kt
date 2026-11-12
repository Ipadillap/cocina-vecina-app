package cl.udec.cocinavecina.ui.login

import cl.udec.cocinavecina.data.ApiError
import cl.udec.cocinavecina.data.network.TokenResponse
import kotlinx.coroutines.CompletableDeferred
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

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    private val token = TokenResponse("token", 900, 7L, "BUYER", true)
    private val calls = mutableListOf<Pair<String, String>>()
    private val saved = mutableListOf<TokenResponse>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(result: suspend () -> Result<TokenResponse> = { Result.success(token) }) =
        LoginViewModel(
            authenticate = { email, password ->
                calls += email to password
                result()
            },
            saveSession = { saved += it },
        )

    private fun LoginViewModel.fill(
        email: String = "ana@correo.cl",
        password: String = "secreto",
    ) {
        onEmailChange(email)
        onPasswordChange(password)
    }

    @Test
    fun startsWithKeepSessionAndQuickAccessEnabled() {
        val state = viewModel().uiState.value
        assertTrue(state.keepSession)
        assertTrue(state.quickAccess)
        assertFalse(state.isLoading)
    }

    @Test
    fun invalidEmailShowsErrorWithoutCallingBackend() {
        val vm = viewModel()
        vm.fill(email = "no-es-correo")
        vm.onLoginClick()
        assertEquals("Ingresa un correo válido", vm.uiState.value.emailError)
        assertNull(vm.uiState.value.passwordError)
        assertTrue(calls.isEmpty())
    }

    @Test
    fun emptyPasswordShowsError() {
        val vm = viewModel()
        vm.fill(password = "")
        vm.onLoginClick()
        assertEquals("Ingresa tu contraseña", vm.uiState.value.passwordError)
        assertNull(vm.uiState.value.emailError)
        assertTrue(calls.isEmpty())
    }

    @Test
    fun bothFieldsInvalidShowBothErrors() {
        val vm = viewModel()
        vm.onLoginClick()
        assertEquals("Ingresa un correo válido", vm.uiState.value.emailError)
        assertEquals("Ingresa tu contraseña", vm.uiState.value.passwordError)
    }

    @Test
    fun editingAFieldClearsItsError() {
        val vm = viewModel()
        vm.onLoginClick()
        vm.onEmailChange("a")
        assertNull(vm.uiState.value.emailError)
        assertEquals("Ingresa tu contraseña", vm.uiState.value.passwordError)
    }

    @Test
    fun emailFormatValidation() {
        assertTrue(LoginViewModel.isValidEmail("nombre@correo.cl"))
        assertTrue(LoginViewModel.isValidEmail("a.b+c@sub.dominio.com"))
        assertFalse(LoginViewModel.isValidEmail("nombre@correo"))
        assertFalse(LoginViewModel.isValidEmail("nombre correo@x.cl"))
        assertFalse(LoginViewModel.isValidEmail(""))
    }

    @Test
    fun successfulLoginTrimsEmailAndSavesSession() {
        val vm = viewModel()
        vm.fill(email = "  ana@correo.cl ")
        vm.onLoginClick()
        assertEquals(listOf("ana@correo.cl" to "secreto"), calls)
        assertEquals(listOf(token), saved)
        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.formError)
    }

    @Test
    fun loadingStateWhileRequestIsInFlightAndIgnoresDoubleSubmit() {
        val gate = CompletableDeferred<Result<TokenResponse>>()
        val vm = viewModel { gate.await() }
        vm.fill()
        vm.onLoginClick()
        assertTrue(vm.uiState.value.isLoading)
        vm.onLoginClick()
        assertEquals(1, calls.size)
        gate.complete(Result.success(token))
        assertFalse(vm.uiState.value.isLoading)
    }

    private fun failureMessage(error: Throwable): String? {
        val vm = viewModel { Result.failure(error) }
        vm.fill()
        vm.onLoginClick()
        assertFalse(vm.uiState.value.isLoading)
        assertTrue(saved.isEmpty())
        return vm.uiState.value.formError
    }

    @Test
    fun invalidCredentialsMessage() {
        assertEquals(LoginViewModel.INVALID_CREDENTIALS, failureMessage(ApiError(401, "detalle técnico")))
    }

    @Test
    fun unverifiedAccountMessage() {
        assertEquals(LoginViewModel.UNVERIFIED_ACCOUNT, failureMessage(ApiError(403, "detalle técnico")))
    }

    @Test
    fun noConnectionMessage() {
        assertEquals(LoginViewModel.NO_CONNECTION, failureMessage(ApiError(0, "Sin conexión con el servidor")))
    }

    @Test
    fun serverErrorMessageHidesTechnicalDetails() {
        assertEquals(LoginViewModel.SERVER_ERROR, failureMessage(ApiError(500, "NullPointerException en AuthService")))
        assertEquals(LoginViewModel.SERVER_ERROR, failureMessage(IOException("boom")))
    }
}
