package com.cocode.linkqrwallet.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Reads a page's title. It follows at most 5 redirects itself, resolving each one the way OkHttp
 * does, checking the address of the page and of every redirect with [UrlSafety], and never
 * following a redirect from https to http. The client checks where each name leads before it
 * connects. If anything points to the phone or the local network, it stops and gives no title.
 */
open class TitleFetcher(private val client: PageClient = OkHttpPageClient()) {
    open suspend fun fetchTitle(url: String): String? = withContext(Dispatchers.IO) {
        try {
            load(url)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    private fun load(start: String): String? {
        var current = start
        repeat(MAX_REDIRECTS + 1) {
            if (!UrlSafety.check(current).isSafe) return null
            val answer = client.get(current)
            if (answer.status in 200..299) {
                return answer.title?.trim()?.takeIf { it.isNotBlank() }
            }
            if (answer.status !in 300..399) return null
            val next = current.toHttpUrlOrNull()?.resolve(answer.location ?: return null) ?: return null
            if (current.startsWith("https:", ignoreCase = true) && !next.isHttps) return null
            current = next.toString()
        }
        return null
    }

    private companion object {
        const val MAX_REDIRECTS = 5
    }
}
