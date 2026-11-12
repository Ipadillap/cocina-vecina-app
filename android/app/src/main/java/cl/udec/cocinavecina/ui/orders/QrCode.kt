package cl.udec.cocinavecina.ui.orders

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

object QrCode {
    fun text(
        orderId: Long,
        code: String,
    ) = "CV:$orderId:$code"

    fun read(
        content: String?,
        orderId: Long,
    ): String? {
        val parts = content?.split(":") ?: return null
        return if (parts.size == 3 && parts[0] == "CV" && parts[1] == orderId.toString()) parts[2] else null
    }

    fun image(
        content: String,
        side: Int = 600,
    ): ImageBitmap {
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, side, side)
        val bitmap = Bitmap.createBitmap(side, side, Bitmap.Config.RGB_565)
        for (x in 0 until side) {
            for (y in 0 until side) {
                bitmap.setPixel(x, y, if (matrix.get(x, y)) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap.asImageBitmap()
    }
}
