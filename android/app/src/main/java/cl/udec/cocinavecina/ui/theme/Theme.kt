package cl.udec.cocinavecina.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val Green = Color(0xFF1E4034)
private val GreenInk = Color(0xFF13241E)
private val TextMuted = Color(0xFF4F5E57)

private val Terracotta = Color(0xFFB8431A)
private val TerracottaDarker = Color(0xFF7E2A0D)
private val TerracottaSoft = Color(0xFFF6E3D9)

private val Mint = Color(0xFFDCE7DF)
private val AppBackground = Color(0xFFF0F2EC)
private val Surface = Color(0xFFFFFFFF)
private val GrayLighter = Color(0xFFEEF1EC)
private val Border = Color(0xFFDDE3DA)
private val BorderStrong = Color(0xFFC9D2C6)
private val Success = Color(0xFF2F8F5B)
private val Placeholder = Color(0xFF9FB0A6)

private val AppColors =
    lightColorScheme(
        primary = Green,
        onPrimary = Color.White,
        primaryContainer = Mint,
        onPrimaryContainer = GreenInk,
        secondary = Terracotta,
        onSecondary = Color.White,
        secondaryContainer = TerracottaSoft,
        onSecondaryContainer = TerracottaDarker,
        tertiary = Success,
        onTertiary = Color.White,
        background = AppBackground,
        onBackground = GreenInk,
        surface = Surface,
        onSurface = GreenInk,
        surfaceVariant = GrayLighter,
        onSurfaceVariant = TextMuted,
        outline = BorderStrong,
        outlineVariant = Border,
        error = Color(0xFFB3261E),
        onError = Color.White,
    )

@Immutable
class ExtraColors(val placeholder: Color)

private val LocalExtraColors = staticCompositionLocalOf { ExtraColors(placeholder = Placeholder) }

val MaterialTheme.extraColors: ExtraColors
    @Composable
    @ReadOnlyComposable
    get() = LocalExtraColors.current

@Composable
fun CocinaVecinaTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalExtraColors provides ExtraColors(placeholder = Placeholder)) {
        MaterialTheme(colorScheme = AppColors, content = content)
    }
}
