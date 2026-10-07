package com.cocode.linkqrwallet.data

import java.net.InetAddress

/**
 * Which numeric addresses the app refuses to contact: its own phone, the local network and
 * reserved ranges. It works on the bytes of a parsed address, so every way of writing one is
 * covered. [UrlSafety] applies it to addresses written in a link, and [CheckedDns] to every
 * address a website name leads to.
 */
object AddressRules {
    private class Block(address: String, private val bits: Int) {
        private val prefix = InetAddress.getByName(address).address

        fun contains(bytes: ByteArray): Boolean {
            if (bytes.size != prefix.size) return false
            for (i in 0 until bits) {
                val mask = 0x80 shr (i % 8)
                if ((bytes[i / 8].toInt() and mask) != (prefix[i / 8].toInt() and mask)) return false
            }
            return true
        }
    }

    private val ipv4Blocks = listOf(
        Block("0.0.0.0", 8), Block("10.0.0.0", 8), Block("100.64.0.0", 10), Block("127.0.0.0", 8),
        Block("169.254.0.0", 16), Block("172.16.0.0", 12), Block("192.0.0.0", 24), Block("192.0.2.0", 24),
        Block("192.168.0.0", 16), Block("198.18.0.0", 15), Block("198.51.100.0", 24),
        Block("203.0.113.0", 24), Block("224.0.0.0", 4), Block("240.0.0.0", 4)
    )

    private val ipv6Blocks = listOf(
        Block("::", 128), Block("::1", 128), Block("fc00::", 7), Block("fe80::", 10),
        Block("fec0::", 10), Block("ff00::", 8), Block("2001:db8::", 32), Block("64:ff9b:1::", 48)
    )

    fun isBlocked(address: InetAddress): Boolean = isBlocked(address.address)

    fun isBlocked(bytes: ByteArray): Boolean = when (bytes.size) {
        4 -> ipv4Blocks.any { it.contains(bytes) }
        16 -> embeddedIpv4(bytes)?.let { isBlocked(it) } == true || ipv6Blocks.any { it.contains(bytes) }
        else -> true
    }

    /**
     * The IPv4 address inside an IPv4-mapped (::ffff:a.b.c.d), IPv4-compatible (::a.b.c.d) or NAT64
     * (64:ff9b::/96) address. The local-use NAT64 range 64:ff9b:1::/48 is blocked as a whole above.
     */
    private fun embeddedIpv4(bytes: ByteArray): ByteArray? {
        val zero = 0.toByte()
        val allZeroTo10 = (0..9).all { bytes[it] == zero }
        val mapped = allZeroTo10 && bytes[10] == 0xFF.toByte() && bytes[11] == 0xFF.toByte()
        val compatible = allZeroTo10 && bytes[10] == zero && bytes[11] == zero
        val nat64 = bytes[0] == zero && bytes[1] == 0x64.toByte() && bytes[2] == 0xFF.toByte() &&
            bytes[3] == 0x9B.toByte() && (4..11).all { bytes[it] == zero }
        return if (mapped || compatible || nat64) bytes.copyOfRange(12, 16) else null
    }
}
