package com.cocode.linkqrwallet

import com.cocode.linkqrwallet.data.CheckedDns
import com.cocode.linkqrwallet.data.OkHttpPageClient
import com.cocode.linkqrwallet.data.TitleFetcher
import java.io.IOException
import java.net.InetAddress
import okhttp3.Dns
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
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
        val answer = OkHttpPageClient(allowTestServer, isBlocked = { false }).get(url)
        assertEquals("Hello", answer.title)
        assertEquals("LinkQRWallet/1.0", server.takeRequest().getHeader("User-Agent"))
    }

    @Test
    fun neverFollowsARedirectItself() {
        server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", "${url}other"))
        server.enqueue(page("<title>Other</title>"))
        val answer = OkHttpPageClient(allowTestServer, isBlocked = { false }).get(url)
        assertEquals(302, answer.status)
        assertEquals("${url}other", answer.location)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun readsOnlyTheStartOfALargePage() {
        val padding = "x".repeat(300_000)
        server.enqueue(page("<html><head><!--$padding--><title>Too late</title></head></html>"))
        assertEquals("", OkHttpPageClient(allowTestServer, isBlocked = { false }).get(url).title)
        server.enqueue(page("<html><head><title>Early</title></head><body>$padding</body></html>"))
        assertEquals("Early", OkHttpPageClient(allowTestServer, isBlocked = { false }).get(url).title)
    }

    @Test
    fun aPageThatIsNotTextGivesNoTitle() {
        server.enqueue(page("<title>Not a page</title>", "image/png"))
        assertNull(OkHttpPageClient(allowTestServer, isBlocked = { false }).get(url).title)
    }

    @Test
    fun anErrorAnswerGivesNoTitle() {
        server.enqueue(page("<title>Oops</title>").setResponseCode(500))
        val answer = OkHttpPageClient(allowTestServer, isBlocked = { false }).get(url)
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

    @Test
    fun refusesALocalAddressThatSlippedPastTheDnsBeforeAnyRequestIsWritten() {
        server.enqueue(page("<title>Secret</title>"))
        // the Dns lets the address through, as OkHttp does for a host written as a number
        assertThrows(IOException::class.java) { OkHttpPageClient(allowTestServer).get(url) }
        assertEquals(0, server.requestCount)
    }

    @Test
    fun neverSendsARequestToANumericHostThatIsLocal() {
        for (host in listOf("127.0.0.1", "0127.0.0.1", "2130706433", "127.1", "0x7f.0.0.1", "0177.0.0.1")) {
            server.enqueue(page("<title>Secret</title>"))
            assertThrows(host, Exception::class.java) { OkHttpPageClient().get("http://$host:${server.port}/") }
            assertEquals(host, 0, server.requestCount)
        }
    }

    @Test
    fun stopsReadingOnceTheTitleIsInsteadOfDrainingTheBody() {
        server.enqueue(page("<html><head><title>Big</title></head><body>" + "x".repeat(3_000_000) + "</body></html>"))
        val sockets = CountingSocketFactory()
        val answer = OkHttpPageClient(allowTestServer, isBlocked = { false }, socketFactory = sockets).get(url)
        assertEquals("Big", answer.title)
        assertTrue("read ${sockets.bytesRead.get()} bytes", sockets.bytesRead.get() < 1_000_000)
    }

    @Test
    fun doesNotRetryARequestTheServerAnswersWith408() {
        server.enqueue(MockResponse().setResponseCode(408))
        server.enqueue(page("<title>Second try</title>"))
        val answer = OkHttpPageClient(allowTestServer, isBlocked = { false }).get(url)
        assertEquals(408, answer.status)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun readsNoMoreThanTheHeadersOfAnythingThatIsNotAPage() {
        val big = "x".repeat(3_000_000)
        val answers = listOf(
            MockResponse().setResponseCode(503).setHeader("Retry-After", "0").setBody(big),
            MockResponse().setResponseCode(302).setHeader("Location", "${url}next").setBody(big),
            MockResponse().setResponseCode(404).setBody(big),
            MockResponse().setResponseCode(408).setBody(big)
        )
        for (response in answers) {
            val other = MockWebServer()
            other.start(loopback, 0)
            try {
                other.enqueue(response)
                other.enqueue(page("<title>Second try</title>"))
                val sockets = CountingSocketFactory()
                val client = OkHttpPageClient(allowTestServer, isBlocked = { false }, socketFactory = sockets)
                val answer = client.get("http://example.test:${other.port}/")
                assertNull(answer.title)
                assertEquals("one request, no retry", 1, other.requestCount)
                assertTrue("status ${answer.status}: read ${sockets.bytesRead.get()} bytes", sockets.bytesRead.get() < 200_000)
            } finally {
                other.shutdown()
            }
        }
    }

    @Test
    fun readsTheStatusAndLocationOfARedirectWithoutItsBody() {
        server.enqueue(MockResponse().setResponseCode(301).setHeader("Location", "${url}next").setBody("x".repeat(500_000)))
        val answer = OkHttpPageClient(allowTestServer, isBlocked = { false }).get(url)
        assertEquals(301, answer.status)
        assertEquals("${url}next", answer.location)
    }

    @Test
    fun aRedirectToAHostWhoseNameLeadsToALocalAddressIsNeverConnectedTo() {
        // a.test may be reached (it stands for the test server), b.test leads to the same local address and may not
        val refusing = CheckedDns { listOf(loopback) }
        val dns = object : Dns {
            override fun lookup(hostname: String) = if (hostname == "a.test") listOf(loopback) else refusing.lookup(hostname)
        }
        server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", "http://b.test:${server.port}/page"))
        server.enqueue(page("<title>Secret</title>"))
        val fetcher = TitleFetcher(OkHttpPageClient(dns, isBlocked = { false }))
        assertNull(runBlocking { fetcher.fetchTitle("http://a.test:${server.port}/") })
        assertEquals(1, server.requestCount)
    }
}
