package cl.udec.cocinavecina.ui.publish

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
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
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) error("No se pudo leer la imagen")
        var sampleSize = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= MAX_SIDE) sampleSize *= 2
        val original =
            context.contentResolver.openInputStream(uri).use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sampleSize })
            } ?: error("No se pudo leer la imagen")
        val scale = MAX_SIDE.toFloat() / max(original.width, original.height)
        val matrix = orientationMatrix(readOrientation(context, uri))
        if (scale < 1f) matrix.postScale(scale, scale)
        val scaled =
            if (matrix.isIdentity) original else Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
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

    private fun readOrientation(
        context: Context,
        uri: Uri,
    ): Int =
        runCatching {
            context.contentResolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

    private fun orientationMatrix(orientation: Int): Matrix =
        Matrix().apply {
            when (orientation) {
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                    setRotate(180f)
                    postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    setRotate(90f)
                    postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    setRotate(270f)
                    postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(270f)
            }
        }
}
