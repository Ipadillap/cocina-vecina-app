package cl.udec.cocinavecina.ui.signup

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

    private fun filledViewModel() =
        SignUpViewModel(register = { Result.success(SignUpResponse(10, "Revisa tu correo.")) }).apply {
            onDisplayNameChange("Ana")
            onEmailChange("ana@correo.cl")
            onPasswordChange("una-clave-larga")
            onAdultChange(true)
            onTermsChange(true)
        }

    @Test
    fun successfulSignUpShowsMessage() {
        val viewModel = filledViewModel()
        viewModel.onSignUpClick()
        assertEquals("Revisa tu correo.", viewModel.uiState.value.successMessage)
    }

    @Test
    fun resetClearsFormAndSuccess() {
        val viewModel = filledViewModel()
        viewModel.onSignUpClick()
        viewModel.reset()
        val state = viewModel.uiState.value
        assertNull(state.successMessage)
        assertEquals("", state.email)
        assertEquals("", state.password)
        assertFalse(state.acceptsTerms)
    }
}
