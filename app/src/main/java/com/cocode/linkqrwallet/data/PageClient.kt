package com.cocode.linkqrwallet.data

import org.jsoup.Jsoup

/** What a website answered: a redirect to [location], or a page with a [title]. */
data class PageAnswer(val status: Int, val location: String? = null, val title: String? = null)

/** Asks one address for its page once, without following redirects. Blocks, so call it off the main thread. */
fun interface PageClient {
    fun get(url: String): PageAnswer
}

object JsoupPageClient : PageClient {
    override fun get(url: String): PageAnswer {
        val response = Jsoup.connect(url)
            .userAgent("LinkQRWallet/1.0")
            .timeout(8000)
            .followRedirects(false)
            .execute()
        val status = response.statusCode()
        return if (status in 200..299) {
            PageAnswer(status, title = response.parse().title())
        } else {
            PageAnswer(status, location = response.header("Location"))
        }
    }
}
