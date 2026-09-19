package com.cocode.linkqrwallet

import com.cocode.linkqrwallet.ui.screens.resizeBufferIfNeeded
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test

/** Covers the buffer-reuse decision qrFrameAnalyzer relies on to avoid allocating a
 *  fresh Y-plane ByteArray on every camera frame -- the analyzer itself needs an
 *  ImageProxy and isn't exercised here. */
class QrImageAnalyzerTest {

    @Test
    fun resizeBufferIfNeeded_reusesTheSameArrayWhenSizeMatches() {
        val buffer = ByteArray(100)

        val result = resizeBufferIfNeeded(buffer, 100)

        assertSame(buffer, result)
    }

    @Test
    fun resizeBufferIfNeeded_allocatesAFreshArrayWhenSizeChanges() {
        val buffer = ByteArray(100)

        val result = resizeBufferIfNeeded(buffer, 200)

        assertNotSame(buffer, result)
        assertEquals(200, result.size)
    }
}
