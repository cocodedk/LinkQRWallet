package com.cocode.linkqrwallet.data

import java.net.InetAddress
import java.net.URI
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Finds which addresses a website name leads to. Blocks, so call it off the main thread. */
fun interface HostResolver {
    fun resolve(host: String): List<InetAddress>
}

private val systemResolver = HostResolver { InetAddress.getAllByName(it).toList() }

/**
 * Reads a page's title. Before it connects to an address, and again for every redirect, it checks
 * the link with [UrlSafety] and checks where the website name leads. If any of them points to the
 * phone or the local network, it stops and gives no title.
 */
open class TitleFetcher(
    private val resolver: HostResolver = systemResolver,
    private val client: PageClient = JsoupPageClient
) {
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
            if (!mayConnectTo(current)) return null
            val answer = client.get(current)
            if (answer.status in 200..299) {
                return answer.title?.trim()?.takeIf { it.isNotBlank() }
            }
            if (answer.status !in 300..399) return null
            current = URI(current).resolve(answer.location ?: return null).toString()
        }
        return null
    }

    private fun mayConnectTo(url: String): Boolean {
        if (!UrlSafety.check(url).isSafe) return false
        val host = URI(url).host?.removeSurrounding("[", "]") ?: return false
        val addresses = resolver.resolve(host)
        return addresses.isNotEmpty() && addresses.none { AddressRules.isBlocked(it) }
    }

    private companion object {
        const val MAX_REDIRECTS = 5
    }
}
