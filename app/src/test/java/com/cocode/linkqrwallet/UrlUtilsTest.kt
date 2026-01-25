package com.cocode.linkqrwallet

import com.cocode.linkqrwallet.data.UrlUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UrlUtilsTest {
    @Test
    fun normalizeUrl_addsSchemeAndValidates() {
        assertEquals("https://example.com", UrlUtils.normalizeUrl("example.com"))
        assertEquals("https://example.com/path", UrlUtils.normalizeUrl("https://example.com/path"))
        assertNull(UrlUtils.normalizeUrl("not a url"))
    }

    @Test
    fun domainFromUrl_stripsWww() {
        assertEquals("example.com", UrlUtils.domainFromUrl("https://www.example.com/hello"))
    }
}
