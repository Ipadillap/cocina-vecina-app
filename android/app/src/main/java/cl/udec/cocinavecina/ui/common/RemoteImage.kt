package cl.udec.cocinavecina.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import coil.compose.SubcomposeAsyncImage

/** Foto remota que muestra el logo mientras carga y si la carga falla. */
@Composable
fun RemoteImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    markSize: Dp,
) {
    SubcomposeAsyncImage(
        model = model,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier,
        loading = { LogoPlaceholder(markSize) },
        error = { LogoPlaceholder(markSize) },
    )
}

@Composable
private fun LogoPlaceholder(markSize: Dp) {
    Box(
        Modifier.background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        CocinaVecinaMark(size = markSize)
    }
}
