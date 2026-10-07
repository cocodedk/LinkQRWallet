package com.cocode.linkqrwallet.data

import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.InetAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit
import javax.net.SocketFactory
import okhttp3.Dns
import okhttp3.Interceptor
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
 * Connects with OkHttp. It never follows a redirect itself, retries nothing, goes to no proxy, and
 * connects only to addresses [dns] returned. As a second check, a network interceptor looks at the
 * address the socket actually connected to and refuses it with [isBlocked] before any request is
 * written, which also covers a host written as a number, which OkHttp never passes to [dns].
 */
class OkHttpPageClient(
    dns: Dns = CheckedDns(),
    isBlocked: (InetAddress) -> Boolean = AddressRules::isBlocked,
    socketFactory: SocketFactory = SocketFactory.getDefault()
) : PageClient {
    private val http = OkHttpClient.Builder()
        .dns(dns)
        .socketFactory(socketFactory)
        .proxy(Proxy.NO_PROXY)
        .followRedirects(false)
        .followSslRedirects(false)
        .retryOnConnectionFailure(false)
        .callTimeout(8, TimeUnit.SECONDS)
        .addNetworkInterceptor(Interceptor { chain ->
            val address = chain.connection()?.route()?.socketAddress?.address
            if (address == null || isBlocked(address)) {
                throw IOException("The app does not send a request to this address")
            }
            chain.proceed(chain.request())
        })
        .build()

    override fun get(url: String): PageAnswer {
        val request = Request.Builder().url(url).header("User-Agent", "LinkQRWallet/1.0").build()
        val call = http.newCall(request)
        call.execute().use { response ->
            val status = response.code
            val answer = if (status in 200..299) {
                PageAnswer(status, title = readTitle(response))
            } else {
                PageAnswer(status, location = response.header("Location"))
            }
            // Closing a response would otherwise read the rest of the body first.
            call.cancel()
            return answer
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
