package cl.udec.cocinavecina.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.udec.cocinavecina.ui.common.Format

data class PriceMark(val price: Int, val label: String, val emphasized: Boolean = true)

@Composable
fun PriceScale(
    minPrice: Int,
    maxPrice: Int,
    minLabel: String,
    maxLabel: String,
    marks: List<PriceMark>,
    modifier: Modifier = Modifier,
) {
    val range = (maxPrice - minPrice).coerceAtLeast(1)
    val fractions = marks.map { ((it.price - minPrice).toFloat() / range).coerceIn(0f, 1f) }
    val description =
        "Escala de precio entre ${Format.pesos(minPrice)} y ${Format.pesos(maxPrice)}. " +
            marks.joinToString { "${it.label}: ${Format.pesos(it.price)}" }
    Column(
        modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        MarkLabels(marks, fractions)
        ScaleTrack(fractions, marks)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            EndLabel(minLabel, minPrice, Alignment.Start)
            EndLabel(maxLabel, maxPrice, Alignment.End)
        }
    }
}

@Composable
private fun MarkLabels(
    marks: List<PriceMark>,
    fractions: List<Float>,
) {
    Layout(
        content = {
            marks.forEach { mark ->
                Text(
                    "${mark.label} ${Format.pesos(mark.price)}",
                    fontSize = 12.sp,
                    fontWeight = if (mark.emphasized) FontWeight.Bold else FontWeight.Normal,
                    color = if (mark.emphasized) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0)) }
        val height = placeables.maxOfOrNull { it.height } ?: 0
        layout(constraints.maxWidth, height) {
            placeables.forEachIndexed { index, placeable ->
                val center = (fractions[index] * constraints.maxWidth).toInt()
                val x = (center - placeable.width / 2).coerceIn(0, (constraints.maxWidth - placeable.width).coerceAtLeast(0))
                placeable.placeRelative(x, 0)
            }
        }
    }
}

@Composable
private fun ScaleTrack(
    fractions: List<Float>,
    marks: List<PriceMark>,
) {
    val trackColor = MaterialTheme.colorScheme.outlineVariant
    val pathColor = MaterialTheme.colorScheme.secondary
    val dotColor = MaterialTheme.colorScheme.secondary
    val mutedDotColor = MaterialTheme.colorScheme.outline
    val dotBorderColor = MaterialTheme.colorScheme.surface
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(20.dp),
    ) {
        val centerY = size.height / 2
        val dotRadius = 7.dp.toPx()
        val start = dotRadius
        val end = size.width - dotRadius
        fun xOf(fraction: Float) = start + (end - start) * fraction
        drawLine(trackColor, Offset(start, centerY), Offset(end, centerY), strokeWidth = 6.dp.toPx(), cap = StrokeCap.Round)
        drawLine(trackColor, Offset(start, centerY - 8.dp.toPx()), Offset(start, centerY + 8.dp.toPx()), strokeWidth = 2.dp.toPx())
        drawLine(trackColor, Offset(end, centerY - 8.dp.toPx()), Offset(end, centerY + 8.dp.toPx()), strokeWidth = 2.dp.toPx())
        if (fractions.size >= 2) {
            drawLine(
                pathColor,
                Offset(xOf(fractions.min()), centerY),
                Offset(xOf(fractions.max()), centerY),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        fractions.forEachIndexed { index, fraction ->
            val center = Offset(xOf(fraction), centerY)
            drawCircle(dotBorderColor, radius = dotRadius + 2.dp.toPx(), center = center)
            drawCircle(if (marks[index].emphasized) dotColor else mutedDotColor, radius = dotRadius, center = center)
        }
    }
}

@Composable
private fun EndLabel(
    label: String,
    price: Int,
    alignment: Alignment.Horizontal,
) {
    Column(horizontalAlignment = alignment) {
        Text(
            label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = if (alignment == Alignment.End) TextAlign.End else TextAlign.Start,
        )
        Text(Format.pesos(price), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}
