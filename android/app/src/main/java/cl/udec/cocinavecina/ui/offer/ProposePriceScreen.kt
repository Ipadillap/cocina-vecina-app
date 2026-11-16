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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.udec.cocinavecina.data.network.OfferView
import cl.udec.cocinavecina.ui.common.Format
import cl.udec.cocinavecina.ui.common.NegotiationRules
import cl.udec.cocinavecina.ui.components.PortionStepper
import cl.udec.cocinavecina.ui.components.PriceInput
import cl.udec.cocinavecina.ui.components.PriceMark
import cl.udec.cocinavecina.ui.components.PriceScale
import cl.udec.cocinavecina.ui.theme.CocinaVecinaTheme
import java.time.Instant
import java.time.temporal.ChronoUnit

private val ScreenPadding = 16.dp

@Composable
fun ProposePriceScreen(
    state: ProposePriceUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onPriceTextChange: (String) -> Unit,
    onIncreasePrice: () -> Unit,
    onDecreasePrice: () -> Unit,
    onShortcutSelect: (Int) -> Unit,
    onPortionsChange: (Int) -> Unit,
    onSubmitClick: () -> Unit,
    onBuyAtPublishedPriceClick: () -> Unit,
) {
    val offer = state.offer
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        IconButton(onClick = onBack, modifier = Modifier.padding(start = 4.dp, top = 4.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = MaterialTheme.colorScheme.onBackground)
        }
        when {
            offer != null -> {
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(start = ScreenPadding, end = ScreenPadding, bottom = ScreenPadding),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Header(state, offer)
                    PriceCard(state, offer, onPriceTextChange, onIncreasePrice, onDecreasePrice)
                    Shortcuts(state, offer, onShortcutSelect)
                    PortionsCard(state, onPortionsChange)
                    RulesNote(offer)
                }
                SubmitBar(state, onSubmitClick, onBuyAtPublishedPriceClick)
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
                    Text(state.loadError ?: ProposePriceViewModel.LOAD_ERROR, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    TextButton(onClick = onRetry) { Text("Reintentar", fontWeight = FontWeight.SemiBold) }
                }
        }
    }
}

@Composable
private fun Header(
    state: ProposePriceUiState,
    offer: OfferView,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "Proponer precio",
                modifier = Modifier.weight(1f),
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                "Ronda ${state.round} de ${NegotiationRules.MAX_ROUNDS}",
                modifier =
                    Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Text(
            "${offer.title} · ${offer.seller}",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PriceCard(
    state: ProposePriceUiState,
    offer: OfferView,
    onPriceTextChange: (String) -> Unit,
    onIncreasePrice: () -> Unit,
    onDecreasePrice: () -> Unit,
) {
    SectionCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Tu precio por porción", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            PriceInput(
                priceText = state.priceText,
                onPriceTextChange = onPriceTextChange,
                onIncrease = onIncreasePrice,
                onDecrease = onDecreasePrice,
                isError = state.priceError != null,
                enabled = !state.isSubmitting,
            )
            val priceError = state.priceError
            when {
                priceError != null -> Text(priceError, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                state.savingsPerPortion > 0 ->
                    Text(
                        "Ahorras ${Format.pesos(state.savingsPerPortion)} por porción · " +
                            "${Format.pesos(state.savingsPerPortion * state.portions)} en total",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                else -> Text("Es el precio publicado", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            PriceScale(
                minPrice = offer.minPrice,
                maxPrice = offer.basePrice,
                minLabel = "Mínimo de ${offer.seller}",
                maxLabel = "Precio publicado",
                marks =
                    listOfNotNull(
                        state.sellerCounterPrice?.let { PriceMark(it, offer.seller.substringBefore(' '), emphasized = false) },
                        state.price?.let { PriceMark(it, "tú") },
                    ),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Shortcuts(
    state: ProposePriceUiState,
    offer: OfferView,
    onShortcutSelect: (Int) -> Unit,
) {
    if (state.priceShortcuts.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Atajos", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.priceShortcuts.forEach { price ->
                val selected = state.price == price
                FilterChip(
                    selected = selected,
                    onClick = { onShortcutSelect(price) },
                    enabled = !state.isSubmitting,
                    label = {
                        Text(
                            if (price == offer.minPrice) "${Format.pesos(price)} · mínimo" else Format.pesos(price),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    shape = RoundedCornerShape(50),
                    colors =
                        FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = MaterialTheme.colorScheme.onSurface,
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    border =
                        FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = MaterialTheme.colorScheme.outlineVariant,
                            selectedBorderColor = MaterialTheme.colorScheme.primary,
                        ),
                )
            }
        }
    }
}

@Composable
private fun PortionsCard(
    state: ProposePriceUiState,
    onPortionsChange: (Int) -> Unit,
) {
    if (state.isCounterProposal) {
        SectionCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    if (state.portions == 1) "1 porción" else "${state.portions} porciones",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text("Las porciones se mantienen durante el regateo.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }
    SectionCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PortionStepper(
                portions = state.portions,
                maxPortions = state.maxPortions,
                onPortionsChange = onPortionsChange,
                enabled = !state.isSubmitting,
            )
            val available = state.offer?.availablePortions ?: 0
            Text(
                if (available == 1) "Queda 1 porción" else "Quedan $available porciones",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RulesNote(offer: OfferView) {
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Text(
                "${offer.seller} puede aceptar, rechazar o contraofertar. Hay hasta ${NegotiationRules.MAX_ROUNDS} rondas de " +
                    "regateo; después solo queda aceptar o rechazar.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SubmitBar(
    state: ProposePriceUiState,
    onSubmitClick: () -> Unit,
    onBuyAtPublishedPriceClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        shadowElevation = 8.dp,
    ) {
        Column(
            Modifier.padding(horizontal = ScreenPadding, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            state.submitError?.let {
                Text(it, modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp), fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
            }
            Button(
                onClick = onSubmitClick,
                enabled = state.canSubmit,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        disabledContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
            ) {
                if (state.isSubmitting) {
                    Box(Modifier.semantics { contentDescription = "Enviando propuesta" }) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Enviar propuesta", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        if (state.priceError == null) {
                            Text("Total ${Format.pesos(state.total)}", fontSize = 12.sp)
                        }
                    }
                }
            }
            if (!state.isCounterProposal) {
                TextButton(onClick = onBuyAtPublishedPriceClick, enabled = !state.isSubmitting) {
                    Text(
                        "Mejor compro al precio publicado",
                        fontSize = 14.sp,
                        textDecoration = TextDecoration.Underline,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
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

private fun previewOffer(): OfferView {
    val now = Instant.now()
    return OfferView(
        id = 1,
        sellerId = 2,
        seller = "Marta R.",
        title = "Charquicán con huevo frito",
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
    )
}

@Preview(showBackground = true, heightDp = 1000, widthDp = 390)
@Composable
private fun ProposePriceScreenPreview() {
    CocinaVecinaTheme {
        ProposePriceScreen(
            state = ProposePriceUiState(offer = previewOffer(), priceText = "3000", portions = 2, isLoading = false),
            onBack = {},
            onRetry = {},
            onPriceTextChange = {},
            onIncreasePrice = {},
            onDecreasePrice = {},
            onShortcutSelect = {},
            onPortionsChange = {},
            onSubmitClick = {},
            onBuyAtPublishedPriceClick = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 1000, widthDp = 390)
@Composable
private fun ProposePriceScreenErrorPreview() {
    CocinaVecinaTheme {
        ProposePriceScreen(
            state = ProposePriceUiState(offer = previewOffer(), priceText = "2500", portions = 1, isLoading = false),
            onBack = {},
            onRetry = {},
            onPriceTextChange = {},
            onIncreasePrice = {},
            onDecreasePrice = {},
            onShortcutSelect = {},
            onPortionsChange = {},
            onSubmitClick = {},
            onBuyAtPublishedPriceClick = {},
        )
    }
}
