package cl.udec.cocinavecina.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.udec.cocinavecina.ui.common.CocinaVecinaLogo
import cl.udec.cocinavecina.ui.components.LabeledTextField
import cl.udec.cocinavecina.ui.theme.CocinaVecinaTheme

@Composable
fun LoginScreen(
    state: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onCreateAccountClick: () -> Unit,
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
                    "Inicia sesión",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    "Ingresa con tu correo y contraseña para ver pedidos, propuestas y tu cocina vecinal.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
                placeholder = "Tu contraseña",
                errorText = state.passwordError,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                keyboardActions = KeyboardActions(onDone = { onLoginClick() }),
                visualTransformation = PasswordVisualTransformation(),
                labelAction = {
                    Text(
                        "¿Olvidaste tu clave?",
                        modifier =
                            Modifier
                                .minimumInteractiveComponentSize()
                                .clickable(role = Role.Button, onClick = onForgotPasswordClick),
                        fontSize = 12.sp,
                        textDecoration = TextDecoration.Underline,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
        }
        LoginBottomBar(state, onLoginClick, onCreateAccountClick)
    }
}

@Composable
private fun LoginBottomBar(
    state: LoginUiState,
    onLoginClick: () -> Unit,
    onCreateAccountClick: () -> Unit,
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
            Button(
                onClick = onLoginClick,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        disabledContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
            ) {
                if (state.isLoading) {
                    Box(Modifier.semantics { contentDescription = "Iniciando sesión" }) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                } else {
                    Text("Iniciar sesión", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Text(
                text =
                    buildAnnotatedString {
                        append("¿No tienes cuenta? ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)) {
                            append("Crea tu cuenta")
                        }
                    },
                modifier =
                    Modifier
                        .minimumInteractiveComponentSize()
                        .clickable(role = Role.Button, onClick = onCreateAccountClick),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun LoginScreenPreview() {
    CocinaVecinaTheme {
        LoginScreen(LoginUiState(), {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun LoginScreenErrorsPreview() {
    CocinaVecinaTheme {
        LoginScreen(
            state =
                LoginUiState(
                    email = "correo-invalido",
                    emailError = LoginViewModel.INVALID_EMAIL,
                    passwordError = LoginViewModel.EMPTY_PASSWORD,
                    formError = LoginViewModel.INVALID_CREDENTIALS,
                ),
            onEmailChange = {},
            onPasswordChange = {},
            onLoginClick = {},
            onForgotPasswordClick = {},
            onCreateAccountClick = {},
        )
    }
}
