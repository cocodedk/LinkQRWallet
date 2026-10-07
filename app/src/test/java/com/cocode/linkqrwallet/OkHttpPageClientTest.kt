package com.cocode.linkqrwallet

import com.cocode.linkqrwallet.data.CheckedDns
import com.cocode.linkqrwallet.data.OkHttpPageClient
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicLong
import javax.net.SocketFactory
import okhttp3.Dns
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun aServerThatAsksForAnImmediateRetryIsAskedAtMostTwice() {
        repeat(3) { server.enqueue(MockResponse().setResponseCode(503).setHeader("Retry-After", "0")) }
        OkHttpPageClient(allowTestServer, isBlocked = { false }).get(url)
        assertTrue("${server.requestCount} requests", server.requestCount <= 2)
    }
}

/** Counts the bytes the client reads from its sockets. */
private class CountingSocketFactory : SocketFactory() {
    val bytesRead = AtomicLong()

    private inner class CountingSocket : Socket() {
        override fun getInputStream(): InputStream = object : FilterInputStream(super.getInputStream()) {
            override fun read(): Int = super.read().also { if (it >= 0) bytesRead.incrementAndGet() }

            override fun read(b: ByteArray, off: Int, len: Int): Int =
                super.read(b, off, len).also { if (it > 0) bytesRead.addAndGet(it.toLong()) }
        }
    }

    override fun createSocket(): Socket = CountingSocket()

    override fun createSocket(host: String, port: Int): Socket =
        CountingSocket().apply { connect(InetSocketAddress(host, port)) }

    override fun createSocket(host: String, port: Int, local: InetAddress, localPort: Int): Socket =
        CountingSocket().apply { bind(InetSocketAddress(local, localPort)); connect(InetSocketAddress(host, port)) }

    override fun createSocket(host: InetAddress, port: Int): Socket =
        CountingSocket().apply { connect(InetSocketAddress(host, port)) }

    override fun createSocket(address: InetAddress, port: Int, local: InetAddress, localPort: Int): Socket =
        CountingSocket().apply { bind(InetSocketAddress(local, localPort)); connect(InetSocketAddress(address, port)) }
}
