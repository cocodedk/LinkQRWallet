package com.cocode.linkqrwallet.data

import java.net.InetAddress
import java.net.URI

/** Why a link was refused. The screens turn each one into a message the person can read. */
enum class UnsafeReason {
    Invalid,
    NoScheme,
    UnsafeScheme,
    NotHttp,
    NoHost,
    Local,
    Onion,
    Private,
    EncodedName
}

data class UrlSafetyResult(
    val isSafe: Boolean,
    val reason: UnsafeReason? = null
)

object UrlSafety {
    private val blockedSchemes = setOf("file", "javascript", "data", "content", "intent")
    private val blockedHosts = setOf("localhost")

    fun check(url: String): UrlSafetyResult {
        val uri = try {
            URI(url)
        } catch (_: Exception) {
            return UrlSafetyResult(false, UnsafeReason.Invalid)
        }
        val scheme = uri.scheme?.lowercase() ?: return UrlSafetyResult(false, UnsafeReason.NoScheme)
        if (scheme in blockedSchemes) {
            return UrlSafetyResult(false, UnsafeReason.UnsafeScheme)
        }
        if (scheme != "http" && scheme != "https") {
            return UrlSafetyResult(false, UnsafeReason.NotHttp)
        }
        val host = uri.host?.lowercase()?.trimEnd('.') ?: return UrlSafetyResult(false, UnsafeReason.NoHost)
        if (host in blockedHosts || host.endsWith(".local")) {
            return UrlSafetyResult(false, UnsafeReason.Local)
        }
        if (host.endsWith(".onion")) {
            return UrlSafetyResult(false, UnsafeReason.Onion)
        }
        val ipv4 = parseNumericIpv4(host)
        if (ipv4 != null && AddressRules.isBlocked(ipv4)) {
            return UrlSafetyResult(false, UnsafeReason.Private)
        }
        if (host.startsWith("[") && host.endsWith("]") && isBlockedIpv6Literal(host)) {
            return UrlSafetyResult(false, UnsafeReason.Private)
        }
        if (host.startsWith("xn--")) {
            return UrlSafetyResult(false, UnsafeReason.EncodedName)
        }
        return UrlSafetyResult(true)
    }

    /** [host] is an IPv6 address in brackets, which only a valid numeric address can be, so nothing is looked up. */
    private fun isBlockedIpv6Literal(host: String): Boolean = try {
        AddressRules.isBlocked(InetAddress.getByName(host.substring(1, host.length - 1)))
    } catch (_: Exception) {
        true
    }

    /**
     * Reads [host] as a numeric IPv4 address the way the system does, so 127.0.0.1, 127.1,
     * 2130706433, 0x7f.0.0.1 and 0177.0.0.1 all give the same bytes. Anything else gives null.
     */
    private fun parseNumericIpv4(host: String): ByteArray? {
        val parts = host.split(".")
        if (parts.size > 4) return null
        val numbers = parts.map { parseIpv4Part(it) ?: return null }
        val last = numbers.last()
        val leading = numbers.dropLast(1)
        if (leading.any { it > 255 } || last >= (1L shl (8 * (5 - parts.size)))) return null
        var value = last
        leading.forEachIndexed { index, number -> value = value or (number shl (24 - 8 * index)) }
        return ByteArray(4) { ((value shr (24 - 8 * it)) and 0xFF).toByte() }
    }

    private fun parseIpv4Part(part: String): Long? {
        val hex = part.startsWith("0x") || part.startsWith("0X")
        val digits = if (hex) part.substring(2) else part
        if (digits.isEmpty() || !digits.all { it.isLetterOrDigit() }) return null
        val radix = when {
            hex -> 16
            digits.length > 1 && digits.startsWith("0") -> 8
            else -> 10
        }
        return digits.toLongOrNull(radix)
    }
}
