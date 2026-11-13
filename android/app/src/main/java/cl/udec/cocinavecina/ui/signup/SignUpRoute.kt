package cl.udec.cocinavecina.ui.signup

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.udec.cocinavecina.data.Repository

@Composable
fun SignUpRoute(
    repository: Repository,
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onLoginClick: () -> Unit,
) {
    val viewModel: SignUpViewModel =
        viewModel(
            factory =
                viewModelFactory {
                    initializer {
                        SignUpViewModel(register = { request -> repository.call { register(request) } })
                    }
                },
        )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val locationLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            viewModel.onLocationChange(granted)
        }
    val notificationsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            viewModel.onNotificationsChange(granted)
        }
    SignUpScreen(
        state = state,
        onDisplayNameChange = viewModel::onDisplayNameChange,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onAdultChange = viewModel::onAdultChange,
        onTermsChange = viewModel::onTermsChange,
        onLocationChange = { enabled ->
            if (enabled) {
                locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            } else {
                viewModel.onLocationChange(false)
            }
        },
        onNotificationsChange = { enabled ->
            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.onNotificationsChange(enabled)
            }
        },
        onSignUpClick = viewModel::onSignUpClick,
        onTermsClick = onTermsClick,
        onPrivacyClick = onPrivacyClick,
        onLoginClick = onLoginClick,
    )
}
