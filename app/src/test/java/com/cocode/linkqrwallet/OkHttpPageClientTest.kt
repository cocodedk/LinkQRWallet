package com.cocode.linkqrwallet

import com.cocode.linkqrwallet.data.CheckedDns
import com.cocode.linkqrwallet.data.OkHttpPageClient
import java.io.IOException
import java.net.InetAddress
import okhttp3.Dns
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

/** Runs the real OkHttp client against a local test server that stands in for a website. */
class OkHttpPageClientTest {
    private val server = MockWebServer()
    private val loopback = InetAddress.getByName("127.0.0.1")
    private val allowTestServer = object : Dns {
        override fun lookup(hostname: String) = listOf(loopback)
    }
    private lateinit var url: String

    @Before
    fun start() {
        server.start(loopback, 0)
        url = "http://example.test:${server.port}/"
    }

    @After
    fun stop() {
        server.shutdown()
    }

    private fun page(body: String, type: String = "text/html; charset=utf-8") =
        MockResponse().setHeader("Content-Type", type).setBody(body)

    @Test
    fun readsTheTitleAndIdentifiesTheApp() {
        server.enqueue(page("<html><head><title> Hello </title></head></html>"))
        val answer = OkHttpPageClient(allowTestServer).get(url)
        assertEquals("Hello", answer.title)
        assertEquals("LinkQRWallet/1.0", server.takeRequest().getHeader("User-Agent"))
    }

    @Test
    fun neverFollowsARedirectItself() {
        server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", "${url}other"))
        server.enqueue(page("<title>Other</title>"))
        val answer = OkHttpPageClient(allowTestServer).get(url)
        assertEquals(302, answer.status)
        assertEquals("${url}other", answer.location)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun readsOnlyTheStartOfALargePage() {
        val padding = "x".repeat(300_000)
        server.enqueue(page("<html><head><!--$padding--><title>Too late</title></head></html>"))
        assertEquals("", OkHttpPageClient(allowTestServer).get(url).title)
        server.enqueue(page("<html><head><title>Early</title></head><body>$padding</body></html>"))
        assertEquals("Early", OkHttpPageClient(allowTestServer).get(url).title)
    }

    @Test
    fun aPageThatIsNotTextGivesNoTitle() {
        server.enqueue(page("<title>Not a page</title>", "image/png"))
        assertNull(OkHttpPageClient(allowTestServer).get(url).title)
    }

    @Test
    fun anErrorAnswerGivesNoTitle() {
        server.enqueue(page("<title>Oops</title>").setResponseCode(500))
        val answer = OkHttpPageClient(allowTestServer).get(url)
        assertEquals(500, answer.status)
        assertNull(answer.title)
    }

    @Test
    fun neverConnectsWhenTheNameLeadsToALocalAddress() {
        server.enqueue(page("<title>Secret</title>"))
        val client = OkHttpPageClient(CheckedDns { listOf(loopback) })
        assertThrows(IOException::class.java) { client.get(url) }
        assertEquals(0, server.requestCount)
    }
}
