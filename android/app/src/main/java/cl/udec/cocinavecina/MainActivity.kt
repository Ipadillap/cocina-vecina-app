package cl.udec.cocinavecina

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cl.udec.cocinavecina.ui.AppNavigation
import cl.udec.cocinavecina.ui.publish.PublishScreen
import cl.udec.cocinavecina.ui.theme.CocinaVecinaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as CocinaVecinaApp).container
        setContent {
            CocinaVecinaTheme {
                //AppNavigation(container)
                PublishScreen(container.repository, {}, onExit = {})
            }
        }
    }
}
