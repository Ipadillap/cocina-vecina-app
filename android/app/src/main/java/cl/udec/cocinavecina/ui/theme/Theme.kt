package cl.udec.cocinavecina.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppColors = lightColorScheme(
    primary = Color(0xFFC62F1A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD3),
    onPrimaryContainer = Color(0xFF410000),
    secondary = Color(0xFFB4530A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDCC6),
    onSecondaryContainer = Color(0xFF331200),
    background = Color(0xFFFFF8F3),
    onBackground = Color(0xFF2B211C),
    surface = Color(0xFFFFF8F3),
    onSurface = Color(0xFF2B211C),
    error = Color(0xFFB3261E),
)

@Composable
fun CocinaVecinaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AppColors, content = content)
}
