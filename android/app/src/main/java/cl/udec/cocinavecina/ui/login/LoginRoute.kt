package cl.udec.cocinavecina.ui.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.udec.cocinavecina.data.Repository
import cl.udec.cocinavecina.data.network.LoginRequest

@Composable
fun LoginRoute(
    repository: Repository,
    onForgotPasswordClick: () -> Unit,
    onCreateAccountClick: () -> Unit,
) {
    val viewModel: LoginViewModel =
        viewModel(
            factory =
                viewModelFactory {
                    initializer {
                        LoginViewModel(
                            authenticate = { email, password -> repository.call { login(LoginRequest(email, password)) } },
                            saveSession = { repository.session.save(it) },
                        )
                    }
                },
        )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LoginScreen(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onLoginClick = viewModel::onLoginClick,
        onForgotPasswordClick = onForgotPasswordClick,
        onCreateAccountClick = onCreateAccountClick,
    )
}
