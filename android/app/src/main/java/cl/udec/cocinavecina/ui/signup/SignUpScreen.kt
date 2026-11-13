package cl.udec.cocinavecina.ui.signup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.udec.cocinavecina.ui.common.CocinaVecinaLogo
import cl.udec.cocinavecina.ui.components.LabeledTextField
import cl.udec.cocinavecina.ui.components.PreferenceCard
import cl.udec.cocinavecina.ui.components.PreferenceDivider
import cl.udec.cocinavecina.ui.components.SwitchPreferenceRow
import cl.udec.cocinavecina.ui.login.LoginViewModel
import cl.udec.cocinavecina.ui.theme.CocinaVecinaTheme

@Composable
fun SignUpScreen(
    state: SignUpUiState,
    onDisplayNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onAdultChange: (Boolean) -> Unit,
    onTermsChange: (Boolean) -> Unit,
    onLocationChange: (Boolean) -> Unit,
    onNotificationsChange: (Boolean) -> Unit,
    onSignUpClick: () -> Unit,
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onLoginClick: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding(),
    ) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CocinaVecinaLogo()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Crea tu cuenta",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    "Puedes mirar sin cuenta. Para publicar o proponer un precio hay que verificar el correo.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LabeledTextField(
                label = "Nombre visible",
                value = state.displayName,
                onValueChange = onDisplayNameChange,
                placeholder = "Cómo te verán tus vecinos",
                errorText = state.displayNameError,
                imeAction = ImeAction.Next,
            )
            LabeledTextField(
                label = "Correo",
                value = state.email,
                onValueChange = onEmailChange,
                placeholder = "nombre@correo.cl",
                errorText = state.emailError,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            )
            LabeledTextField(
                label = "Contraseña",
                value = state.password,
                onValueChange = onPasswordChange,
                placeholder = "Mínimo ${SignUpViewModel.MIN_PASSWORD_LENGTH} caracteres",
                errorText = state.passwordError,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                keyboardActions = KeyboardActions(onDone = { onSignUpClick() }),
                visualTransformation = PasswordVisualTransformation(),
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PreferenceCard {
                    ConsentRow(
                        text = AnnotatedString("Declaro tener 18 años o más"),
                        checked = state.isAdult,
                        onCheckedChange = onAdultChange,
                    )
                    PreferenceDivider()
                    ConsentRow(
                        text = termsText(onTermsClick, onPrivacyClick),
                        checked = state.acceptsTerms,
                        onCheckedChange = onTermsChange,
                    )
                }
                state.consentError?.let {
                    Text(it, modifier = Modifier.padding(horizontal = 4.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                }
            }
            Text(
                "Permisos, cada uno por separado",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            PreferenceCard {
                SwitchPreferenceRow(
                    title = "Ubicación",
                    subtitle = "Para ordenar por cercanía. Sin ella, buscas por dirección",
                    checked = state.locationEnabled,
                    onCheckedChange = onLocationChange,
                )
                PreferenceDivider()
                SwitchPreferenceRow(
                    title = "Notificaciones",
                    subtitle = "Para avisarte de propuestas y pedidos",
                    checked = state.notificationsEnabled,
                    onCheckedChange = onNotificationsChange,
                )
            }
        }
        SignUpBottomBar(state, onSignUpClick, onLoginClick)
    }
}

@Composable
private fun termsText(
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
): AnnotatedString {
    val linkStyles =
        TextLinkStyles(
            SpanStyle(fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline),
        )
    return buildAnnotatedString {
        append("Acepto los ")
        withLink(LinkAnnotation.Clickable("terms", linkStyles) { onTermsClick() }) { append("Términos") }
        append(" y la ")
        withLink(LinkAnnotation.Clickable("privacy", linkStyles) { onPrivacyClick() }) { append("Política de privacidad") }
    }
}

@Composable
private fun ConsentRow(
    text: AnnotatedString,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
        )
        Text(
            text,
            modifier = Modifier.weight(1f),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun submitLabel(state: SignUpUiState): String =
    when {
        state.successMessage != null -> "Revisa tu correo"
        state.hasEmptyField -> "Completa tus datos"
        state.hasPendingConsent -> "Acepta para continuar"
        else -> "Crear cuenta"
    }

@Composable
private fun SignUpBottomBar(
    state: SignUpUiState,
    onSignUpClick: () -> Unit,
    onLoginClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        shadowElevation = 8.dp,
    ) {
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            state.formError?.let {
                Text(
                    it,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            state.successMessage?.let {
                Text(
                    it,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            Button(
                onClick = onSignUpClick,
                enabled = !state.isLoading && state.successMessage == null,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        disabledContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
            ) {
                if (state.isLoading) {
                    Box(Modifier.semantics { contentDescription = "Creando cuenta" }) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                } else {
                    Text(submitLabel(state), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Text(
                text =
                    buildAnnotatedString {
                        append("Ya tengo cuenta · ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)) {
                            append("Iniciar sesión")
                        }
                    },
                modifier =
                    Modifier
                        .minimumInteractiveComponentSize()
                        .clickable(role = Role.Button, onClick = onLoginClick),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun SignUpScreenPreview() {
    CocinaVecinaTheme {
        SignUpScreen(SignUpUiState(), {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun SignUpScreenFilledPreview() {
    CocinaVecinaTheme {
        SignUpScreen(
            state =
                SignUpUiState(
                    displayName = "Ana",
                    email = "correo-invalido",
                    password = "secreto123",
                    isAdult = true,
                    locationEnabled = true,
                    emailError = LoginViewModel.INVALID_EMAIL,
                    consentError = SignUpViewModel.CONSENT_REQUIRED,
                    formError = SignUpViewModel.EMAIL_TAKEN,
                ),
            onDisplayNameChange = {},
            onEmailChange = {},
            onPasswordChange = {},
            onAdultChange = {},
            onTermsChange = {},
            onLocationChange = {},
            onNotificationsChange = {},
            onSignUpClick = {},
            onTermsClick = {},
            onPrivacyClick = {},
            onLoginClick = {},
        )
    }
}
