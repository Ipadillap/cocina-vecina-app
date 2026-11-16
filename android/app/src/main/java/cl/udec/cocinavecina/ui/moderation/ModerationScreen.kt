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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.udec.cocinavecina.data.Repository
import cl.udec.cocinavecina.data.network.Decision
import cl.udec.cocinavecina.data.network.OfferUnderReview
import cl.udec.cocinavecina.ui.common.EmptyState
import cl.udec.cocinavecina.ui.common.ErrorMessage
import cl.udec.cocinavecina.ui.common.Format
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val Muted = Color(0xFF3F4D47)
private val ChipBg = Color(0xFFF6E3D9)
private val ChipFg = Color(0xFF7E2A0D)
private val Accent = Color(0xFF9A3411)
private val OkFg = Color(0xFF1E4034)

private val clock = DateTimeFormatter.ofPattern("HH:mm")

/** Una decisión tomada en esta sesión (la bitácora del servidor aún no tiene endpoint en el contrato). */
private data class LoggedDecision(val action: String, val reason: String, val at: String)

@Composable
fun ModerationScreen(repository: Repository) {
    var tab by remember { mutableIntStateOf(0) }
    var queue by remember { mutableStateOf<List<OfferUnderReview>?>(null) }
    var log by remember { mutableStateOf<List<LoggedDecision>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var version by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(version) {
        repository.call { moderationQueue() }.onSuccess { queue = it }.onFailure { error = it.message }
    }

    fun decide(
        label: String,
        reason: String,
        request: suspend () -> Result<Any?>,
    ) {
        scope.launch {
            request()
                .onSuccess {
                    error = null
                    log = listOf(LoggedDecision(label, reason, LocalTime.now().format(clock))) + log
                }
                .onFailure { error = it.message }
            version++
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Moderación", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            Text(
                "Moderador",
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFF13241E)).padding(horizontal = 10.dp, vertical = 5.dp),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium,
            )
        }
        TabRow(selectedTabIndex = tab) {
            listOf("Ocultas por reportes", "Bitácora").forEachIndexed { index, title ->
                Tab(selected = tab == index, onClick = { tab = index }, text = { Text(title, fontWeight = FontWeight.Bold, maxLines = 1) })
            }
        }
        ErrorMessage(error)
        if (tab == 0) {
            HiddenOffers(
                queue = queue,
                onReactivate = { offer, reason ->
                    decide("Reactivar «${offer.title}»", reason) { repository.call { decideOffer(offer.offerId, Decision("REACTIVATE", reason)) } }
                },
                onWithdraw = { offer, reason ->
                    decide("Retirar «${offer.title}»", reason) { repository.call { decideOffer(offer.offerId, Decision("WITHDRAW", reason)) } }
                },
                onSuspendSeller = { offer, reason ->
                    decide("Suspender cuenta #${offer.sellerId}", reason) { repository.call { decideAccount(offer.sellerId, Decision("SUSPEND", reason)) } }
                },
            )
        } else {
            DecisionLog(log)
        }
    }
}

@Composable
private fun HiddenOffers(
    queue: List<OfferUnderReview>?,
    onReactivate: (OfferUnderReview, String) -> Unit,
    onWithdraw: (OfferUnderReview, String) -> Unit,
    onSuspendSeller: (OfferUnderReview, String) -> Unit,
) {
    val offers = queue ?: return
    if (offers.isEmpty()) return EmptyState("No hay ofertas en revisión.")
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text(
                "Se ocultan solas al tercer reporte de personas distintas y esperan tu revisión.",
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
            )
        }
        items(offers, key = { it.offerId }) { offer ->
            var reason by remember(offer.offerId) { mutableStateOf("") }
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(offer.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Vendedor #${offer.sellerId} · ${offer.sellerReportCount} reportes en total", style = MaterialTheme.typography.bodySmall, color = Muted)
                    Text("${offer.reports.size} reportes · oculta", color = Accent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    offer.reports.forEach { report ->
                        Text(
                            Format.reportReason(report.reason) + (report.detail?.let { ": $it" } ?: ""),
                            modifier = Modifier.clip(RoundedCornerShape(9.dp)).background(ChipBg).padding(horizontal = 9.dp, vertical = 4.dp),
                            color = ChipFg,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it.take(200) },
                        label = { Text("Motivo de la decisión") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onReactivate(offer, reason) },
                            enabled = reason.isNotBlank(),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = OkFg),
                        ) { Text("Reactivar") }
                        OutlinedButton(onClick = { onWithdraw(offer, reason) }, enabled = reason.isNotBlank(), modifier = Modifier.weight(1f)) {
                            Text("Mantener oculta")
                        }
                    }
                    OutlinedButton(onClick = { onSuspendSeller(offer, reason) }, enabled = reason.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                        Text("Suspender la cuenta del vendedor")
                    }
                }
            }
        }
    }
}

@Composable
private fun DecisionLog(entries: List<LoggedDecision>) {
    if (entries.isEmpty()) return EmptyState("Aún no tomas decisiones en esta sesión.")
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("Cada decisión queda auditada en el servidor con tu usuario y la hora.", style = MaterialTheme.typography.bodySmall, color = Muted) }
        items(entries) { entry ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(entry.action, fontWeight = FontWeight.Bold)
                    Text(entry.at, style = MaterialTheme.typography.bodySmall, color = Muted)
                    Text("Motivo: ${entry.reason}")
                }
            }
        }
    }
}
