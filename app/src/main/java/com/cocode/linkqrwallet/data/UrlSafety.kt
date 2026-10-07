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
        val host = uri.host?.lowercase() ?: return UrlSafetyResult(false, UnsafeReason.NoHost)
        if (host in blockedHosts || host.endsWith(".local")) {
            return UrlSafetyResult(false, UnsafeReason.Local)
        }
        if (host.endsWith(".onion")) {
            return UrlSafetyResult(false, UnsafeReason.Onion)
        }
        val ipv4 = parseIpv4(host)
        if (ipv4 != null && AddressRules.isBlockedIpv4(ipv4)) {
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

    private fun parseIpv4(host: String): IntArray? {
        val parts = host.split(".")
        if (parts.size != 4) return null
        val bytes = IntArray(4)
        for (i in 0..3) {
            val part = parts[i]
            if (part.isEmpty() || part.length > 3) return null
            val value = part.toIntOrNull() ?: return null
            if (value < 0 || value > 255) return null
            bytes[i] = value
        }
        return bytes
    }
}
