package cl.udec.cocinavecina.ui.moderation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import cl.udec.cocinavecina.data.Repository
import cl.udec.cocinavecina.data.network.Decision
import cl.udec.cocinavecina.data.network.OfferUnderReview
import cl.udec.cocinavecina.ui.common.EmptyState
import cl.udec.cocinavecina.ui.common.ErrorMessage
import cl.udec.cocinavecina.ui.common.Format
import kotlinx.coroutines.launch

@Composable
fun ModerationScreen(repository: Repository) {
    var queue by remember { mutableStateOf<List<OfferUnderReview>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var version by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(version) {
        repository.call { moderationQueue() }.onSuccess { queue = it }.onFailure { error = it.message }
    }
    ErrorMessage(error)
    if (queue.isEmpty()) return EmptyState("No hay ofertas en revisión.")
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(queue, key = { it.offerId }) { o ->
            var reason by remember(o.offerId) { mutableStateOf("") }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(o.title, style = MaterialTheme.typography.titleMedium)
                    Text("Vendedor ${o.sellerId} · ${o.sellerReportCount} reporte(s) en total")
                    o.reports.forEach { r -> Text("• ${Format.reportReason(r.reason)}${r.detail?.let { ": $it" } ?: ""}") }
                    OutlinedTextField(
                        reason,
                        { reason = it.take(200) },
                        label = { Text("Motivo de la decisión") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("REACTIVATE" to "Reactivar", "WITHDRAW" to "Retirar").forEach { (action, text) ->
                            val onClick = {
                                scope.launch {
                                    repository.call { decideOffer(o.offerId, Decision(action, reason)) }
                                        .onFailure { error = it.message }
                                    version++
                                }
                                Unit
                            }
                            if (action == "REACTIVATE") {
                                Button(onClick = onClick, enabled = reason.isNotBlank()) { Text(text) }
                            } else {
                                OutlinedButton(onClick = onClick, enabled = reason.isNotBlank()) { Text(text) }
                            }
                        }
                    }
                }
            }
        }
    }
}
