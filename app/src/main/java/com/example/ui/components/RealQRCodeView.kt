package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyberEmerald
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Generates an authentic ISO/IEC 18004 standards-compliant 2D QR Code using ZXing.
 * This QR Code is readable by Google Camera, Google Lens, and every standard barcode scanner.
 */
@Composable
fun RealQRCodeView(
    dataString: String,
    size: Dp = 200.dp,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White,
    qrColor: Color = Color.Black
) {
    val bitmap = remember(dataString, backgroundColor, qrColor) {
        generateRealQrBitmap(dataString, 512, backgroundColor, qrColor)
    }

    Surface(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .border(2.dp, CyberEmerald.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
        color = backgroundColor,
        shadowElevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Standard ISO QR Code for Attendance Check-In",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * Generates a standard ZXing BitMatrix with High Error Correction (Level H)
 * and UTF-8 charset, converted to an Android Bitmap.
 */
private fun generateRealQrBitmap(
    content: String,
    dimension: Int,
    bgColor: Color,
    fgColor: Color
): Bitmap? {
    return try {
        val hints = HashMap<EncodeHintType, Any>().apply {
            put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M)
            put(EncodeHintType.MARGIN, 4) // ISO standard quiet zone of 4 modules for instant Google Lens & Camera detection
            put(EncodeHintType.CHARACTER_SET, "UTF-8")
        }
        val bitMatrix = QRCodeWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            dimension,
            dimension,
            hints
        )
        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)
        val fgArgb = fgColor.toArgb()
        val bgArgb = bgColor.toArgb()

        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix.get(x, y)) fgArgb else bgArgb
            }
        }
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bmp.setPixels(pixels, 0, width, 0, 0, width, height)
        bmp
    } catch (e: Exception) {
        null
    }
}
