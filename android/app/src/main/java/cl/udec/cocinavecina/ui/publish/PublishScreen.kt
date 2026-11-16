@file:OptIn(ExperimentalLayoutApi::class)

package cl.udec.cocinavecina.ui.publish

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.udec.cocinavecina.data.Repository
import cl.udec.cocinavecina.data.network.NewOffer
import cl.udec.cocinavecina.data.network.NewPickupPoint
import cl.udec.cocinavecina.data.network.PickupPointView
import cl.udec.cocinavecina.location.AddressGeocoder
import cl.udec.cocinavecina.location.Coordinate
import cl.udec.cocinavecina.location.Grid
import cl.udec.cocinavecina.location.Locator
import cl.udec.cocinavecina.ui.common.ErrorMessage
import cl.udec.cocinavecina.ui.common.Format
import cl.udec.cocinavecina.ui.common.PrimaryButton
import cl.udec.cocinavecina.ui.explore.OffersMap
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val NONE_KNOWN = "NONE_KNOWN"
private const val TOTAL_STEPS = 3

/**
 * Todo lo que el usuario va completando a lo largo de los 3 pasos.
 * Vive en la pantalla padre, así no se pierde al avanzar/retroceder entre pasos
 * ni al rotar el teléfono (ver [Saver]).
 */
@Stable
private class PublishDraft(
    title: String = "",
    category: String = "MAIN_COURSE",
    allergens: List<String> = emptyList(),
    containsExcluded: Boolean = false,
    photoKey: String? = null,
    portions: Int = 4,
    basePrice: Int = 3500,
    minPrice: Int = 2800,
    preparedAtMs: Long,
    endsAtMs: Long,
    pointId: Long? = null,
    alias: String = "Mi casa",
    address: String = "",
    pinLat: Double? = null,
    pinLon: Double? = null,
) {
    // Paso 1
    var title by mutableStateOf(title)
    var category by mutableStateOf(category)
    var allergens by mutableStateOf(allergens)
    var containsExcluded by mutableStateOf(containsExcluded)
    var photoKey by mutableStateOf(photoKey)

    // Paso 2
    var portions by mutableStateOf(portions)
    var basePrice by mutableStateOf(basePrice)
    var minPrice by mutableStateOf(minPrice)

    // Paso 3
    var preparedAtMs by mutableStateOf(preparedAtMs)
    var endsAtMs by mutableStateOf(endsAtMs)
    var pointId by mutableStateOf(pointId)
    var alias by mutableStateOf(alias)
    var address by mutableStateOf(address)
    var pinLat by mutableStateOf(pinLat)
    var pinLon by mutableStateOf(pinLon)

    val step1Valid: Boolean get() = title.isNotBlank() && allergens.isNotEmpty() && !containsExcluded

    /** Punto fijado con GPS, mapa o dirección geocodificada; el servidor lo reduce a la celda de 400 m. */
    val pin: Coordinate? get() = pinLat?.let { lat -> pinLon?.let { lon -> Coordinate(lat, lon) } }

    fun setPin(point: Coordinate) {
        pinLat = point.lat
        pinLon = point.lon
    }

    /** Se puede publicar con un punto de retiro ya guardado o creando uno nuevo (nombre + dirección o punto fijado). */
    val hasPickupPoint: Boolean get() = pointId != null || (alias.isNotBlank() && (address.isNotBlank() || pin != null))

    // ---- Precio: el mínimo aceptado va entre el 50 % (redondeado a $100) y el precio base
    val minAllowed: Int get() = PublishRules.lowestAcceptedPrice(basePrice)

    fun changeBase(delta: Int) {
        basePrice = (basePrice + delta).coerceIn(PublishRules.MIN_PRICE, PublishRules.MAX_PRICE)
        minPrice = minPrice.coerceIn(minAllowed, basePrice)
    }

    // ---- Horario
    val prepared: Instant get() = Instant.ofEpochMilli(preparedAtMs)
    val endsAt: Instant get() = Instant.ofEpochMilli(endsAtMs)
    val maxEnd: Instant get() = PublishRules.maxSaleEnd(prepared)

    fun movePrepared(deltaMin: Long, now: Instant) {
        preparedAtMs = PublishRules.clampPrepared(prepared.plus(deltaMin, ChronoUnit.MINUTES), now).toEpochMilli()
        endsAtMs = PublishRules.clampSaleEnd(endsAt, prepared, now).toEpochMilli() // el máximo depende de la hora de preparación
    }

    fun moveEnd(deltaMin: Long, now: Instant) {
        endsAtMs = PublishRules.clampSaleEnd(endsAt.plus(deltaMin, ChronoUnit.MINUTES), prepared, now).toEpochMilli()
    }

    fun useMaxEnd() {
        endsAtMs = maxEnd.toEpochMilli()
    }

    companion object {
        val Saver: Saver<PublishDraft, Any> = listSaver(
            save = {
                listOf(
                    it.title, it.category, ArrayList(it.allergens), it.containsExcluded, it.photoKey,
                    it.portions, it.basePrice, it.minPrice, it.preparedAtMs, it.endsAtMs,
                    it.pointId, it.alias, it.address, it.pinLat, it.pinLon,
                )
            },
            restore = {
                PublishDraft(
                    title = it[0] as String,
                    category = it[1] as String,
                    allergens = (it[2] as List<*>).filterIsInstance<String>(),
                    containsExcluded = it[3] as Boolean,
                    photoKey = it[4] as String?,
                    portions = it[5] as Int,
                    basePrice = it[6] as Int,
                    minPrice = it[7] as Int,
                    preparedAtMs = it[8] as Long,
                    endsAtMs = it[9] as Long,
                    pointId = it[10] as Long?,
                    alias = it[11] as String,
                    address = it[12] as String,
                    pinLat = it[13] as Double?,
                    pinLon = it[14] as Double?,
                )
            },
        )
    }
}

@Composable
fun PublishScreen(
    repository: Repository,
    onPublished: () -> Unit,
    onExit: () -> Unit = {}, // cerrar sin publicar (la X del paso 1)
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val now = remember { Instant.now().truncatedTo(ChronoUnit.MINUTES) }
    val draft = rememberSaveable(saver = PublishDraft.Saver) {
        PublishDraft(
            preparedAtMs = now.minus(30, ChronoUnit.MINUTES).toEpochMilli(),
            endsAtMs = now.plus(120, ChronoUnit.MINUTES).toEpochMilli(),
        )
    }
    var step by rememberSaveable { mutableIntStateOf(1) }
    var pickingOnMap by rememberSaveable { mutableStateOf(false) }
    var pickupPoints by remember { mutableStateOf<List<PickupPointView>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var uploading by remember { mutableStateOf(false) }
    var locating by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        repository.call { myPickupPoints() }.onSuccess { points ->
            pickupPoints = points
            if (draft.pointId == null) draft.pointId = points.firstOrNull()?.id
        }
    }

    // T3: selector de foto -> compresión a ≤ 0,3 MB -> subida (la visualización la sirve Nginx).
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                uploading = true
                error = null
                val bytes = withContext(Dispatchers.IO) { Photos.compress(context, uri) }
                repository.uploadPhoto(bytes)
                    .onSuccess { draft.photoKey = it }
                    .onFailure { error = it.message }
                uploading = false
            }
        }
    }

    // T2: ubicación del vendedor para su punto de retiro (el servidor la reduce a la celda de 400 m).
    val useGps = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        draft.setPin(Locator.lastKnownExact(context))
    }

    // T4: dirección -> Nominatim (≤ 1 consulta/s, con caché). Si no hay resultado, se envía solo la dirección.
    fun locateAddress() {
        scope.launch {
            locating = true
            error = null
            val found = AddressGeocoder.locate(draft.address)
            if (found != null) draft.setPin(found) else error = "No encontramos esa dirección. Pruébala con calle y número o elige el punto en el mapa."
            locating = false
        }
    }

    // Botón "atrás" del sistema: cierra el mapa o retrocede de paso en vez de salir de la pantalla
    BackHandler(enabled = pickingOnMap || step > 1) {
        if (pickingOnMap) pickingOnMap = false else step -= 1
        error = null
    }

    val scroll = rememberScrollState()
    LaunchedEffect(step) { scroll.scrollTo(0) }

    fun submit() {
        if (draft.endsAt.isBefore(Instant.now().plus(PublishRules.MIN_SALE_MIN, ChronoUnit.MINUTES))) {
            error = "La hora de término quedó muy cerca. Ajústala para vender al menos ${PublishRules.MIN_SALE_MIN} min."
            return
        }
        busy = true
        error = null
        scope.launch {
            val id = draft.pointId ?: repository.call {
                val pin = draft.pin
                createPickupPoint(NewPickupPoint(draft.alias, draft.address.ifBlank { draft.alias }, null, pin?.lat, pin?.lon))
            }.onFailure { error = it.message }.getOrNull()?.id
            if (id != null) {
                val newOffer = NewOffer(
                    pickupPointId = id,
                    title = draft.title.trim(),
                    description = null,
                    category = draft.category,
                    photoKey = draft.photoKey,
                    basePrice = draft.basePrice,
                    minPrice = draft.minPrice,
                    portions = draft.portions,
                    allergens = draft.allergens.toList(),
                    excludedIngredients = emptyList(),
                    preparedAt = draft.prepared.toString(),
                    expiresAt = draft.endsAt.toString(),
                )
                repository.call { publish(newOffer) }.onSuccess { onPublished() }.onFailure { error = it.message }
            }
            busy = false
        }
    }

    if (pickingOnMap) {
        PointPicker(
            initial = draft.pin ?: Locator.CONCEPCION,
            onConfirm = {
                draft.setPin(it)
                pickingOnMap = false
            },
            onCancel = { pickingOnMap = false },
        )
        return
    }

    Column(Modifier.fillMaxSize()) {
        WizardHeader(
            step = step,
            title = when (step) {
                1 -> "El plato"
                2 -> "Porciones y precio"
                else -> "Horario y retiro"
            },
            onBack = {
                if (step > 1) {
                    step -= 1
                    error = null
                } else {
                    onExit()
                }
            },
        )

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(scroll)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when (step) {
                1 -> DishStep(
                    draft = draft,
                    uploading = uploading,
                    onPickPhoto = {
                        pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                )
                2 -> PriceStep(draft)
                else -> ScheduleStep(
                    draft = draft,
                    now = now,
                    pickupPoints = pickupPoints,
                    locating = locating,
                    onLocateAddress = ::locateAddress,
                    onUseGps = {
                        if (Locator.hasPermission(context)) {
                            draft.setPin(Locator.lastKnownExact(context))
                        } else {
                            useGps.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                        }
                    },
                    onPickOnMap = { pickingOnMap = true },
                )
            }
        }

        // Barra inferior: siempre un solo botón principal
        Surface(tonalElevation = 3.dp) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ErrorMessage(error)
                when (step) {
                    1 -> PrimaryButton(
                        text = "Siguiente: porciones y precio",
                        enabled = draft.step1Valid && !uploading,
                        onClick = { step = 2 },
                    )
                    2 -> PrimaryButton(
                        text = "Siguiente: horario",
                        onClick = { step = 3 },
                    )
                    else -> {
                        val price = if (draft.minPrice < draft.basePrice) {
                            "${Format.pesos(draft.basePrice)} (desde ${Format.pesos(draft.minPrice)})"
                        } else {
                            "${Format.pesos(draft.basePrice)} precio fijo"
                        }
                        Text(
                            "${draft.portions} porciones · $price · hasta ${draft.endsAt.clock()}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        PrimaryButton(
                            text = "Publicar ahora",
                            enabled = !busy && !locating && draft.hasPickupPoint,
                            onClick = { submit() },
                        )
                    }
                }
            }
        }
    }
}

/** T4: elegir el punto de retiro tocando el mapa (OpenFreeMap). Solo se usa para obtener la celda; nunca se muestra. */
@Composable
private fun PointPicker(initial: Coordinate, onConfirm: (Coordinate) -> Unit, onCancel: () -> Unit) {
    var pin by remember { mutableStateOf(initial) }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onCancel) { Icon(Icons.Default.Close, contentDescription = "Cerrar el mapa") }
            Text("Elige el punto de retiro", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        OffersMap(
            center = pin,
            zoom = 15.0,
            offers = emptyList(),
            onOfferClick = {},
            modifier = Modifier.weight(1f).fillMaxWidth(),
            onMapClick = { pin = it },
        )
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "Toca el mapa para mover el punto. Los vecinos solo verán una zona de ${Grid.CELL_METERS.toInt()} m, nunca el punto exacto.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PrimaryButton(text = "Usar este punto", onClick = { onConfirm(pin) })
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Encabezado con progreso
// ---------------------------------------------------------------------------------------------

@Composable
private fun WizardHeader(step: Int, title: String, onBack: () -> Unit) {
    Column(
        Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconButton(onClick = onBack) {
                if (step == 1) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar sin publicar")
                } else {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver al paso ${step - 1}")
                }
            }
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f),
            )
            Text(
                "$step de $TOTAL_STEPS",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(TOTAL_STEPS) { i ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (i < step) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant,
                        ),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Paso 1: el plato
// ---------------------------------------------------------------------------------------------

@Composable
private fun DishStep(draft: PublishDraft, uploading: Boolean, onPickPhoto: () -> Unit) {
    var showRules by remember { mutableStateOf(false) }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier
                .size(104.dp)
                .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                when {
                    uploading -> "Subiendo…"
                    draft.photoKey == null -> "1 foto"
                    else -> "Foto lista ✓"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onPickPhoto, enabled = !uploading, modifier = Modifier.height(48.dp)) {
                Text(if (draft.photoKey == null) "Elegir de galería" else "Cambiar foto")
            }
        }
    }

    OutlinedTextField(
        value = draft.title,
        onValueChange = { draft.title = it.take(80) },
        label = { Text("Nombre del plato") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )

    Text("Categoría", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Format.CATEGORIES.forEach { (key, text) ->
            FilterChip(draft.category == key, { draft.category = key }, label = { Text(text) })
        }
    }
    TextButton(onClick = { showRules = true }) { Text("¿Qué no se puede publicar?") }

    Text("Alérgenos que contiene · obligatorio", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilterChip(
            selected = NONE_KNOWN in draft.allergens,
            onClick = {
                draft.allergens = if (NONE_KNOWN in draft.allergens) emptyList() else listOf(NONE_KNOWN)
            },
            label = { Text("No contiene ninguno de estos") },
        )
        Format.ALLERGENS.forEach { (key, text) ->
            FilterChip(
                selected = key in draft.allergens,
                onClick = {
                    val without = draft.allergens - NONE_KNOWN
                    draft.allergens = if (key in without) without - key else without + key
                },
                label = { Text(text) },
            )
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Switch(draft.containsExcluded, { draft.containsExcluded = it })
        Text(
            "Contiene alcohol, mariscos crudos, huevo crudo, lácteos sin pasteurizar, conservas caseras o es para lactantes",
            Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    if (draft.containsExcluded) ErrorMessage("Estas preparaciones no se pueden publicar (RB-7).")

    if (showRules) {
        AlertDialog(
            onDismissRequest = { showRules = false },
            confirmButton = { TextButton(onClick = { showRules = false }) { Text("Entendido") } },
            title = { Text("¿Qué no se puede publicar?") },
            text = {
                Text(
                    "No se permiten preparaciones con alcohol, mariscos crudos, huevo crudo, " +
                        "lácteos sin pasteurizar, conservas caseras ni alimentos para lactantes (RB-7).",
                )
            },
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Paso 2: porciones y precio
// ---------------------------------------------------------------------------------------------

@Composable
private fun PriceStep(draft: PublishDraft) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StepperCard(
            label = "Porciones",
            value = draft.portions.toString(),
            minusText = "−", minusDesc = "Una porción menos",
            onMinus = { draft.portions = (draft.portions - 1).coerceAtLeast(1) },
            plusText = "+", plusDesc = "Una porción más",
            onPlus = { draft.portions = (draft.portions + 1).coerceAtMost(PublishRules.MAX_PORTIONS) },
            modifier = Modifier.weight(1f),
        )
        StepperCard(
            label = "Precio por porción",
            value = Format.pesos(draft.basePrice),
            minusText = "−100", minusDesc = "Bajar 100 pesos",
            onMinus = { draft.changeBase(-100) },
            plusText = "+100", plusDesc = "Subir 100 pesos",
            onPlus = { draft.changeBase(100) },
            modifier = Modifier.weight(1f),
        )
    }

    // % mínimo de regateo
    val lo = draft.minAllowed
    val hi = draft.basePrice
    val pct = (draft.minPrice * 100f / hi).roundToInt()
    SectionCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Aceptas propuestas desde", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text("$pct % del precio", style = MaterialTheme.typography.bodySmall)
        }
        Text(
            Format.pesos(draft.minPrice),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        Slider(
            value = draft.minPrice.toFloat(),
            onValueChange = { draft.minPrice = ((it / 100f).roundToInt() * 100).coerceIn(lo, hi) },
            valueRange = lo.toFloat()..hi.toFloat(),
            steps = ((hi - lo) / 100 - 1).coerceAtLeast(0),
            modifier = Modifier.semantics { contentDescription = "Precio mínimo por porción" },
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${Format.pesos(lo)} · 50 %", style = MaterialTheme.typography.bodySmall)
            Text("${Format.pesos(hi)} · sin regateo", style = MaterialTheme.typography.bodySmall)
        }
        Text(
            "Los vecinos podrán proponer entre ${Format.pesos(draft.minPrice)} y ${Format.pesos(hi)}. " +
                "Tú decides si aceptas, rechazas o contraofertas.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    // Vista previa
    Text(
        "ASÍ LO VERÁN TUS VECINOS",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    SectionCard(Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    draft.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Text(
                    "${draft.portions} porc. · zona de 400 m",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
                    Text(Format.pesos(draft.basePrice), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        if (draft.minPrice < draft.basePrice) "regateo desde ${Format.pesos(draft.minPrice)}" else "precio fijo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Paso 3: horario y retiro
// ---------------------------------------------------------------------------------------------

@Composable
private fun ScheduleStep(
    draft: PublishDraft,
    now: Instant,
    pickupPoints: List<PickupPointView>,
    locating: Boolean,
    onLocateAddress: () -> Unit,
    onUseGps: () -> Unit,
    onPickOnMap: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StepperCard(
            label = "Lo preparé a las",
            value = draft.prepared.clock(),
            minusText = "−10", minusDesc = "10 minutos antes",
            onMinus = { draft.movePrepared(-PublishRules.TIME_STEP_MIN, now) },
            plusText = "+10", plusDesc = "10 minutos después",
            onPlus = { draft.movePrepared(PublishRules.TIME_STEP_MIN, now) },
            modifier = Modifier.weight(1f),
        )
        StepperCard(
            label = "A la venta hasta",
            value = draft.endsAt.clock(),
            minusText = "−10", minusDesc = "10 minutos antes",
            onMinus = { draft.moveEnd(-PublishRules.TIME_STEP_MIN, now) },
            plusText = "+10", plusDesc = "10 minutos después",
            onPlus = { draft.moveEnd(PublishRules.TIME_STEP_MIN, now) },
            modifier = Modifier.weight(1f),
            highlight = true,
        )
    }

    OutlinedButton(
        onClick = { draft.useMaxEnd() },
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
    ) {
        Text("Dejarlo el máximo permitido · hasta ${draft.maxEnd.clock()}")
    }

    // Línea de tiempo de inocuidad
    val pickupEnd = PublishRules.pickupEnd(draft.endsAt)
    SectionCard(Modifier.fillMaxWidth()) {
        Text("Tu plato en sus primeras 4 horas", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        SafetyTimeline(prepared = draft.prepared, now = now, end = draft.endsAt, pickupEnd = pickupEnd, max = draft.maxEnd)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            LegendDot(MaterialTheme.colorScheme.tertiary, "A la venta · ${Format.duration(Duration.between(now, draft.endsAt).toMinutes())}")
            LegendDot(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f), "Retiro hasta ${pickupEnd.clock()}")
            LegendDot(MaterialTheme.colorScheme.onSurface, "Límite · ${draft.maxEnd.clock()}")
        }
        Text(
            "Mínimo ${PublishRules.MIN_SALE_MIN} min a la venta y máximo 3 h 30 min desde que lo preparaste, " +
                "para que el último retiro ocurra antes de 4 horas.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    // Punto de retiro
    SectionCard(Modifier.fillMaxWidth()) {
        Text("Punto de retiro", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        if (pickupPoints.isEmpty()) {
            OutlinedTextField(
                draft.alias, { draft.alias = it.take(40) },
                label = { Text("Nombre del punto") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                draft.address, { draft.address = it.take(200) },
                label = { Text("Dirección (solo la verá quien cierre el trato)") },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onLocateAddress, enabled = draft.address.isNotBlank() && !locating) {
                    Text(if (locating) "Buscando…" else "Ubicar dirección")
                }
                OutlinedButton(onClick = onUseGps) { Text("Usar mi ubicación") }
                OutlinedButton(onClick = onPickOnMap) { Text("Elegir en el mapa") }
            }
            if (draft.pin != null) {
                Text(
                    "Punto fijado ✓ Los vecinos verán solo una zona de ${Grid.CELL_METERS.toInt()} m.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                pickupPoints.forEach { p ->
                    FilterChip(draft.pointId == p.id, { draft.pointId = p.id }, label = { Text(p.alias) })
                }
            }
        }
        Text(
            "Los vecinos verán una zona de 400 m. La dirección y tus indicaciones se muestran solo a quien cierre el trato.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Barra de 4 h desde la preparación: ya transcurrido, a la venta, ventana de retiro y marca del límite. */
@Composable
private fun SafetyTimeline(prepared: Instant, now: Instant, end: Instant, pickupEnd: Instant, max: Instant) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val elapsed = MaterialTheme.colorScheme.outline
    val sale = MaterialTheme.colorScheme.tertiary
    val pickup = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)
    val marker = MaterialTheme.colorScheme.onSurface

    fun frac(t: Instant): Float = (Duration.between(prepared, t).toMinutes() / 240f).coerceIn(0f, 1f)

    Canvas(
        Modifier
            .fillMaxWidth()
            .height(28.dp),
    ) {
        val w = size.width
        val barTop = 7.dp.toPx()
        val barH = 14.dp.toPx()
        drawRoundRect(track, Offset(0f, barTop), Size(w, barH), CornerRadius(4.dp.toPx()))
        drawRect(elapsed, Offset(0f, barTop), Size(w * frac(now), barH))
        drawRect(sale, Offset(w * frac(now), barTop), Size(w * (frac(end) - frac(now)), barH))
        drawRect(pickup, Offset(w * frac(end), barTop), Size(w * (frac(pickupEnd) - frac(end)), barH))
        val x = w * frac(max)
        drawLine(
            color = marker,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Piezas reutilizables
// ---------------------------------------------------------------------------------------------

@Composable
private fun SectionCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
private fun StepperCard(
    label: String,
    value: String,
    minusText: String,
    minusDesc: String,
    onMinus: () -> Unit,
    plusText: String,
    plusDesc: String,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
) {
    val accent = MaterialTheme.colorScheme.tertiary
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            if (highlight) 1.5.dp else 1.dp,
            if (highlight) accent else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (highlight) accent else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = onMinus,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .semantics { contentDescription = minusDesc },
                ) { Text(minusText) }
                OutlinedButton(
                    onClick = onPlus,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .semantics { contentDescription = plusDesc },
                ) { Text(plusText) }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color),
        )
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

// ---------------------------------------------------------------------------------------------
// Formato
// ---------------------------------------------------------------------------------------------

private fun Instant.clock(): String = Format.clockTime(toString())
