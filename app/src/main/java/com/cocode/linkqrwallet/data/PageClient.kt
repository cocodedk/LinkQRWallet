package com.cocode.linkqrwallet.data

import java.io.ByteArrayInputStream
import java.net.Proxy
import java.util.concurrent.TimeUnit
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup

/** What a website answered: a redirect to [location], or a page with a [title]. */
data class PageAnswer(val status: Int, val location: String? = null, val title: String? = null)

/** Asks one address for its page once, without following redirects. Blocks, so call it off the main thread. */
fun interface PageClient {
    fun get(url: String): PageAnswer
}

/**
 * Connects with OkHttp. It never follows a redirect itself, goes to no proxy, and connects only
 * to addresses [dns] returned, so the addresses that were checked are the ones used.
 */
class OkHttpPageClient(dns: Dns = CheckedDns()) : PageClient {
    private val http = OkHttpClient.Builder()
        .dns(dns)
        .proxy(Proxy.NO_PROXY)
        .followRedirects(false)
        .followSslRedirects(false)
        .callTimeout(8, TimeUnit.SECONDS)
        .build()

    override fun get(url: String): PageAnswer {
        val request = Request.Builder().url(url).header("User-Agent", "LinkQRWallet/1.0").build()
        http.newCall(request).execute().use { response ->
            val status = response.code
            return if (status in 200..299) {
                PageAnswer(status, title = readTitle(response))
            } else {
                PageAnswer(status, location = response.header("Location"))
            }
        }
    }

    /** Reads at most [MAX_BODY_BYTES] of an HTML or XML page, enough to reach its title. */
    private fun readTitle(response: Response): String? {
        val body = response.body ?: return null
        val type = body.contentType()
        if (type != null && type.type != "text" && !type.subtype.contains("xml")) return null
        val source = body.source()
        source.request(MAX_BODY_BYTES)
        val bytes = source.buffer.readByteArray(minOf(source.buffer.size, MAX_BODY_BYTES))
        val charset = type?.charset()?.name()
        return Jsoup.parse(ByteArrayInputStream(bytes), charset, response.request.url.toString()).title()
    }

    private companion object {
        const val MAX_BODY_BYTES = 256L * 1024
    }
}
