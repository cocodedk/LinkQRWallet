package com.cocode.linkqrwallet.ui.screens

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.cocode.linkqrwallet.data.QrDecoder
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Builds a CameraX analyzer that decodes QR codes from each frame's Y (luminance)
 * plane via ZXing and calls [onDecoded] with the raw text once per code. [hasResult]
 * gates further decode attempts after a hit -- ScanQrScreen owns clearing it, since
 * whether a decoded value is accepted (a valid, safe URL) is a UI-level decision.
 */
fun qrFrameAnalyzer(hasResult: AtomicBoolean, onDecoded: (String) -> Unit) =
    ImageAnalysis.Analyzer { imageProxy ->
        if (hasResult.get()) {
            imageProxy.close()
            return@Analyzer
        }
        // Copy the Y plane out and release the frame immediately -- ZXing decoding is
        // CPU work on this same background thread and does not need the camera to
        // hold the buffer open while it runs.
        val yPlane = imageProxy.toYByteArray()
        val width = imageProxy.width
        val height = imageProxy.height
        imageProxy.close()

        val value = QrDecoder.decode(yPlane, width, height)
        if (!value.isNullOrBlank()) {
            onDecoded(value)
        }
    }

/**
 * Copies out the Y (luminance) plane of a YUV_420_888 frame. QR detection only needs
 * luminance, and some devices pad each row (rowStride > width), so this copies
 * row-by-row rather than assuming a tightly packed buffer.
 */
private fun ImageProxy.toYByteArray(): ByteArray {
    val plane = planes[0]
    val buffer = plane.buffer
    val rowStride = plane.rowStride
    val pixelStride = plane.pixelStride
    val out = ByteArray(width * height)
    if (pixelStride == 1 && rowStride == width) {
        buffer.get(out)
        return out
    }
    val row = ByteArray(rowStride)
    var outPos = 0
    for (y in 0 until height) {
        buffer.position(y * rowStride)
        buffer.get(row, 0, minOf(rowStride, buffer.remaining()))
        for (x in 0 until width) {
            out[outPos++] = row[x * pixelStride]
        }
    }
    return out
}
