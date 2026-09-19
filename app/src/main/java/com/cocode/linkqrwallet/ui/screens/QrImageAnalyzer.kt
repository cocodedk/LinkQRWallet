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
 *
 * The Y-plane buffer is reused across frames (reallocated only when a frame's
 * width*height differs from what's already allocated), instead of a fresh ~1.2 MB
 * ByteArray per frame at typical analysis resolutions.
 */
fun qrFrameAnalyzer(hasResult: AtomicBoolean, onDecoded: (String) -> Unit): ImageAnalysis.Analyzer {
    var buffer = ByteArray(0)
    return ImageAnalysis.Analyzer { imageProxy ->
        if (hasResult.get()) {
            imageProxy.close()
            return@Analyzer
        }

        // Extraction and metadata reads are wrapped so a plane/buffer failure on a
        // single frame can't leak the frame (a skipped close stalls CameraX's pipeline
        // under STRATEGY_KEEP_ONLY_LATEST) or crash the analyzer thread -- it's just
        // treated as no result this frame.
        var width = 0
        var height = 0
        var extracted = false
        try {
            width = imageProxy.width
            height = imageProxy.height
            buffer = resizeBufferIfNeeded(buffer, width * height)
            imageProxy.toYByteArray(buffer)
            extracted = true
        } catch (_: Exception) {
            // Fall through with extracted = false.
        } finally {
            // Release the frame immediately -- ZXing decoding below is CPU work on
            // this same background thread and does not need the camera to hold the
            // buffer open while it runs.
            imageProxy.close()
        }
        if (!extracted) return@Analyzer

        val value = QrDecoder.decode(buffer, width, height)
        if (!value.isNullOrBlank()) {
            onDecoded(value)
        }
    }
}

/** Reuses [current] when its size already matches [neededSize], else allocates fresh. */
internal fun resizeBufferIfNeeded(current: ByteArray, neededSize: Int): ByteArray =
    if (current.size == neededSize) current else ByteArray(neededSize)

/**
 * Copies the Y (luminance) plane of a YUV_420_888 frame into [out] (sized
 * width*height by the caller). QR detection only needs luminance, and some devices
 * pad each row (rowStride > width), so this copies row-by-row rather than assuming a
 * tightly packed buffer.
 */
private fun ImageProxy.toYByteArray(out: ByteArray) {
    val plane = planes[0]
    val buffer = plane.buffer
    val rowStride = plane.rowStride
    val pixelStride = plane.pixelStride
    if (pixelStride == 1 && rowStride == width) {
        buffer.get(out, 0, width * height)
        return
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
}
