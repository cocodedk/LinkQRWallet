package com.cocode.linkqrwallet

import com.cocode.linkqrwallet.data.UnsafeReason
import com.cocode.linkqrwallet.data.UrlSafety
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlSafetyTest {
    @Test
    fun blocksNonHttpSchemes() {
        assertFalse(UrlSafety.check("file:///etc/passwd").isSafe)
        assertFalse(UrlSafety.check("javascript:alert(1)").isSafe)
    }

    @Test
    fun blocksPrivateAddresses() {
        assertFalse(UrlSafety.check("http://127.0.0.1").isSafe)
        assertFalse(UrlSafety.check("http://192.168.1.10").isSafe)
        assertFalse(UrlSafety.check("http://10.0.0.5").isSafe)
    }

    @Test
    fun allowsPublicHttp() {
        assertTrue(UrlSafety.check("https://example.com").isSafe)
    }

    @Test
    fun saysWhyALinkWasRefused() {
        assertEquals(UnsafeReason.UnsafeScheme, UrlSafety.check("javascript:alert(1)").reason)
        assertEquals(UnsafeReason.NotHttp, UrlSafety.check("ftp://example.com").reason)
        assertEquals(UnsafeReason.Local, UrlSafety.check("http://localhost").reason)
        assertEquals(UnsafeReason.Onion, UrlSafety.check("http://example.onion").reason)
        assertEquals(UnsafeReason.Private, UrlSafety.check("http://10.0.0.5").reason)
        assertEquals(UnsafeReason.EncodedName, UrlSafety.check("http://xn--80ak6aa92e.com").reason)
    }
}
