package cl.udec.cocinavecina.ui.moderation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.udec.cocinavecina.data.Repository

private val Muted = Color(0xFF3F4D47)
private val ChipBg = Color(0xFFF6E3D9)
private val ChipFg = Color(0xFF7E2A0D)
private val Accent = Color(0xFF9A3411)
private val OkBg = Color(0xFFDCE7DF)
private val OkFg = Color(0xFF1E4034)
private val Ink = Color(0xFF13241E)

private data class MockOffer(
    val title: String,
    val seller: String,
    val reports: List<String>,
)

private data class MockDeletion(
    val id: Long,
    val userId: Long,
    val reason: String,
    val requestedAt: String,
    val dueAt: String,
)

private data class MockLog(
    val id: Long,
    val moderatorId: Long,
    val action: String,
    val reason: String,
    val occurredAt: String,
)

@Composable
fun ModerationScreen(repository: Repository) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Ofertas Ocultas", "Eliminación de Cuenta", "Bitácora")

    // Datos de ejemplo. No dependen de moderationQueue().
    var offers by remember {
        mutableStateOf(
            listOf(
                MockOffer(
                    title = "Cazuela de vacuno",
                    seller = "Vendedor #184 · 4 reportes en total",
                    reports = listOf("Comida en mal estado", "No coincide la foto"),
                ),
                MockOffer(
                    title = "Empanadas de pino",
                    seller = "Vendedor #902 · 3 reportes en total",
                    reports = listOf("No entregó"),
                ),
            ),
        )
    }
    var decided by remember { mutableStateOf(setOf<String>()) }
    var deletions by remember {
        mutableStateOf(
            listOf(
                MockDeletion(
                    id = 1L,
                    userId = 2290L,
                    reason = "Ya no uso la app",
                    requestedAt = "13-10-2026",
                    dueAt = "vence 12-11-2026",
                ),
            ),
        )
    }
    var logs by remember {
        mutableStateOf(
            listOf(
                MockLog(1L, 7L, "RETIRAR oferta #441", "Tercer reporte por comida en mal estado", "14-11-2026 18:22"),
                MockLog(2L, 7L, "REACTIVAR oferta #390", "Reportes duplicados de la misma persona", "14-11-2026 11:05"),
            ),
        )
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Moderación", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            Text(
                "Moderador · 2FA",
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Ink)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium,
            )
        }

        ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 12.dp) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontWeight = FontWeight.Bold, maxLines = 1) },
                )
            }
        }

        when (selectedTab) {
            0 -> OffersTab(
                offers = offers,
                decided = decided,
                onDecide = { title, action, reason ->
                    decided = decided + title
                    logs = listOf(
                        MockLog(
                            id = (logs.maxOfOrNull { it.id } ?: 0L) + 1,
                            moderatorId = 1L,
                            action = if (action == "REACTIVATE") "REACTIVAR $title" else "RETIRAR $title",
                            reason = reason,
                            occurredAt = "ahora",
                        ),
                    ) + logs
                },
            )
            1 -> DeletionTab(
                requests = deletions,
                onDecide = { id, action, reason ->
                    deletions = deletions.filterNot { it.id == id }
                    logs = listOf(
                        MockLog(
                            id = (logs.maxOfOrNull { it.id } ?: 0L) + 1,
                            moderatorId = 1L,
                            action = if (action == "APPROVE") "APROBAR_BORRADO #$id" else "RECHAZAR_BORRADO #$id",
                            reason = reason,
                            occurredAt = "ahora",
                        ),
                    ) + logs
                },
            )
            else -> LogTab(logs)
        }
    }
}

@Composable
private fun OffersTab(
    offers: List<MockOffer>,
    decided: Set<String>,
    onDecide: (String, String, String) -> Unit,
) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text(
                "Se ocultan solas al tercer reporte de personas distintas y esperan tu revisión.",
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
            )
        }
        items(offers, key = { it.title }) { offer ->
            var reason by remember(offer.title) { mutableStateOf("") }
            val done = offer.title in decided
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(offer.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(offer.seller, style = MaterialTheme.typography.bodySmall, color = Muted)
                    Text("3 reportes · oculta", color = Accent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        offer.reports.forEach { label ->
                            Text(
                                label,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(ChipBg)
                                    .padding(horizontal = 9.dp, vertical = 4.dp),
                                color = ChipFg,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                    if (done) {
                        Text(
                            "Decisión registrada. Quedó en la bitácora.",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(OkBg)
                                .padding(10.dp),
                            color = OkFg,
                            fontWeight = FontWeight.Bold,
                        )
                    } else {
                        OutlinedTextField(
                            value = reason,
                            onValueChange = { reason = it.take(200) },
                            label = { Text("Motivo de la decisión") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onDecide(offer.title, "REACTIVATE", reason) },
                                enabled = reason.isNotBlank(),
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = OkFg),
                            ) { Text("Reactivar") }
                            OutlinedButton(
                                onClick = { onDecide(offer.title, "WITHDRAW", reason) },
                                enabled = reason.isNotBlank(),
                                modifier = Modifier.weight(1f),
                            ) { Text("Retirar") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeletionTab(
    requests: List<MockDeletion>,
    onDecide: (Long, String, String) -> Unit,
) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text(
                "Pedidos de derechos que llegaron por correo. El plazo legal es de 30 días corridos.",
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
            )
        }
        if (requests.isEmpty()) {
            item { Text("No quedan solicitudes.", color = Muted) }
        }
        items(requests, key = { it.id }) { request ->
            var reason by remember(request.id) { mutableStateOf(request.reason) }
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Eliminar cuenta", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(request.dueAt, color = Accent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        "Cuenta #${request.userId} · recibida ${request.requestedAt}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                    )
                    Text("Motivo: ${request.reason}")
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it.take(200) },
                        label = { Text("Motivo de la decisión") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = { onDecide(request.id, "APPROVE", reason) },
                        enabled = reason.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = OkFg),
                    ) { Text("Aprobar borrado") }
                    OutlinedButton(
                        onClick = { onDecide(request.id, "REJECT", reason) },
                        enabled = reason.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Rechazar") }
                }
            }
        }
    }
}

@Composable
private fun LogTab(logs: List<MockLog>) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(logs, key = { it.id }) { entry ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(entry.action, fontWeight = FontWeight.Bold)
                    Text(
                        "Moderador #${entry.moderatorId} · ${entry.occurredAt}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                    )
                    Text("Motivo: ${entry.reason}")
                }
            }
        }
    }
}
