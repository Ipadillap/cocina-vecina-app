package cl.udec.cocinavecina.ui.common


import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
private val LogoGreen = Color(0xFF1E4034)
private val LogoCream = Color(0xFFF0F2EC)
private val LogoOrange = Color(0xFFF28B5E)
private val LogoInk = Color(0xFF13241E)

/** Solo el símbolo (plato con vapor). Úsalo en íconos, avatares o donde no quepa el texto. */
@Composable
fun CocinaVecinaMark(
    modifier: Modifier = Modifier,
    size: Dp = 34.dp
) {
    Canvas(modifier = modifier.size(size)) {
        withTransform({
            scale(this@Canvas.size.width / 32f, this@Canvas.size.height / 32f, pivot = Offset.Zero)
        }) {
            drawRoundRect(
                color = LogoGreen,
                size = Size(32f, 32f),
                cornerRadius = CornerRadius(8f)
            )

            val bowl = Path().apply {
                moveTo(7f, 16.5f)
                lineTo(25f, 16.5f)
                arcTo(
                    rect = Rect(left = 7f, top = 7.5f, right = 25f, bottom = 25.5f),
                    startAngleDegrees = 0f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = false
                )
                close()
            }
            drawPath(bowl, LogoCream)

            val steam = Path().apply {
                listOf(11.5f, 16f, 20.5f).forEach { x ->
                    moveTo(x, 12.5f)
                    cubicTo(x - 1.2f, 10.9f, x + 1.2f, 9.9f, x, 8f)
                }
            }
            drawPath(
                path = steam,
                color = LogoOrange,
                style = Stroke(width = 1.8f, cap = StrokeCap.Round)
            )
        }
    }
}

/** Logo completo: símbolo + nombre. */
@Composable
fun CocinaVecinaLogo(
    modifier: Modifier = Modifier,
    markSize: Dp = 34.dp,
    textColor: Color = LogoInk
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CocinaVecinaMark(size = markSize)
        Text(
            text = "CocinaVecina",
            color = textColor,
            fontSize = 21.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.21).sp
        )
    }
}
