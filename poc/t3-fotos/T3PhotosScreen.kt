package cl.udec.poc.t3

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun T3PhotosScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("T3: Selecciona una imagen para comprimir a <= 300 KB y subir a Nginx") }
    var imageUrl by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val bytes = withContext(Dispatchers.IO) { compressUnder300KB(ctx, uri) }
                val filename = "t3_${System.currentTimeMillis()}.jpg"
                val ok = withContext(Dispatchers.IO) { uploadToPocServer(filename, bytes) }
                if (ok) {
                    imageUrl = "http://10.0.2.2:8081/photos/$filename"
                    status = "T3 OK: ${bytes.size / 1024} KB subidos y servidos desde Nginx"
                } else {
                    status = "T3 CompresiÃ³n OK (${bytes.size / 1024} KB) | Inicia upload_server.py en puerto 8082"
                }
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("T3: Fotos <= 0,3 MB + Nginx", style = MaterialTheme.typography.titleMedium)
        Text(status, style = MaterialTheme.typography.bodySmall)
        Button(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
            Text("Elegir foto y subir a Nginx")
        }
        imageUrl?.let {
            AsyncImage(model = it, contentDescription = "Foto servida por Nginx", modifier = Modifier.fillMaxWidth().height(260.dp))
        }
    }
}

private fun compressUnder300KB(ctx: Context, uri: Uri): ByteArray {
    val bmp = ctx.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it) }
    val scale = 1080f / max(bmp.width, bmp.height)
    val scaled = if (scale < 1f) Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt(), (bmp.height * scale).toInt(), true) else bmp
    var q = 85
    var out: ByteArray
    do {
        val baos = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, q, baos)
        out = baos.toByteArray()
        q -= 10
    } while (out.size > 300_000 && q >= 30)
    return out
}

private fun uploadToPocServer(filename: String, bytes: ByteArray): Boolean = runCatching {
    val conn = (URL("http://10.0.2.2:8082/$filename").openConnection() as HttpURLConnection).apply {
        requestMethod = "PUT"
        doOutput = true
        setRequestProperty("Content-Type", "image/jpeg")
    }
    conn.outputStream.use { it.write(bytes) }
    conn.responseCode == 204
}.getOrDefault(false)
