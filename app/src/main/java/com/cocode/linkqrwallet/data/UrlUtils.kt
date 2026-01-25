package com.cocode.linkqrwallet.data

import java.net.URI

object UrlUtils {
    fun normalizeUrl(raw: String): String? {
        var trimmed = raw.trim()
        if (trimmed.isBlank()) return null
        if (!trimmed.contains("://")) {
            trimmed = "https://$trimmed"
        }
        return try {
            val uri = URI(trimmed)
            if (uri.scheme.isNullOrBlank() || uri.host.isNullOrBlank()) {
                null
            } else {
                uri.toString()
            }
        } catch (_: Exception) {
            null
        }
    }

    fun domainFromUrl(url: String): String {
        return try {
            val host = URI(url).host ?: url
            host.removePrefix("www.")
        } catch (_: Exception) {
            url
        }
    }
}
