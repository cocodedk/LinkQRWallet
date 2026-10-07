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
        val name = host.trimEnd('.')
        if (name in blockedHosts || name.endsWith(".local")) {
            return UrlSafetyResult(false, UnsafeReason.Local)
        }
        if (name.endsWith(".onion")) {
            return UrlSafetyResult(false, UnsafeReason.Onion)
        }
        if (host.startsWith("[")) {
            return numericResult(parseIpv6Literal(host))
        }
        if (isNumeric(host)) {
            return numericResult(parseCanonicalIpv4(host))
        }
        if (host.startsWith("xn--")) {
            return UrlSafetyResult(false, UnsafeReason.EncodedName)
        }
        return UrlSafetyResult(true)
    }

    /** A numeric host that cannot be read exactly (bytes is null) is refused, as is one in a blocked range. */
    private fun numericResult(bytes: ByteArray?): UrlSafetyResult = when {
        bytes == null -> UrlSafetyResult(false, UnsafeReason.Invalid)
        AddressRules.isBlocked(bytes) -> UrlSafetyResult(false, UnsafeReason.Private)
        else -> UrlSafetyResult(true)
    }

    /** [host] is an IPv6 address in brackets, which only a valid numeric address can be, so nothing is looked up. */
    private fun parseIpv6Literal(host: String): ByteArray? = try {
        if (host.endsWith("]")) InetAddress.getByName(host.substring(1, host.length - 1)).address else null
    } catch (_: Exception) {
        null
    }

    private fun isDecimal(part: String) = part.isNotEmpty() && part.all { it in '0'..'9' }

    private fun isHex(part: String) =
        part.length > 2 && part.startsWith("0x") && part.drop(2).all { it in '0'..'9' || it in 'a'..'f' }

    /**
     * True when [host] is only digits and dots, or every dot-separated part is a number in some
     * spelling the system reads (decimal, octal such as 0177, or hex such as 0x7f). Those are
     * addresses, not names, and different programs read them differently.
     */
    private fun isNumeric(host: String): Boolean {
        if (host.all { it in '0'..'9' || it == '.' }) return true
        return host.removeSuffix(".").split(".").all { isDecimal(it) || isHex(it) }
    }

    /** Four decimal parts of 0 to 255 with no leading zeros and nothing else, such as 93.184.216.34. Anything else is null. */
    private fun parseCanonicalIpv4(host: String): ByteArray? {
        val parts = host.split(".")
        if (parts.size != 4) return null
        val values = parts.map { part ->
            if (!isDecimal(part) || part.length > 3 || (part.length > 1 && part.startsWith("0"))) return null
            part.toInt().takeIf { it <= 255 } ?: return null
        }
        return ByteArray(4) { values[it].toByte() }
    }
}
