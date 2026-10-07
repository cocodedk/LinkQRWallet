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

/** Carries the answer out of the network interceptor, so OkHttp neither follows up on it nor reads its body. */
private class AnswerReady(val answer: PageAnswer) : IOException("The answer was read from the headers")

/**
 * Connects with OkHttp. It never follows a redirect itself, retries nothing, goes to no proxy, and
 * connects only to addresses [dns] returned. A network interceptor looks at the address the socket
 * actually connected to and refuses it with [isBlocked] before any request is written, which also
 * covers a host written as a number, which OkHttp never passes to [dns]. For any answer outside
 * 200 to 299 it takes the status and Location from the headers and cancels the call, so OkHttp
 * neither retries it nor reads its body. For a page it reads at most [MAX_BODY_BYTES] of the body.
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
            val response = chain.proceed(chain.request())
            if (response.code !in 200..299) {
                val answer = PageAnswer(response.code, location = response.header("Location"))
                chain.call().cancel()
                response.close()
                throw AnswerReady(answer)
            }
            response
        })
        .build()

    override fun get(url: String): PageAnswer {
        val request = Request.Builder().url(url).header("User-Agent", "LinkQRWallet/1.0").build()
        val call = http.newCall(request)
        try {
            call.execute().use { response ->
                val answer = PageAnswer(response.code, title = readTitle(response))
                // Closing a response would otherwise read the rest of the body first.
                call.cancel()
                return answer
            }
        } catch (ready: AnswerReady) {
            return ready.answer
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
