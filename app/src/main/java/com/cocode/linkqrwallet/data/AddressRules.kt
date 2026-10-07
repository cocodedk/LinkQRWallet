package com.cocode.linkqrwallet.data

import java.net.Inet4Address
import java.net.InetAddress

/**
 * Which numeric addresses the app refuses to contact: its own phone, the local network, and
 * reserved ranges. [UrlSafety] applies this to addresses written in a link, and [TitleFetcher]
 * applies it to where a website name leads and to every redirect.
 */
object AddressRules {
    fun isBlocked(address: InetAddress): Boolean {
        if (address.isLoopbackAddress || address.isAnyLocalAddress || address.isLinkLocalAddress ||
            address.isSiteLocalAddress || address.isMulticastAddress
        ) {
            return true
        }
        val bytes = address.address
        return if (address is Inet4Address) {
            isBlockedIpv4(IntArray(4) { bytes[it].toInt() and 0xFF })
        } else {
            // fc00::/7, the IPv6 unique local range
            (bytes[0].toInt() and 0xFE) == 0xFC
        }
    }

    fun isBlockedIpv4(bytes: IntArray): Boolean {
        val b0 = bytes[0]
        val b1 = bytes[1]
        return when {
            b0 == 10 -> true
            b0 == 127 -> true
            b0 == 0 -> true
            b0 == 169 && b1 == 254 -> true
            b0 == 192 && b1 == 168 -> true
            b0 == 172 && b1 in 16..31 -> true
            b0 >= 224 -> true
            else -> false
        }
    }
}
