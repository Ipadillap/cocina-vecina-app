package cl.udec.cocinavecina.ui.account

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.udec.cocinavecina.data.Repository
import cl.udec.cocinavecina.location.Locator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

private const val EXPORT_FILE_NAME = "cocinavecina-mis-datos.json"
private const val JSON_MIME_TYPE = "application/json"

@Composable
fun AccountRoute(
    repository: Repository,
    verificationVersion: Int,
    onModerationClick: () -> Unit,
    onLogout: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: AccountViewModel =
        viewModel(
            factory =
                viewModelFactory {
                    initializer {
                        AccountViewModel(
                            loadAccount = { repository.call { account() } },
                            loadPickupPoints = { repository.call { myPickupPoints() } },
                            exportData = { repository.call { withContext(Dispatchers.IO) { exportAccount().use { it.string() } } } },
                            deleteAccount = {
                                repository.call {
                                    val response = deleteAccount()
                                    if (!response.isSuccessful) throw HttpException(response)
                                }
                            },
                        )
                    }
                },
        )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(JSON_MIME_TYPE)) { uri ->
            if (uri != null) {
                viewModel.onExportDestinationChosen { content -> context.writeText(uri, content) }
            }
        }
    LifecycleResumeEffect(verificationVersion) {
        viewModel.refresh()
        viewModel.onPermissionsChanged(
            isLocationGranted = Locator.hasPermission(context),
            areNotificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled(),
        )
        onPauseOrDispose { }
    }
    LaunchedEffect(state.isExportPickerRequested) {
        if (state.isExportPickerRequested) {
            viewModel.onExportPickerShown()
            exportLauncher.launch(EXPORT_FILE_NAME)
        }
    }
    LaunchedEffect(state.isAccountDeleted) {
        if (state.isAccountDeleted) onLogout()
    }
    AccountScreen(
        state = state,
        onRetry = viewModel::onRetry,
        onPermissionClick = {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
            runCatching { context.startActivity(intent) }
        },
        onTermsClick = viewModel::onTermsClick,
        onTermsDismiss = viewModel::onTermsDismiss,
        onExportClick = viewModel::onExportClick,
        onDeleteClick = viewModel::onDeleteClick,
        onDeleteDismiss = viewModel::onDeleteDismiss,
        onDeleteConfirm = viewModel::onDeleteConfirm,
        onModerationClick = onModerationClick,
        onLogoutClick = onLogout,
    )
}

private suspend fun Context.writeText(
    uri: Uri,
    content: String,
) = withContext(Dispatchers.IO) {
    val stream = contentResolver.openOutputStream(uri) ?: throw IOException("No se pudo abrir el archivo.")
    stream.use { it.write(content.toByteArray()) }
}
