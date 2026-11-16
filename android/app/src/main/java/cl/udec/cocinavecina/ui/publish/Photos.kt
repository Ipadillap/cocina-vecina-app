package cl.udec.cocinavecina.ui.publish

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import kotlin.math.max

object Photos {
    private const val MAX_SIDE = 1080
    private const val MAX_BYTES = 300_000

    fun compress(
        context: Context,
        uri: Uri,
    ): ByteArray {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
        var sampleSize = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= MAX_SIDE) sampleSize *= 2
        val original =
            context.contentResolver.openInputStream(uri).use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sampleSize })
            } ?: error("No se pudo leer la imagen")
        val scale = MAX_SIDE.toFloat() / max(original.width, original.height)
        val scaled =
            if (scale < 1f) {
                Bitmap.createScaledBitmap(original, (original.width * scale).toInt(), (original.height * scale).toInt(), true)
            } else {
                original
            }
        var quality = 85
        var bytes: ByteArray
        do {
            val output = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, output)
            bytes = output.toByteArray()
            quality -= 10
        } while (bytes.size > MAX_BYTES && quality >= 35)
        return bytes
    }
}
