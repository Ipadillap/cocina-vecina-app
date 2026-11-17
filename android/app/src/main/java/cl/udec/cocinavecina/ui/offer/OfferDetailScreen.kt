package cl.udec.cocinavecina.ui.offer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.udec.cocinavecina.data.network.OfferView
import cl.udec.cocinavecina.ui.common.CocinaVecinaMark
import cl.udec.cocinavecina.ui.common.Format
import cl.udec.cocinavecina.ui.components.PortionStepper
import cl.udec.cocinavecina.ui.theme.CocinaVecinaTheme
import cl.udec.cocinavecina.ui.common.RemoteImage
import java.time.Instant
import java.time.temporal.ChronoUnit

private val ScreenPadding = 16.dp
private const val URGENT_MINUTES = 60L

@Composable
fun OfferDetailScreen(
    state: OfferDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onPortionsChange: (Int) -> Unit,
    onProposePriceClick: () -> Unit,
    onBuyClick: () -> Unit,
    onReportClick: () -> Unit,
    onReportDismiss: () -> Unit,
    onReportConfirm: (String) -> Unit,
) {
    val offer = state.offer
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        when {
            offer != null -> OfferContent(state, offer, onBack, onPortionsChange, onProposePriceClick, onBuyClick, onReportClick)
            state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            else -> LoadError(state.loadError, onBack, onRetry)
        }
    }
    if (state.isReportDialogVisible) {
        ReportDialog(state.isReporting, onReportDismiss, onReportConfirm)
    }
}

@Composable
private fun OfferContent(
    state: OfferDetailUiState,
    offer: OfferView,
    onBack: () -> Unit,
    onPortionsChange: (Int) -> Unit,
    onProposePriceClick: () -> Unit,
    onBuyClick: () -> Unit,
    onReportClick: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            OfferPhoto(offer, onBack, onReportClick)
            Column(
                Modifier.padding(ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        offer.title,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    offer.description?.let {
                        Text(it, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                state.reportMessage?.let {
                    Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.tertiary)
                }
                TimeStrip(offer)
                SellerBlock(offer, state.distanceMeters)
                KeyFacts(offer)
                AllergensSection(offer.allergens)
                PickupSection()
            }
        }
        PurchaseBar(state, offer, onPortionsChange, onProposePriceClick, onBuyClick)
    }
}

@Composable
private fun OfferPhoto(
    offer: OfferView,
    onBack: () -> Unit,
    onReportClick: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(260.dp),
    ) {
        if (offer.photoUrl != null) {
            RemoteImage(
                model = offer.photoUrl,
                contentDescription = offer.title,
                modifier = Modifier.fillMaxSize(),
                markSize = 72.dp,
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                CocinaVecinaMark(size = 72.dp)
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(
                onClick = onBack,
                colors =
                    IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
            Surface(
                onClick = onReportClick,
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Filled.Warning, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                    Text("Reportar", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
private fun TimeStrip(offer: OfferView) {
    val minutesLeft = Format.minutesLeft(offer.expiresAt)
    val urgent = minutesLeft < URGENT_MINUTES
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (urgent) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                if (minutesLeft > 0) {
                    "A la venta hasta ${Format.clockTime(offer.expiresAt)} · ${Format.timeLeft(offer.expiresAt)}"
                } else {
                    "Ya no está a la venta"
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (urgent) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "Preparado a las ${Format.clockTime(offer.preparedAt)}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SellerBlock(
    offer: OfferView,
    distanceMeters: Long?,
) {
    SectionCard {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    offer.seller.trim().take(1).uppercase(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(offer.seller, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                val details =
                    listOfNotNull(
                        if (offer.sellerVerified == true) "Cuenta verificada" else null,
                        distanceMeters?.let { "≈ ${Format.distance(it)} de ti" },
                    )
                if (details.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (offer.sellerVerified == true) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.tertiary)
                        }
                        Text(details.joinToString(" · "), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyFacts(offer: OfferView) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FactTile(
            label = "Quedan",
            value = offer.totalPortions?.let { "${offer.availablePortions} de $it" } ?: "${offer.availablePortions}",
            caption = if (offer.availablePortions == 1) "porción" else "porciones",
            modifier = Modifier.weight(1f),
        )
        FactTile(
            label = "Precio",
            value = Format.pesos(offer.basePrice),
            caption = "por porción",
            modifier = Modifier.weight(1f),
            highlighted = true,
        )
        FactTile(
            label = "Regateo",
            value = Format.pesos(offer.minPrice),
            caption = "mínimo",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun FactTile(
    label: String,
    value: String,
    caption: String,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = if (highlighted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
            Text(caption, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AllergensSection(allergens: List<String>) {
    SectionCard {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle(Icons.Filled.Info, "Alérgenos declarados")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                allergens.ifEmpty { listOf("NONE_KNOWN") }.forEach { allergen ->
                    Text(
                        Format.allergen(allergen),
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Text(
                "Los declara quien cocina. Es comida casera sin certificación sanitaria: si tienes una alergia grave, consulta antes de comprar.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PickupSection() {
    SectionCard {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionTitle(Icons.Filled.Lock, "Retiro a pie, pago en persona")
            Text(
                "La dirección y el teléfono aparecen solo cuando ambos aceptan el trato. Pagas en persona al retirar.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        content = content,
    )
}

@Composable
private fun SectionTitle(
    icon: ImageVector,
    title: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun PurchaseBar(
    state: OfferDetailUiState,
    offer: OfferView,
    onPortionsChange: (Int) -> Unit,
    onProposePriceClick: () -> Unit,
    onBuyClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        shadowElevation = 8.dp,
    ) {
        Column(
            Modifier.padding(horizontal = ScreenPadding, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (state.isOnSale) {
                PortionStepper(
                    portions = state.portions,
                    maxPortions = state.maxPortions,
                    onPortionsChange = onPortionsChange,
                    enabled = !state.isBuying,
                )
            } else {
                Text(
                    "Esta oferta ya no está disponible.",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            state.buyError?.let {
                Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onProposePriceClick,
                    enabled = state.isOnSale && !state.isBuying,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                ) {
                    Text("Proponer precio", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onBuyClick,
                    enabled = state.isOnSale && !state.isBuying,
                    modifier = Modifier.weight(1.3f).height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            disabledContentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                ) {
                    if (state.isBuying) {
                        Box(Modifier.semantics { contentDescription = "Enviando solicitud" }) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Comprar a ${Format.pesos(offer.basePrice)}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text("Total ${Format.pesos(state.total)}", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadError(
    message: String?,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(ScreenPadding)) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
        }
        Column(
            Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(message ?: OfferDetailViewModel.LOAD_ERROR, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
            TextButton(onClick = onRetry) { Text("Reintentar", fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun ReportDialog(
    isReporting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var reason by rememberSaveable { mutableStateOf(Format.REPORT_REASONS.first().first) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿Por qué la reportas?", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.selectableGroup()) {
                Format.REPORT_REASONS.forEach { (key, label) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(selected = reason == key, role = Role.RadioButton, onClick = { reason = key })
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = reason == key, onClick = null)
                        Text(label, modifier = Modifier.padding(start = 8.dp), fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(reason) }, enabled = !isReporting) {
                Text("Reportar", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

private fun previewOffer(): OfferView {
    val now = Instant.now()
    return OfferView(
        id = 1,
        sellerId = 2,
        seller = "Marta R.",
        title = "Charquicán con huevo frito",
        description = "Con zapallo, choclo y carne molida.",
        category = "MAIN_COURSE",
        basePrice = 3500,
        minPrice = 2800,
        availablePortions = 3,
        allergens = listOf("EGG"),
        preparedAt = now.minus(90, ChronoUnit.MINUTES).toString(),
        expiresAt = now.plus(52, ChronoUnit.MINUTES).toString(),
        status = "ACTIVE",
        approxLatitude = -36.82,
        approxLongitude = -73.05,
        totalPortions = 4,
        sellerVerified = true,
    )
}

@Preview(showBackground = true, heightDp = 1100, widthDp = 390)
@Composable
private fun OfferDetailScreenPreview() {
    CocinaVecinaTheme {
        OfferDetailScreen(
            state = OfferDetailUiState(offer = previewOffer(), distanceMeters = 400, portions = 2, isLoading = false),
            onBack = {},
            onRetry = {},
            onPortionsChange = {},
            onProposePriceClick = {},
            onBuyClick = {},
            onReportClick = {},
            onReportDismiss = {},
            onReportConfirm = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 844, widthDp = 390)
@Composable
private fun OfferDetailScreenErrorPreview() {
    CocinaVecinaTheme {
        OfferDetailScreen(
            state = OfferDetailUiState(isLoading = false, loadError = OfferDetailViewModel.NO_CONNECTION),
            onBack = {},
            onRetry = {},
            onPortionsChange = {},
            onProposePriceClick = {},
            onBuyClick = {},
            onReportClick = {},
            onReportDismiss = {},
            onReportConfirm = {},
        )
    }
}
