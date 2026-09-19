package com.cocode.linkqrwallet

import com.cocode.linkqrwallet.data.QrDecoder
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QrDecoderTest {

    /** Builds a Y-plane luminance frame from a QR code written by ZXing itself, so the
     *  decode path is exercised without a camera or an Android Bitmap. */
    private fun renderToYPlane(content: String, size: Int): ByteArray {
        val matrix = QRCodeWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            size,
            size,
            mapOf(EncodeHintType.MARGIN to 4)
        )
        val yPlane = ByteArray(size * size)
        for (y in 0 until size) {
            for (x in 0 until size) {
                // Dark module = low luminance, light module = high luminance.
                yPlane[y * size + x] = if (matrix[x, y]) 0 else 0xFF.toByte()
            }
        }
        return yPlane
    }

    @Test
    fun decode_readsBackWhatWasEncoded() {
        val content = "https://example.com/hello"
        val size = 200

        val decoded = QrDecoder.decode(renderToYPlane(content, size), size, size)

        assertEquals(content, decoded)
    }

    @Test
    fun decode_returnsNullWhenNoCodeIsPresent() {
        val blankFrame = ByteArray(100 * 100) { 0xFF.toByte() }

        assertNull(QrDecoder.decode(blankFrame, 100, 100))
    }
}
