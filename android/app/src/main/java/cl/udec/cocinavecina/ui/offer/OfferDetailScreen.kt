package cl.udec.cocinavecina.ui.offer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cl.udec.cocinavecina.data.Repository
import coil.compose.AsyncImage
import cl.udec.cocinavecina.data.network.NewPurchaseRequest
import cl.udec.cocinavecina.data.network.NewReport
import cl.udec.cocinavecina.data.network.OfferView
import cl.udec.cocinavecina.ui.common.PrimaryButton
import cl.udec.cocinavecina.ui.common.Loading
import cl.udec.cocinavecina.ui.common.Format
import cl.udec.cocinavecina.ui.common.ErrorMessage
import kotlinx.coroutines.launch

@Composable
fun OfferDetailScreen(repository: Repository, offerId: Long, onSubmitted: () -> Unit) {
    var offer by remember { mutableStateOf<OfferView?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var portions by remember { mutableIntStateOf(1) }
    var price by remember { mutableStateOf("") }
    var reporting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(offerId) {
        repository.call { offer(offerId) }.onSuccess { offer = it }.onFailure { error = it.message }
    }
    val o = offer
    if (o == null) {
        if (error == null) Loading() else ErrorMessage(error)
        return
    }
    val max = minOf(o.availablePortions, 5)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        repository.absoluteUrl(o.photoUrl)?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = o.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(200.dp),
            )
        }
        Text(o.title, style = MaterialTheme.typography.headlineSmall)
        Text("Cocina: ${o.seller} · ${Format.timeLeft(o.expiresAt)}")
        o.description?.let { Text(it) }
        Text("Precio: ${Format.pesos(o.basePrice)} · acepta propuestas desde ${Format.pesos(o.minPrice)}")
        Text("Alérgenos: " + o.allergens.joinToString { Format.allergen(it) })
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Text(
                "Elaborado por un particular sin certificación sanitaria. Retira dentro del horario y consume pronto.",
                Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Porciones")
            OutlinedButton(onClick = { if (portions > 1) portions-- }) { Text("−") }
            Text("$portions", style = MaterialTheme.typography.titleLarge)
            OutlinedButton(onClick = { if (portions < max) portions++ }) { Text("+") }
        }
        OutlinedTextField(
            value = price,
            onValueChange = { price = it.filter(Char::isDigit) },
            label = { Text("Proponer otro precio por porción (opcional)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        ErrorMessage(error)
        PrimaryButton(
            text = if (price.isBlank()) "Solicitar a ${Format.pesos(o.basePrice)}" else "Enviar propuesta",
            enabled = o.availablePortions > 0,
            onClick = {
                scope.launch {
                    repository.call { submit(o.id, NewPurchaseRequest(portions, price.toIntOrNull())) }
                        .onSuccess { onSubmitted() }
                        .onFailure { error = it.message }
                }
            },
        )
        TextButton(onClick = { reporting = true }) { Text("Reportar esta publicación") }
    }

    if (reporting) {
        var reason by remember { mutableStateOf(Format.REPORT_REASONS.first().first) }
        AlertDialog(
            onDismissRequest = { reporting = false },
            title = { Text("¿Por qué la reportas?") },
            text = {
                Column {
                    Format.REPORT_REASONS.forEach { (key, text) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(reason == key, { reason = key })
                            Text(text)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    reporting = false
                    scope.launch { repository.call { report(o.id, NewReport(reason)) } }
                }) { Text("Reportar") }
            },
            dismissButton = { TextButton(onClick = { reporting = false }) { Text("Cancelar") } },
        )
    }
}
