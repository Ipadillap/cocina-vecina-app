package cl.udec.cocinavecina.ui.orders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.udec.cocinavecina.data.Repository
import cl.udec.cocinavecina.data.network.OrderView
import cl.udec.cocinavecina.ui.common.EmptyState
import cl.udec.cocinavecina.ui.common.ErrorMessage
import cl.udec.cocinavecina.ui.common.Format

@Composable
fun OrdersScreen(
    repository: Repository,
    onOpen: (Long) -> Unit,
) {
    var orders by remember { mutableStateOf<List<OrderView>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        repository.call { myOrders() }.onSuccess { orders = it }.onFailure { error = it.message }
    }
    ErrorMessage(error)
    val list = orders ?: return
    if (list.isEmpty()) return EmptyState("Aún no tienes pedidos.")
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(list, key = { it.id }) { p ->
            Card(Modifier.fillMaxWidth().clickable { onOpen(p.id) }) {
                Column(Modifier.padding(16.dp)) {
                    Text("Pedido ${p.id} · ${Format.orderStatus(p.status)}", style = MaterialTheme.typography.titleMedium)
                    Text("${p.portions} porción(es) · ${Format.pesos(p.agreedPrice)} c/u")
                }
            }
        }
    }
}
