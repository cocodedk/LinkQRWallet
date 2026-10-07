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

    @Test
    fun blocksLocalIpv6Addresses() {
        val urls = listOf(
            "http://[::1]/",
            "http://[fd00::1]/",
            "http://[fc00::1]:8080/",
            "http://[fe80::1]/",
            "http://[::ffff:10.0.0.1]/"
        )
        urls.forEach { assertEquals(it, UnsafeReason.Private, UrlSafety.check(it).reason) }
    }

    @Test
    fun allowsPublicIpv6Addresses() {
        assertTrue(UrlSafety.check("https://[2606:4700:4700::1111]/").isSafe)
    }
}
