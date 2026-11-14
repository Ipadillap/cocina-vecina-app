package cl.udec.cocinavecina

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cl.udec.cocinavecina.data.session.SessionState
import cl.udec.cocinavecina.ui.AppNavigation
import cl.udec.cocinavecina.ui.orders.OrderScreen
import cl.udec.cocinavecina.ui.requests.RequestsScreen
import cl.udec.cocinavecina.ui.theme.CocinaVecinaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as CocinaVecinaApp).container
        setContent {
            CocinaVecinaTheme {
                val fakeSession = SessionState.Active(
                    userId = 1L,
                    role = "SELLER"
                )
                //AppNavigation(container)
                //PublishScreen(container.repository, {}, onExit = {})
                //RequestsScreen(container, session = fakeSession, onOpenOrder = {})
                OrderScreen(container, session = fakeSession, orderId = 1L)
            }
        }
    }
}
