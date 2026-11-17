package cl.udec.cocinavecina.ui.explore

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.udec.cocinavecina.data.network.NearbyOffer
import cl.udec.cocinavecina.ui.common.CocinaVecinaMark
import cl.udec.cocinavecina.ui.common.Format
import cl.udec.cocinavecina.ui.common.RemoteImage

private const val URGENT_MINUTES = 60L

@Composable
fun OfferCard(
    offer: NearbyOffer,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OfferPhoto(offer.photoUrl)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    offer.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull("≈ ${Format.distance(offer.distanceMeters)}", offer.seller).joinToString(" · "),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    portionsLabel(offer.availablePortions),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    Modifier.fillMaxWidth().padding(top = 6.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            Format.pesos(offer.basePrice),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            "regateo desde ${Format.pesos(offer.minPrice)}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TimeLeftBadge(offer.expiresAt)
                }
            }
        }
    }
}

@Composable
private fun OfferPhoto(photoUrl: String?) {
    val shape = RoundedCornerShape(12.dp)
    if (photoUrl != null) {
        RemoteImage(
            model = photoUrl,
            contentDescription = null,
            modifier = Modifier.size(88.dp).clip(shape),
            markSize = 36.dp,
        )
    } else {
        Box(
            Modifier
                .size(88.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            CocinaVecinaMark(size = 36.dp)
        }
    }
}

@Composable
private fun TimeLeftBadge(expiresAt: String) {
    val urgent = Format.minutesLeft(expiresAt) < URGENT_MINUTES
    Text(
        Format.timeLeft(expiresAt),
        modifier =
            Modifier
                .clip(RoundedCornerShape(50))
                .background(if (urgent) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (urgent) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun portionsLabel(portions: Int): String = if (portions == 1) "1 porción" else "$portions porciones"
