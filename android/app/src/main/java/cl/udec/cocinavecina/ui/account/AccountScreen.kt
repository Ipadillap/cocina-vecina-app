package cl.udec.cocinavecina.ui.account

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.udec.cocinavecina.data.network.AccountView
import cl.udec.cocinavecina.data.network.PickupPointView
import cl.udec.cocinavecina.ui.components.PreferenceCard
import cl.udec.cocinavecina.ui.components.PreferenceDivider
import cl.udec.cocinavecina.ui.theme.CocinaVecinaTheme

private val ScreenPadding = 16.dp
private const val DELETION_DAYS = 30

@Composable
fun AccountScreen(
    state: AccountUiState,
    onRetry: () -> Unit,
    onPermissionClick: () -> Unit,
    onTermsClick: () -> Unit,
    onTermsDismiss: () -> Unit,
    onExportClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDeleteDismiss: () -> Unit,
    onDeleteConfirm: () -> Unit,
    onModerationClick: () -> Unit,
    onLogoutClick: () -> Unit,
) {
    val account = state.account
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        when {
            account != null ->
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(ScreenPadding),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    ProfileHeader(state, account)
                    PersonalDataCard(state, account)
                    PermissionsCard(state, onPermissionClick)
                    TermsCard(state, onTermsClick)
                    if (state.isModerator) {
                        ModerationCard(onModerationClick)
                    }
                    DataOwnershipCard(state, onExportClick, onDeleteClick)
                    state.message?.let {
                        Text(it, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.tertiary)
                    }
                    state.actionError?.let {
                        Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                    }
                    LogoutButton(onLogoutClick)
                }
            state.isLoading ->
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            else ->
                Column(
                    Modifier.weight(1f).fillMaxWidth().padding(ScreenPadding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        state.loadError ?: AccountViewModel.LOAD_ERROR,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    TextButton(onClick = onRetry) { Text("Reintentar", fontWeight = FontWeight.SemiBold) }
                    LogoutButton(onLogoutClick)
                }
        }
    }
    if (state.isTermsVisible) {
        TermsDialog(state, onTermsDismiss)
    }
    if (state.isDeleteDialogVisible) {
        DeleteDialog(onDeleteDismiss, onDeleteConfirm)
    }
}

@Composable
private fun ProfileHeader(
    state: AccountUiState,
    account: AccountView,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(state.initial, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimary)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(account.displayName, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
                Text(account.email, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (account.verified) {
                    Row(
                        Modifier
                            .padding(top = 4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.tertiary)
                        Text("Correo verificado", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                } else {
                    Text(
                        "Correo sin verificar. Abre el enlace que te enviamos para publicar y comprar.",
                        modifier =
                            Modifier
                                .padding(top = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }
        ModeSelector(state)
    }
}

@Composable
private fun ModeSelector(state: AccountUiState) {
    val modes =
        if (state.isModerator) {
            listOf(state.modeLabel)
        } else {
            listOf("Solo compra", "Compra y vende")
        }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.fillMaxWidth().padding(4.dp)) {
            modes.forEach { mode ->
                val selected = mode == state.modeLabel
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .semantics { contentDescription = if (selected) "Modo actual: $mode" else mode }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        mode,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        PreferenceCard(content = content)
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    action: String? = null,
    onActionClick: () -> Unit = {},
) {
    val clickable = action != null
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (clickable) Modifier.clickable(role = Role.Button, onClick = onActionClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = valueColor, textAlign = TextAlign.End)
        action?.let {
            Text(
                it,
                modifier = Modifier.padding(start = 12.dp),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun PersonalDataCard(
    state: AccountUiState,
    account: AccountView,
) {
    SectionCard("Mis datos") {
        InfoRow("Nombre", account.displayName)
        PreferenceDivider()
        InfoRow("Teléfono", state.maskedPhone ?: "Sin teléfono")
        if (state.isSeller) {
            PreferenceDivider()
            InfoRow("Puntos de retiro", state.pickupPointsSummary ?: "Sin puntos de retiro")
        }
    }
}

@Composable
private fun PermissionsCard(
    state: AccountUiState,
    onPermissionClick: () -> Unit,
) {
    SectionCard("Permisos que diste") {
        PermissionRow("Ubicación", "Para mostrarte comida cerca de ti", state.isLocationGranted, onPermissionClick)
        PreferenceDivider()
        PermissionRow("Notificaciones", "Contraofertas y avisos de tus pedidos", state.areNotificationsEnabled, onPermissionClick)
    }
}

@Composable
private fun PermissionRow(
    title: String,
    subtitle: String,
    granted: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            if (granted) "Activada" else "Desactivada",
            modifier =
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (granted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (granted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TermsCard(
    state: AccountUiState,
    onTermsClick: () -> Unit,
) {
    SectionCard("Términos y privacidad") {
        InfoRow(
            label = listOfNotNull("Versión", state.termsVersionLabel).joinToString(" "),
            value = state.termsAcceptedLabel?.let { "Aceptados el $it" } ?: "Aceptados",
            action = "Leer",
            onActionClick = onTermsClick,
        )
    }
}

@Composable
private fun ModerationCard(onModerationClick: () -> Unit) {
    SectionCard("Moderación") {
        InfoRow("Ofertas ocultas por reportes", "", action = "Revisar", onActionClick = onModerationClick)
    }
}

@Composable
private fun DataOwnershipCard(
    state: AccountUiState,
    onExportClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    SectionCard("Tus datos son tuyos") {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Puedes descargar todo lo que guardamos sobre ti o eliminar tu cuenta cuando quieras.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = onExportClick,
                enabled = !state.isActing,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            ) {
                if (state.pendingAction == AccountAction.EXPORT) {
                    BusyIndicator(MaterialTheme.colorScheme.primary, "Descargando")
                } else {
                    Text("Descargar mis datos (JSON)", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            OutlinedButton(
                onClick = onDeleteClick,
                enabled = !state.isActing,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
            ) {
                if (state.pendingAction == AccountAction.DELETE) {
                    BusyIndicator(MaterialTheme.colorScheme.secondary, "Eliminando")
                } else {
                    Text("Eliminar mi cuenta", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Text(
                "Al eliminarla, borramos o anonimizamos tus datos dentro de $DELETION_DAYS días.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LogoutButton(onLogoutClick: () -> Unit) {
    var confirming by rememberSaveable { mutableStateOf(false) }
    TextButton(onClick = { confirming = true }, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
        Text("Cerrar sesión", modifier = Modifier.padding(start = 8.dp), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text("¿Cerrar sesión?", fontWeight = FontWeight.Bold) },
            text = { Text("Vas a volver a la pantalla de inicio de sesión. Puedes entrar con otra cuenta.") },
            confirmButton = {
                TextButton(onClick = {
                    confirming = false
                    onLogoutClick()
                }) {
                    Text("Cerrar sesión", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirming = false }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun BusyIndicator(
    color: Color,
    description: String,
) {
    Box(Modifier.semantics { contentDescription = description }) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = color)
    }
}

@Composable
private fun TermsDialog(
    state: AccountUiState,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(listOfNotNull("Términos y privacidad", state.termsVersionLabel).joinToString(" "), fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TERMS_SUMMARY.forEach { Text("• $it", fontSize = 14.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Entendido", fontWeight = FontWeight.SemiBold) }
        },
    )
}

@Composable
private fun DeleteDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
        title = { Text("¿Eliminar tu cuenta?", fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "Se cerrará tu sesión y borraremos o anonimizaremos tus datos dentro de $DELETION_DAYS días. " +
                    "Tus ofertas activas se retirarán. Esta acción no se puede deshacer.",
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Eliminar cuenta", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

private val TERMS_SUMMARY =
    listOf(
        "CocinaVecina conecta a vecinos que venden y compran porciones de comida casera; no prepara ni transporta comida.",
        "El pago se hace en persona al retirar. La app no cobra ni guarda medios de pago.",
        "En el mapa solo mostramos una zona aproximada de 400 m. La dirección exacta se ve recién al cerrar el trato.",
        "Quien vende declara los alérgenos y es responsable de la higiene de lo que ofrece.",
        "Puedes descargar tus datos o eliminar tu cuenta cuando quieras desde esta pantalla.",
    )

private fun previewState() =
    AccountUiState(
        account =
            AccountView(
                id = 2,
                email = "marta@ejemplo.cl",
                displayName = "Marta R.",
                phone = "+56987650142",
                role = SELLER_ROLE,
                verified = true,
                termsVersion = "1.0",
                termsAcceptedAt = "2026-10-08T15:00:00Z",
            ),
        pickupPoints = listOf(PickupPointView(1, "Casa", -36.82, -73.05)),
        isLocationGranted = true,
        areNotificationsEnabled = false,
        isLoading = false,
    )

@Preview(showBackground = true, heightDp = 1200, widthDp = 390)
@Composable
private fun AccountScreenPreview() {
    CocinaVecinaTheme {
        AccountScreen(previewState(), {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
    }
}
