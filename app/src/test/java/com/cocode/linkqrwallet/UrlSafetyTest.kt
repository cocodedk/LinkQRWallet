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

    @Test
    fun refusesEveryNumericHostThatIsNotPlainDottedDecimal() {
        // Different programs read these differently (0127.0.0.1 is 87.0.0.1 or 127.0.0.1), so none is accepted.
        val urls = listOf(
            "http://2130706433/",
            "http://0127.0.0.1/",
            "http://010.0.0.1/",
            "http://0177.0.0.1/",
            "http://0x7f.0.0.1/",
            "http://0x7f000001/",
            "http://127.1/",
            "http://127.0.0.1./",
            "http://8.8.8.8./",
            "http://134744072/",
            "http://1.2.3/",
            "http://08.8.8.8/",
            "http://1.2.3.256/"
        )
        urls.forEach { assertFalse(it, UrlSafety.check(it).isSafe) }
        assertEquals(UnsafeReason.Invalid, UrlSafety.check("http://0127.0.0.1/").reason)
        assertEquals(UnsafeReason.Invalid, UrlSafety.check("http://2130706433/").reason)
        assertEquals(UnsafeReason.Local, UrlSafety.check("http://localhost./").reason)
    }

    @Test
    fun checksPlainDottedDecimalAndIpv6LiteralsOnTheirBytes() {
        val blocked = listOf(
            "http://127.0.0.1/", "http://100.64.0.1/", "http://192.0.2.1/", "http://198.18.0.1/",
            "http://255.255.255.255/", "http://[64:ff9b::7f00:1]/", "http://[::ffff:127.0.0.1]/"
        )
        blocked.forEach { assertEquals(it, UnsafeReason.Private, UrlSafety.check(it).reason) }
        assertTrue(UrlSafety.check("http://87.0.0.1/").isSafe)
        assertTrue(UrlSafety.check("https://1.1.1.1/").isSafe)
        assertTrue(UrlSafety.check("https://93.184.216.34/page").isSafe)
    }

    @Test
    fun allowsNamesThatLookALittleLikeNumbers() {
        assertTrue(UrlSafety.check("https://1password.com/").isSafe)
        assertTrue(UrlSafety.check("https://example.com./").isSafe)
        assertTrue(UrlSafety.check("https://0x.example.com/").isSafe)
        assertTrue(UrlSafety.check("https://123.example.com/").isSafe)
    }
}
