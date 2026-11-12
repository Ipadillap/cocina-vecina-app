package cl.udec.cocinavecina.ui.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import cl.udec.cocinavecina.data.Repository
import cl.udec.cocinavecina.data.network.SignUpRequest
import cl.udec.cocinavecina.ui.common.ErrorMessage
import cl.udec.cocinavecina.ui.common.PrimaryButton
import kotlinx.coroutines.launch

@Composable
fun SignUpScreen(
    repository: Repository,
    onBackToLogin: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("BUYER") }
    var adult by remember { mutableStateOf(false) }
    var terms by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("CocinaVecina", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
        Text("Crear cuenta", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            email,
            { email = it },
            label = { Text("Correo") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            password,
            { password = it },
            label = { Text("Contraseña") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(name, { name = it }, label = { Text("Nombre visible") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            phone,
            { phone = it },
            label = { Text("Teléfono (se muestra solo tras un acuerdo)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(role == "BUYER", { role = "BUYER" }, label = { Text("Quiero comprar") })
            FilterChip(role == "SELLER", { role = "SELLER" }, label = { Text("También quiero vender") })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(adult, { adult = it })
            Text("Declaro tener 18 años o más")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(terms, { terms = it })
            Text("Acepto los términos y la política de privacidad")
        }
        ErrorMessage(error)
        notice?.let { Text(it, color = MaterialTheme.colorScheme.secondary) }
        PrimaryButton(
            text = "Crear cuenta",
            enabled = !busy,
            onClick = {
                busy = true
                error = null
                scope.launch {
                    repository.call {
                        register(
                            SignUpRequest(email.trim(), name.trim(), password, phone.ifBlank { null }, role, adult, terms),
                        )
                    }.onSuccess { notice = it.message }.onFailure { error = it.message }
                    busy = false
                }
            },
        )
        TextButton(onClick = onBackToLogin) { Text("Ya tengo cuenta") }
    }
}
