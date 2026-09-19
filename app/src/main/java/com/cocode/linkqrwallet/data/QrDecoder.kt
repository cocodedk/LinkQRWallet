package com.cocode.linkqrwallet.data

import com.google.zxing.BinaryBitmap
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader

/**
 * Decodes a QR code from a camera frame's luminance (Y) plane using ZXing directly,
 * rather than ML Kit / Google Play Services barcode scanning. ML Kit's barcode module
 * is proprietary and F-Droid's build scanner rejects it outright -- this keeps the
 * dependency surface to the already-used `com.google.zxing:core`.
 */
object QrDecoder {
    private val reader = QRCodeReader()

    /**
     * Tries the frame as captured, then rotated 90/180/270 degrees. CameraX hands the
     * analyzer frames in the sensor's native orientation, which for a phone held
     * upright usually needs a 90-degree turn before the finder pattern reads --
     * trying all four avoids depending on a device- or orientation-specific constant.
     */
    fun decode(yPlane: ByteArray, width: Int, height: Int): String? {
        var data = yPlane
        var w = width
        var h = height
        repeat(4) { attempt ->
            val source = PlanarYUVLuminanceSource(data, w, h, 0, 0, w, h, false)
            val bitmap = BinaryBitmap(HybridBinarizer(source))
            try {
                return reader.decode(bitmap).text
            } catch (_: ReaderException) {
                // Covers NotFoundException (no finder pattern at this rotation) as well
                // as ChecksumException/FormatException, which are routine on a partial
                // or motion-blurred frame while the user is still aiming the camera --
                // none of these mean decoding should stop, only that this frame/
                // rotation didn't produce a code.
            } finally {
                reader.reset()
            }
            if (attempt < 3) {
                data = rotate90(data, w, h)
                val rotatedWidth = h
                h = w
                w = rotatedWidth
            }
        }
        return null
    }

    /** Rotates a single-byte-per-pixel plane 90 degrees clockwise. */
    private fun rotate90(data: ByteArray, width: Int, height: Int): ByteArray {
        val rotated = ByteArray(data.size)
        var pos = 0
        for (x in 0 until width) {
            for (y in height - 1 downTo 0) {
                rotated[pos++] = data[y * width + x]
            }
        }
        return rotated
    }
}
