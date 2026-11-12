package cl.udec.cocinavecina.ui.publish

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cl.udec.cocinavecina.data.Repository
import cl.udec.cocinavecina.data.network.NewOffer
import cl.udec.cocinavecina.data.network.NewPickupPoint
import cl.udec.cocinavecina.data.network.PickupPointView
import cl.udec.cocinavecina.location.Locator
import cl.udec.cocinavecina.ui.common.ErrorMessage
import cl.udec.cocinavecina.ui.common.Format
import cl.udec.cocinavecina.ui.common.PrimaryButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant

private const val NONE_KNOWN = "NONE_KNOWN"

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PublishScreen(
    repository: Repository,
    onPublished: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pickupPoints by remember { mutableStateOf<List<PickupPointView>>(emptyList()) }
    var pointId by remember { mutableStateOf<Long?>(null) }
    var alias by remember { mutableStateOf("Mi casa") }
    var address by remember { mutableStateOf("") }
    var useLocation by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("MAIN_COURSE") }
    var portions by remember { mutableIntStateOf(4) }
    var basePrice by remember { mutableStateOf("3500") }
    var minPrice by remember { mutableStateOf("2500") }
    val allergens = remember { mutableStateListOf<String>() }
    var containsExcluded by remember { mutableStateOf(false) }
    var preparedMinutesAgo by remember { mutableIntStateOf(30) }
    var durationMinutes by remember { mutableIntStateOf(120) }
    var photoKey by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        repository.call { myPickupPoints() }.onSuccess {
            pickupPoints = it
            pointId = it.firstOrNull()?.id
        }
    }
    val pickPhoto =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                scope.launch {
                    val bytes = withContext(Dispatchers.IO) { Photos.compress(context, uri) }
                    repository.uploadPhoto(bytes).onSuccess { photoKey = it }.onFailure { error = it.message }
                }
            }
        }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Publicar porciones", style = MaterialTheme.typography.headlineSmall)

        if (pickupPoints.isEmpty()) {
            Text("Punto de retiro (la dirección solo la verá quien acuerde la compra)")
            OutlinedTextField(
                alias,
                { alias = it },
                label = { Text("Nombre del punto") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                address,
                { address = it },
                label = { Text("Dirección") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(useLocation, { useLocation = it })
                Text("Usar mi ubicación actual en vez de buscar la dirección")
            }
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                pickupPoints.forEach { p -> FilterChip(pointId == p.id, { pointId = p.id }, label = { Text(p.alias) }) }
            }
        }

        OutlinedButton(onClick = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
            Text(if (photoKey == null) "Agregar foto" else "Foto lista ✓")
        }
        OutlinedTextField(title, {
            title = it.take(80)
        }, label = { Text("¿Qué cocinaste?") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Format.CATEGORIES.forEach { (key, text) -> FilterChip(category == key, { category = key }, label = { Text(text) }) }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Porciones")
            OutlinedButton(onClick = { if (portions > 1) portions-- }) { Text("−") }
            Text("$portions", style = MaterialTheme.typography.titleLarge)
            OutlinedButton(onClick = { if (portions < 50) portions++ }) { Text("+") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                basePrice,
                { basePrice = it.filter(Char::isDigit) },
                label = { Text("Precio") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                minPrice,
                { minPrice = it.filter(Char::isDigit) },
                label = { Text("Mínimo aceptado") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
        }
        Text("Alérgenos (obligatorio)")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(NONE_KNOWN in allergens, {
                allergens.clear()
                allergens.add(NONE_KNOWN)
            }, label = { Text("Ninguno conocido") })
            Format.ALLERGENS.forEach { (key, text) ->
                FilterChip(key in allergens, {
                    allergens.remove(NONE_KNOWN)
                    if (key in allergens) allergens.remove(key) else allergens.add(key)
                }, label = { Text(text) })
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(containsExcluded, { containsExcluded = it })
            Text(
                "Contiene alcohol, mariscos crudos, huevo crudo, lácteos sin pasteurizar, conservas caseras o es para lactantes",
                Modifier.padding(start = 8.dp),
            )
        }
        if (containsExcluded) ErrorMessage("Estas preparaciones no se pueden publicar (RB-7).")
        Text("Preparado hace")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(0, 30, 60, 90).forEach {
                    m ->
                FilterChip(preparedMinutesAgo == m, { preparedMinutesAgo = m }, label = { Text("$m min") })
            }
        }
        Text("Disponible por")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(30 to "30 min", 60 to "1 h", 120 to "2 h", 180 to "3 h").forEach { (m, text) ->
                FilterChip(durationMinutes == m, { durationMinutes = m }, label = { Text(text) })
            }
        }
        ErrorMessage(error)
        PrimaryButton(
            text = "Publicar",
            enabled = !busy && !containsExcluded && title.isNotBlank() && allergens.isNotEmpty(),
            onClick = {
                busy = true
                error = null
                scope.launch {
                    val id =
                        pointId ?: run {
                            val here = if (useLocation) Locator.lastKnown(context) else null
                            repository.call {
                                createPickupPoint(NewPickupPoint(alias, address.ifBlank { alias }, null, here?.lat, here?.lon))
                            }.onFailure { error = it.message }.getOrNull()?.id
                        }
                    if (id != null) {
                        val now = Instant.now()
                        val newOffer =
                            NewOffer(
                                pickupPointId = id,
                                title = title.trim(),
                                description = null,
                                category = category,
                                photoKey = photoKey,
                                basePrice = basePrice.toIntOrNull() ?: 0,
                                minPrice = minPrice.toIntOrNull() ?: 0,
                                portions = portions,
                                allergens = allergens.toList(),
                                excludedIngredients = emptyList(),
                                preparedAt = now.minus(Duration.ofMinutes(preparedMinutesAgo.toLong())).toString(),
                                expiresAt = now.plus(Duration.ofMinutes(durationMinutes.toLong())).toString(),
                            )
                        repository.call { publish(newOffer) }.onSuccess { onPublished() }.onFailure { error = it.message }
                    }
                    busy = false
                }
            },
        )
    }
}
