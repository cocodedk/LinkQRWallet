package com.cocode.linkqrwallet.data

import java.net.URI
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads a page's title. It follows at most 5 redirects itself, checking the address of the page
 * and of every redirect with [UrlSafety]. The client checks where each name leads before it
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
            current = URI(current).resolve(answer.location ?: return null).toString()
        }
        return null
    }

    private companion object {
        const val MAX_REDIRECTS = 5
    }
}
