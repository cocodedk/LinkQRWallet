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
        val urls = listOf(
            "javascript:alert(1)",
            "ftp://example.com",
            "http://localhost",
            "http://example.onion",
            "http://10.0.0.5",
            "http://xn--80ak6aa92e.com"
        )
        assertEquals(
            listOf(
                UnsafeReason.UnsafeScheme,
                UnsafeReason.NotHttp,
                UnsafeReason.Local,
                UnsafeReason.Onion,
                UnsafeReason.Private,
                UnsafeReason.EncodedName
            ),
            urls.map { UrlSafety.check(it).reason }
        )
    }
}
