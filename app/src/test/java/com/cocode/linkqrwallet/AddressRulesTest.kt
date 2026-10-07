package com.cocode.linkqrwallet

import com.cocode.linkqrwallet.data.AddressRules
import java.net.InetAddress
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AddressRulesTest {
    private fun v4(a: Int, b: Int, c: Int, d: Int) = byteArrayOf(a.toByte(), b.toByte(), c.toByte(), d.toByte())

    private fun v6(vararg hextets: Int): ByteArray {
        require(hextets.size == 8)
        return ByteArray(16) { ((hextets[it / 2] shr (if (it % 2 == 0) 8 else 0)) and 0xFF).toByte() }
    }

    private fun name(bytes: ByteArray) = bytes.joinToString(".") { (it.toInt() and 0xFF).toString() }

    private fun assertBlocked(bytes: ByteArray) = assertTrue(name(bytes), AddressRules.isBlocked(bytes))

    private fun assertAllowed(bytes: ByteArray) = assertFalse(name(bytes), AddressRules.isBlocked(bytes))

    private fun assertRange(first: ByteArray, last: ByteArray, before: ByteArray?, after: ByteArray?) {
        assertBlocked(first)
        assertBlocked(last)
        before?.let { assertAllowed(it) }
        after?.let { assertAllowed(it) }
    }

    @Test
    fun ipv4RangesAreBlockedAtBothEdgesAndOpenJustOutside() {
        assertRange(v4(0, 0, 0, 0), v4(0, 255, 255, 255), null, v4(1, 0, 0, 0))
        assertRange(v4(10, 0, 0, 0), v4(10, 255, 255, 255), v4(9, 255, 255, 255), v4(11, 0, 0, 0))
        assertRange(v4(100, 64, 0, 0), v4(100, 127, 255, 255), v4(100, 63, 255, 255), v4(100, 128, 0, 0))
        assertRange(v4(127, 0, 0, 0), v4(127, 255, 255, 255), v4(126, 255, 255, 255), v4(128, 0, 0, 0))
        assertRange(v4(169, 254, 0, 0), v4(169, 254, 255, 255), v4(169, 253, 255, 255), v4(169, 255, 0, 0))
        assertRange(v4(172, 16, 0, 0), v4(172, 31, 255, 255), v4(172, 15, 255, 255), v4(172, 32, 0, 0))
        assertRange(v4(192, 0, 0, 0), v4(192, 0, 0, 255), v4(191, 255, 255, 255), v4(192, 0, 1, 0))
        assertRange(v4(192, 0, 2, 0), v4(192, 0, 2, 255), v4(192, 0, 1, 255), v4(192, 0, 3, 0))
        assertRange(v4(192, 168, 0, 0), v4(192, 168, 255, 255), v4(192, 167, 255, 255), v4(192, 169, 0, 0))
        assertRange(v4(198, 18, 0, 0), v4(198, 19, 255, 255), v4(198, 17, 255, 255), v4(198, 20, 0, 0))
        assertRange(v4(198, 51, 100, 0), v4(198, 51, 100, 255), v4(198, 51, 99, 255), v4(198, 51, 101, 0))
        assertRange(v4(203, 0, 113, 0), v4(203, 0, 113, 255), v4(203, 0, 112, 255), v4(203, 0, 114, 0))
        assertRange(v4(224, 0, 0, 0), v4(239, 255, 255, 255), v4(223, 255, 255, 255), null)
        assertRange(v4(240, 0, 0, 0), v4(255, 255, 255, 255), null, null)
    }

    @Test
    fun publicIpv4AddressesAreAllowed() {
        assertAllowed(v4(8, 8, 8, 8))
        assertAllowed(v4(1, 1, 1, 1))
        assertAllowed(v4(93, 184, 216, 34))
    }

    @Test
    fun ipv6RangesAreBlockedAtBothEdgesAndOpenJustOutside() {
        assertBlocked(v6(0, 0, 0, 0, 0, 0, 0, 0))
        assertBlocked(v6(0, 0, 0, 0, 0, 0, 0, 1))
        assertBlocked(v6(0xfc00, 0, 0, 0, 0, 0, 0, 0))
        assertBlocked(v6(0xfdff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff))
        assertAllowed(v6(0xfbff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff))
        assertAllowed(v6(0xfe00, 0, 0, 0, 0, 0, 0, 1))
        assertBlocked(v6(0xfe80, 0, 0, 0, 0, 0, 0, 1))
        assertBlocked(v6(0xfebf, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff))
        assertBlocked(v6(0xfec0, 0, 0, 0, 0, 0, 0, 1))
        assertBlocked(v6(0xff00, 0, 0, 0, 0, 0, 0, 1))
        assertBlocked(v6(0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff))
        assertBlocked(v6(0x2001, 0x0db8, 0, 0, 0, 0, 0, 0))
        assertBlocked(v6(0x2001, 0x0db8, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff))
        assertAllowed(v6(0x2001, 0x0db7, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff))
        assertAllowed(v6(0x2001, 0x0db9, 0, 0, 0, 0, 0, 0))
    }

    @Test
    fun publicIpv6AddressesAreAllowed() {
        assertAllowed(v6(0x2606, 0x4700, 0x4700, 0, 0, 0, 0, 0x1111))
        assertAllowed(v6(0x2001, 0x4860, 0x4860, 0, 0, 0, 0, 0x8888))
    }

    @Test
    fun anIpv4AddressInsideAnIpv6AddressIsCheckedAsIpv4() {
        // IPv4-mapped ::ffff:a.b.c.d
        assertBlocked(v6(0, 0, 0, 0, 0, 0xffff, 0x0a00, 0x0001))
        assertBlocked(v6(0, 0, 0, 0, 0, 0xffff, 0x7f00, 0x0001))
        assertAllowed(v6(0, 0, 0, 0, 0, 0xffff, 0x0808, 0x0808))
        // IPv4-compatible ::a.b.c.d
        assertBlocked(v6(0, 0, 0, 0, 0, 0, 0xc0a8, 0x0001))
        assertAllowed(v6(0, 0, 0, 0, 0, 0, 0x0808, 0x0808))
        // NAT64 64:ff9b::a.b.c.d
        assertBlocked(v6(0x64, 0xff9b, 0, 0, 0, 0, 0x7f00, 0x0001))
        assertBlocked(v6(0x64, 0xff9b, 0, 0, 0, 0, 0xa9fe, 0xa9fe))
        assertAllowed(v6(0x64, 0xff9b, 0, 0, 0, 0, 0x0808, 0x0808))
    }

    @Test
    fun aParsedAddressIsCheckedWhateverItsSpelling() {
        // The system reads a spelling such as 2130706433 or 0177.0.0.1 and hands over these bytes.
        assertTrue(AddressRules.isBlocked(InetAddress.getByAddress(v4(127, 0, 0, 1))))
        assertTrue(AddressRules.isBlocked(InetAddress.getByAddress(v4(10, 0, 0, 1))))
        // Java turns ::ffff:10.0.0.1 into a plain IPv4 address
        assertTrue(AddressRules.isBlocked(InetAddress.getByName("::ffff:10.0.0.1")))
        assertTrue(AddressRules.isBlocked(InetAddress.getByName("::1")))
        assertFalse(AddressRules.isBlocked(InetAddress.getByName("2606:4700:4700::1111")))
    }

    @Test
    fun anAddressOfAnUnknownLengthIsBlocked() {
        assertTrue(AddressRules.isBlocked(ByteArray(5)))
        assertTrue(AddressRules.isBlocked(ByteArray(0)))
    }
}
