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
     * A single decode pass, no manual rotation retries. ZXing's QR detector locates a
     * code's three finder squares from their relative geometry, so it already reads a
     * code that is rotated within the frame (e.g. the phone held sideways) without the
     * caller pre-rotating the buffer -- see QrDecoderTest for a 90-degree-rotated
     * frame decoded in one pass. Retrying rotations here used to allocate three extra
     * width*height copies on every missed frame for no benefit.
     */
    fun decode(yPlane: ByteArray, width: Int, height: Int): String? {
        val source = PlanarYUVLuminanceSource(yPlane, width, height, 0, 0, width, height, false)
        val bitmap = BinaryBitmap(HybridBinarizer(source))
        return try {
            reader.decode(bitmap).text
        } catch (_: ReaderException) {
            // Covers NotFoundException (no finder pattern in this frame) as well as
            // ChecksumException/FormatException, which are routine on a partial or
            // motion-blurred frame while the user is still aiming the camera.
            null
        } finally {
            reader.reset()
        }
    }
}
