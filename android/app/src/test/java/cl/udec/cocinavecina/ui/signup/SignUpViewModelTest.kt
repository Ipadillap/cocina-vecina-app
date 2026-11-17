package cl.udec.cocinavecina.ui.signup

import cl.udec.cocinavecina.data.ApiError
import cl.udec.cocinavecina.data.network.SignUpRequest
import cl.udec.cocinavecina.data.network.SignUpResponse
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

@OptIn(ExperimentalCoroutinesApi::class)
class SignUpViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val sent = mutableListOf<SignUpRequest>()
    private val signedIn = mutableListOf<Pair<String, String>>()

    private fun filledViewModel(
        result: Result<SignUpResponse> = Result.success(SignUpResponse(10, "Revisa tu correo.")),
        signInResult: Result<Unit> = Result.success(Unit),
    ) = SignUpViewModel(
        register = { request ->
            sent += request
            result
        },
        signIn = { email, password ->
            signedIn += email to password
            signInResult
        },
    ).apply {
        onDisplayNameChange("Ana")
        onEmailChange("ana@correo.cl")
        onPasswordChange("Clave#Larga2026")
        onConfirmPasswordChange("Clave#Larga2026")
        onAdultChange(true)
        onTermsChange(true)
    }

    @Test
    fun successfulSignUpSignsInAndClearsForm() {
        val viewModel = filledViewModel()
        viewModel.onSignUpClick()
        assertEquals(listOf("ana@correo.cl" to "Clave#Larga2026"), signedIn)
        assertEquals(SignUpUiState(), viewModel.uiState.value)
    }

    @Test
    fun signInFailureAfterSignUpShowsAccountCreated() {
        val viewModel = filledViewModel(signInResult = Result.failure(IllegalStateException("sin red")))
        viewModel.onSignUpClick()
        val state = viewModel.uiState.value
        assertEquals(SignUpViewModel.ACCOUNT_CREATED, state.successMessage)
        assertFalse(state.isLoading)
        assertEquals("", state.password)
    }

    @Test
    fun failedSignUpDoesNotSignIn() {
        val viewModel = filledViewModel(Result.failure(ApiError(0, "Sin conexión con el servidor")))
        viewModel.onSignUpClick()
        assertTrue(signedIn.isEmpty())
        assertNull(viewModel.uiState.value.successMessage)
        assertEquals(SignUpViewModel.NO_CONNECTION, viewModel.uiState.value.formError)
    }

    @Test
    fun differentPasswordsAreNotSent() {
        val viewModel = filledViewModel()
        viewModel.onConfirmPasswordChange("Clave#Larga2027")
        viewModel.onSignUpClick()
        assertTrue(sent.isEmpty())
        assertTrue(signedIn.isEmpty())
        assertEquals(SignUpViewModel.PASSWORDS_DIFFER, viewModel.uiState.value.confirmPasswordError)
    }

    @Test
    fun missingConfirmationIsNotSent() {
        val viewModel = SignUpViewModel(register = { Result.success(SignUpResponse(1, "")) }, signIn = { _, _ -> Result.success(Unit) })
        viewModel.onPasswordChange("Clave#Larga2026")
        assertTrue(viewModel.uiState.value.hasEmptyField)
        viewModel.onConfirmPasswordChange("Clave#Larga2026")
        viewModel.onDisplayNameChange("Ana")
        viewModel.onEmailChange("ana@correo.cl")
        assertFalse(viewModel.uiState.value.hasEmptyField)
    }

    @Test
    fun editingPasswordClearsConfirmationError() {
        val viewModel = filledViewModel()
        viewModel.onConfirmPasswordChange("otra")
        viewModel.onSignUpClick()
        viewModel.onPasswordChange("Clave#Larga2026")
        assertNull(viewModel.uiState.value.confirmPasswordError)
    }

    @Test
    fun consentsAreSentToTheBackend() {
        val viewModel = filledViewModel()
        viewModel.onLocationChange(true)
        viewModel.onSignUpClick()
        assertTrue(sent.single().acceptsLocation)
        assertFalse(sent.single().acceptsNotifications)
    }

    @Test
    fun resetClearsFormAndSuccess() {
        val viewModel = filledViewModel(signInResult = Result.failure(IllegalStateException("sin red")))
        viewModel.onSignUpClick()
        assertEquals(SignUpViewModel.ACCOUNT_CREATED, viewModel.uiState.value.successMessage)
        viewModel.reset()
        val state = viewModel.uiState.value
        assertNull(state.successMessage)
        assertEquals("", state.email)
        assertEquals("", state.password)
        assertEquals("", state.confirmPassword)
        assertFalse(state.acceptsTerms)
    }

    @Test
    fun buyerSignUpSendsBuyerRoleWithoutPhone() {
        val viewModel = filledViewModel()
        viewModel.onPhoneChange("+56 9 1111 2222")
        viewModel.onSignUpClick()
        assertEquals("BUYER", sent.single().role)
        assertNull(sent.single().phone)
    }

    @Test
    fun sellerSignUpSendsSellerRoleAndPhone() {
        val viewModel = filledViewModel()
        viewModel.onWantsToSellChange(true)
        viewModel.onPhoneChange(" +56 9 1111 2222 ")
        viewModel.onSignUpClick()
        assertEquals("SELLER", sent.single().role)
        assertEquals("+56 9 1111 2222", sent.single().phone)
    }

    @Test
    fun sellerWithoutPhoneIsNotSent() {
        val viewModel = filledViewModel()
        viewModel.onWantsToSellChange(true)
        viewModel.onSignUpClick()
        assertTrue(sent.isEmpty())
        assertEquals(SignUpViewModel.PHONE_REQUIRED, viewModel.uiState.value.phoneError)
    }

    @Test
    fun sellerWithInvalidPhoneIsNotSent() {
        val viewModel = filledViewModel()
        viewModel.onWantsToSellChange(true)
        viewModel.onPhoneChange("12345")
        viewModel.onSignUpClick()
        assertTrue(sent.isEmpty())
        assertEquals(SignUpViewModel.INVALID_PHONE, viewModel.uiState.value.phoneError)
    }

    @Test
    fun weakPasswordIsNotSent() {
        val viewModel = filledViewModel()
        viewModel.onPasswordChange("una-clave-larga")
        viewModel.onSignUpClick()
        assertTrue(sent.isEmpty())
        assertEquals(SignUpViewModel.WEAK_PASSWORD, viewModel.uiState.value.passwordError)
    }

    @Test
    fun passwordRuleMatchesBackend() {
        assertTrue(SignUpViewModel.isStrongPassword("Clave#Larga2026"))
        assertFalse(SignUpViewModel.isStrongPassword("Cl#ve2026"))
        assertFalse(SignUpViewModel.isStrongPassword("clave#larga2026"))
        assertFalse(SignUpViewModel.isStrongPassword("Clave-Larga2026"))
        assertFalse(SignUpViewModel.isStrongPassword("Clave# Larga2026"))
    }

    @Test
    fun backendFieldErrorsAreShownOnTheirFields() {
        val conflict =
            ApiError(
                409,
                "Ya existe una cuenta con el correo electrónico. Ya existe una cuenta con ese nombre de usuario.",
                listOf(
                    "email: Ya existe una cuenta con el correo electrónico.",
                    "displayName: Ya existe una cuenta con ese nombre de usuario.",
                ),
            )
        val viewModel = filledViewModel(Result.failure(conflict))
        viewModel.onSignUpClick()
        val state = viewModel.uiState.value
        assertEquals("Ya existe una cuenta con el correo electrónico.", state.emailError)
        assertEquals("Ya existe una cuenta con ese nombre de usuario.", state.displayNameError)
        assertEquals(SignUpViewModel.INVALID_DATA, state.formError)
    }

    @Test
    fun backendGeneralErrorIsShownInForm() {
        val rejected = ApiError(422, "Debes ser mayor de edad.", listOf("adult: Debes ser mayor de edad."))
        val viewModel = filledViewModel(Result.failure(rejected))
        viewModel.onSignUpClick()
        assertEquals("Debes ser mayor de edad.", viewModel.uiState.value.formError)
    }
}
