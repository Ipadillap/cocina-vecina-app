package cl.udec.cocinavecina.ui.orders

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight

enum class OrdersSection(val label: String) {
    ORDERS("Pedidos"),
    REQUESTS("Solicitudes"),
}

@Composable
fun OrdersHub(
    section: OrdersSection,
    onSectionChange: (OrdersSection) -> Unit,
    ordersContent: @Composable () -> Unit,
    requestsContent: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = section.ordinal,
            containerColor = MaterialTheme.colorScheme.background,
        ) {
            OrdersSection.entries.forEach { entry ->
                Tab(
                    selected = entry == section,
                    onClick = { onSectionChange(entry) },
                    text = { Text(entry.label, fontWeight = if (entry == section) FontWeight.Bold else FontWeight.Normal) },
                )
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (section) {
                OrdersSection.ORDERS -> ordersContent()
                OrdersSection.REQUESTS -> requestsContent()
            }
        }
    }
}
