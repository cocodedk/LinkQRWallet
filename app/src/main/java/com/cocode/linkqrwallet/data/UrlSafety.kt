package com.cocode.linkqrwallet.data

import java.net.URI

data class UrlSafetyResult(
    val isSafe: Boolean,
    val reason: String? = null
)

object UrlSafety {
    private val blockedSchemes = setOf("file", "javascript", "data", "content", "intent")
    private val blockedHosts = setOf("localhost")

    fun check(url: String): UrlSafetyResult {
        val uri = try {
            URI(url)
        } catch (_: Exception) {
            return UrlSafetyResult(false, "URL is not valid.")
        }
        val scheme = uri.scheme?.lowercase() ?: return UrlSafetyResult(false, "URL must include a scheme.")
        if (scheme in blockedSchemes) {
            return UrlSafetyResult(false, "Unsafe URL scheme blocked.")
        }
        if (scheme != "http" && scheme != "https") {
            return UrlSafetyResult(false, "Only http and https URLs are allowed.")
        }
        val host = uri.host?.lowercase() ?: return UrlSafetyResult(false, "URL must include a host.")
        if (host in blockedHosts || host.endsWith(".local")) {
            return UrlSafetyResult(false, "Local addresses are blocked.")
        }
        if (host.endsWith(".onion")) {
            return UrlSafetyResult(false, "Hidden service URLs are blocked.")
        }
        val ipv4 = parseIpv4(host)
        if (ipv4 != null && isPrivateOrReservedIpv4(ipv4)) {
            return UrlSafetyResult(false, "Private network addresses are blocked.")
        }
        if (host.startsWith("xn--")) {
            return UrlSafetyResult(false, "Suspicious internationalized domain blocked.")
        }
        return UrlSafetyResult(true)
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

    private fun isPrivateOrReservedIpv4(bytes: IntArray): Boolean {
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
