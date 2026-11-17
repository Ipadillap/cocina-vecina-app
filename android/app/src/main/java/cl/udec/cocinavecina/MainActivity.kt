package cl.udec.cocinavecina

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cl.udec.cocinavecina.ui.AppNavigation
import cl.udec.cocinavecina.ui.theme.CocinaVecinaTheme
import cl.udec.cocinavecina.ui.verification.VerificationLink

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as CocinaVecinaApp).container
        if (savedInstanceState == null) confirmEmail(intent)
        setContent {
            CocinaVecinaTheme {
                AppNavigation(container)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        confirmEmail(intent)
    }

    private fun confirmEmail(intent: Intent?) {
        VerificationLink.tokenFrom(intent)?.let { (application as CocinaVecinaApp).container.verification.confirm(it) }
    }
}
