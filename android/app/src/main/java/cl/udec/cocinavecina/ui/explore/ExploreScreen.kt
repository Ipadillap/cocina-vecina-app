package cl.udec.cocinavecina.ui.explore

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.udec.cocinavecina.data.network.NearbyOffer
import cl.udec.cocinavecina.ui.common.CocinaVecinaLogo
import cl.udec.cocinavecina.ui.common.Format
import cl.udec.cocinavecina.ui.theme.CocinaVecinaTheme
import java.time.Instant
import java.time.temporal.ChronoUnit

private val ScreenPadding = 16.dp

@Composable
fun ExploreScreen(
    state: ExploreUiState,
    onPlaceSelect: (ExplorePlace) -> Unit,
    onCurrentLocationSelect: () -> Unit,
    onRadiusSelect: (SearchRadius) -> Unit,
    onOrderSelect: (OfferOrder) -> Unit,
    onCategorySelect: (String?) -> Unit,
    onViewModeChange: (ExploreViewMode) -> Unit,
    onRetry: () -> Unit,
    onOfferClick: (Long) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        ExploreHeader(state, onPlaceSelect, onCurrentLocationSelect, onRadiusSelect)
        when (state.viewMode) {
            ExploreViewMode.LIST ->
                LazyColumn(
                    Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = ScreenPadding),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(key = "map") {
                        MapPreview(state, onOfferClick, onExpand = { onViewModeChange(ExploreViewMode.MAP) })
                    }
                    item(key = "filters") {
                        FiltersSection(state, onCategorySelect, onOrderSelect, onViewModeChange)
                    }
                    item(key = "status") {
                        StatusSection(state, onRetry)
                    }
                    items(state.visibleOffers, key = { it.id }) { offer ->
                        OfferCard(
                            offer = offer,
                            onClick = { onOfferClick(offer.id) },
                            modifier = Modifier.padding(horizontal = ScreenPadding),
                        )
                    }
                }
            ExploreViewMode.MAP ->
                Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = ScreenPadding)
                            .clip(RoundedCornerShape(16.dp)),
                    ) {
                        OffersMap(
                            center = state.place.coordinate,
                            zoom = state.radius.mapZoom,
                            offers = state.visibleOffers,
                            onOfferClick = onOfferClick,
                            modifier = Modifier.fillMaxSize(),
                        )
                        MapAttribution(Modifier.align(Alignment.BottomStart))
                    }
                    PrivacyNote()
                    FiltersSection(state, onCategorySelect, onOrderSelect, onViewModeChange)
                    StatusSection(state, onRetry)
                }
        }
    }
}

@Composable
private fun ExploreHeader(
    state: ExploreUiState,
    onPlaceSelect: (ExplorePlace) -> Unit,
    onCurrentLocationSelect: () -> Unit,
    onRadiusSelect: (SearchRadius) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = ScreenPadding, end = ScreenPadding, top = 12.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CocinaVecinaLogo(markSize = 28.dp)
        Text(
            buildAnnotatedString {
                append("Buscando cerca de ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)) {
                    append(state.place.searchLabel)
                }
            },
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PlaceSelector(state.place, onPlaceSelect, onCurrentLocationSelect, Modifier.weight(1f))
            RadiusSelector(state.radius, onRadiusSelect)
        }
        state.locationNotice?.let {
            Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun PlaceSelector(
    selected: ExplorePlace,
    onPlaceSelect: (ExplorePlace) -> Unit,
    onCurrentLocationSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        SelectorPill(
            text = selected.name,
            leadingIcon = Icons.Filled.LocationOn,
            description = "Cambiar lugar de búsqueda",
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Mi ubicación") },
                leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null) },
                trailingIcon = { if (selected.isCurrentLocation) Icon(Icons.Filled.Check, contentDescription = "Seleccionado") },
                onClick = {
                    expanded = false
                    onCurrentLocationSelect()
                },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ExplorePlace.NEIGHBORHOODS.forEach { place ->
                DropdownMenuItem(
                    text = { Text(place.name) },
                    trailingIcon = { if (place.id == selected.id) Icon(Icons.Filled.Check, contentDescription = "Seleccionado") },
                    onClick = {
                        expanded = false
                        onPlaceSelect(place)
                    },
                )
            }
        }
    }
}

@Composable
private fun RadiusSelector(
    selected: SearchRadius,
    onRadiusSelect: (SearchRadius) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        SelectorPill(
            text = selected.label,
            description = "Cambiar radio de búsqueda",
            onClick = { expanded = true },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SearchRadius.entries.forEach { radius ->
                DropdownMenuItem(
                    text = { Text("Hasta ${radius.label}") },
                    trailingIcon = { if (radius == selected) Icon(Icons.Filled.Check, contentDescription = "Seleccionado") },
                    onClick = {
                        expanded = false
                        onRadiusSelect(radius)
                    },
                )
            }
        }
    }
}

@Composable
private fun SelectorPill(
    text: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.semantics { contentDescription = "$description. Actual: $text" },
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            Modifier
                .heightIn(min = 40.dp)
                .padding(start = 12.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            leadingIcon?.let {
                Icon(it, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Text(
                text,
                modifier = Modifier.weight(1f, fill = false),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MapPreview(
    state: ExploreUiState,
    onOfferClick: (Long) -> Unit,
    onExpand: () -> Unit,
) {
    Column(Modifier.padding(horizontal = ScreenPadding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(16.dp)),
        ) {
            OffersMap(
                center = state.place.coordinate,
                zoom = state.radius.mapZoom,
                offers = state.visibleOffers,
                onOfferClick = onOfferClick,
                modifier = Modifier.fillMaxSize(),
                interactive = false,
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(role = Role.Button, onClickLabel = "Ver mapa completo", onClick = onExpand),
            )
            MapAttribution(Modifier.align(Alignment.BottomStart))
        }
        PrivacyNote(Modifier)
    }
}

@Composable
private fun MapAttribution(modifier: Modifier = Modifier) {
    Text(
        "© OpenStreetMap",
        modifier =
            modifier
                .padding(6.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                .padding(horizontal = 6.dp, vertical = 2.dp),
        fontSize = 10.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun PrivacyNote(modifier: Modifier = Modifier.padding(horizontal = ScreenPadding)) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            "La dirección exacta llega al cerrar el trato",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FiltersSection(
    state: ExploreUiState,
    onCategorySelect: (String?) -> Unit,
    onOrderSelect: (OfferOrder) -> Unit,
    onViewModeChange: (ExploreViewMode) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CategoryChips(state.category, onCategorySelect)
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = ScreenPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                resultsLabel(state.visibleOffers.size),
                modifier = Modifier.weight(1f),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            OrderSelector(state.order, onOrderSelect)
            ViewModeToggle(state.viewMode, onViewModeChange)
        }
        if (state.isLoading) {
            LinearProgressIndicator(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ScreenPadding),
            )
        }
    }
}

@Composable
private fun CategoryChips(
    selected: String?,
    onCategorySelect: (String?) -> Unit,
) {
    val options = listOf<Pair<String?, String>>(null to "Todas") + Format.CATEGORIES
    LazyRow(
        contentPadding = PaddingValues(horizontal = ScreenPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(options, key = { it.first ?: "all" }) { (value, label) ->
            FilterChip(
                selected = selected == value,
                onClick = { onCategorySelect(value) },
                label = { Text(label, fontSize = 13.sp) },
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
                        selected = selected == value,
                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                        selectedBorderColor = MaterialTheme.colorScheme.primary,
                    ),
            )
        }
    }
}

@Composable
private fun OrderSelector(
    selected: OfferOrder,
    onOrderSelect: (OfferOrder) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        SelectorPill(
            text = selected.label,
            description = "Cambiar orden",
            onClick = { expanded = true },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            OfferOrder.entries.forEach { order ->
                DropdownMenuItem(
                    text = { Text(order.label) },
                    trailingIcon = { if (order == selected) Icon(Icons.Filled.Check, contentDescription = "Seleccionado") },
                    onClick = {
                        expanded = false
                        onOrderSelect(order)
                    },
                )
            }
        }
    }
}

@Composable
private fun ViewModeToggle(
    selected: ExploreViewMode,
    onViewModeChange: (ExploreViewMode) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(Modifier.padding(3.dp).selectableGroup()) {
            ViewModeOption(Icons.AutoMirrored.Filled.List, "Ver lista", selected == ExploreViewMode.LIST) {
                onViewModeChange(ExploreViewMode.LIST)
            }
            ViewModeOption(Icons.Filled.Place, "Ver mapa", selected == ExploreViewMode.MAP) {
                onViewModeChange(ExploreViewMode.MAP)
            }
        }
    }
}

@Composable
private fun ViewModeOption(
    icon: ImageVector,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(width = 44.dp, height = 36.dp)
            .clip(RoundedCornerShape(50))
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatusSection(
    state: ExploreUiState,
    onRetry: () -> Unit,
) {
    val error = state.error
    when {
        error != null ->
            Column(Modifier.fillMaxWidth().padding(horizontal = ScreenPadding)) {
                Text(error, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onRetry) { Text("Reintentar", fontWeight = FontWeight.SemiBold) }
            }
        !state.isLoading && state.visibleOffers.isEmpty() ->
            Text(
                if (state.offers.isEmpty()) {
                    "No hay ofertas activas en este radio. Prueba con un radio mayor u otro lugar."
                } else {
                    "No hay ofertas de esta categoría en este radio."
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenPadding, vertical = 24.dp),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
    }
}

private fun resultsLabel(count: Int): String = if (count == 1) "1 resultado" else "$count resultados"

private fun previewOffers(): List<NearbyOffer> {
    val now = Instant.now()
    return listOf(
        NearbyOffer(1, "Pastel de choclo", "MAIN_COURSE", null, 4500, 3800, 4, now.plus(150, ChronoUnit.MINUTES).toString(), -36.8268, -73.0497, 650, "Ana"),
        NearbyOffer(2, "Empanadas de pino", "EMPANADAS", null, 3500, 3000, 6, now.plus(40, ChronoUnit.MINUTES).toString(), -36.8268, -73.0497, 980, "Rosa"),
        NearbyOffer(3, "Crema de zapallo", "SOUPS_AND_CREAMS", null, 2500, 2000, 1, now.plus(90, ChronoUnit.MINUTES).toString(), -36.8304, -73.0389, 240, "Rosa"),
    )
}

@Preview(showBackground = true, heightDp = 844, widthDp = 390)
@Composable
private fun ExploreScreenListPreview() {
    CocinaVecinaTheme {
        ExploreScreen(ExploreUiState(offers = previewOffers()), {}, {}, {}, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, heightDp = 844, widthDp = 390)
@Composable
private fun ExploreScreenMapPreview() {
    CocinaVecinaTheme {
        ExploreScreen(
            state = ExploreUiState(offers = previewOffers(), viewMode = ExploreViewMode.MAP, category = "EMPANADAS"),
            onPlaceSelect = {},
            onCurrentLocationSelect = {},
            onRadiusSelect = {},
            onOrderSelect = {},
            onCategorySelect = {},
            onViewModeChange = {},
            onRetry = {},
            onOfferClick = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 844, widthDp = 390)
@Composable
private fun ExploreScreenErrorPreview() {
    CocinaVecinaTheme {
        ExploreScreen(
            state = ExploreUiState(error = ExploreViewModel.NO_CONNECTION, locationNotice = ExploreViewModel.LOCATION_DENIED),
            onPlaceSelect = {},
            onCurrentLocationSelect = {},
            onRadiusSelect = {},
            onOrderSelect = {},
            onCategorySelect = {},
            onViewModeChange = {},
            onRetry = {},
            onOfferClick = {},
        )
    }
}
