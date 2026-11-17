package cl.udec.cocinavecina.ui.verification

import android.content.Intent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import cl.udec.cocinavecina.data.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface VerificationState {
    data object Idle : VerificationState

    data object Verifying : VerificationState

    data class Done(val message: String) : VerificationState

    data class Failed(val message: String) : VerificationState
}

object VerificationLink {
    private const val PATH = "/v1/auth/verify"

    /** Token del enlace del correo (`/v1/auth/verify?token=...`), o null si el intent es otra cosa. */
    fun tokenFrom(intent: Intent?): String? {
        val uri = intent?.takeIf { it.action == Intent.ACTION_VIEW }?.data ?: return null
        if (uri.path != PATH) return null
        return uri.getQueryParameter("token")?.takeIf { it.isNotBlank() }
    }
}

/** Confirma el correo con el token del enlace (RF-1). Vive en [cl.udec.cocinavecina.AppContainer] para sobrevivir a rotaciones. */
class EmailVerification(
    private val repository: Repository,
    private val scope: CoroutineScope,
    private val onVerified: () -> Unit,
) {
    private val mutableState = MutableStateFlow<VerificationState>(VerificationState.Idle)
    val state: StateFlow<VerificationState> = mutableState.asStateFlow()

    private val mutableSuccessCount = MutableStateFlow(0)

    /** Sube con cada verificación exitosa; las pantallas lo usan para recargar la cuenta. */
    val successCount: StateFlow<Int> = mutableSuccessCount.asStateFlow()

    fun confirm(token: String) {
        if (mutableState.value == VerificationState.Verifying) return
        mutableState.value = VerificationState.Verifying
        scope.launch {
            repository.call { verify(token).close() }.fold(
                onSuccess = {
                    mutableState.value = VerificationState.Done("Correo verificado. Ya puedes publicar y comprar.")
                    mutableSuccessCount.update { it + 1 }
                    onVerified()
                },
                onFailure = { mutableState.value = VerificationState.Failed(it.message ?: "No pudimos verificar el correo.") },
            )
        }
    }

    fun dismiss() {
        if (mutableState.value != VerificationState.Verifying) mutableState.value = VerificationState.Idle
    }
}

@Composable
fun VerificationDialog(
    state: VerificationState,
    onDismiss: () -> Unit,
) {
    if (state == VerificationState.Idle) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (state is VerificationState.Failed) "No se pudo verificar" else "Verificación de correo") },
        text = {
            when (state) {
                VerificationState.Verifying -> CircularProgressIndicator()
                is VerificationState.Done -> Text(state.message)
                is VerificationState.Failed -> Text(state.message)
                VerificationState.Idle -> Unit
            }
        },
        confirmButton = {
            if (state != VerificationState.Verifying) TextButton(onClick = onDismiss) { Text("Entendido") }
        },
    )
}
