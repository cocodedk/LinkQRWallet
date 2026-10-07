package com.cocode.linkqrwallet

import com.cocode.linkqrwallet.data.HostResolver
import com.cocode.linkqrwallet.data.PageAnswer
import com.cocode.linkqrwallet.data.PageClient
import com.cocode.linkqrwallet.data.TitleFetcher
import java.net.InetAddress
import java.net.UnknownHostException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The title lookup must not reach the phone or the local network, not even through a redirect. */
class TitleFetcherTest {
    private val asked = mutableListOf<String>()
    private val answers = mutableMapOf<String, PageAnswer>()
    private var onAsk: (String) -> PageAnswer = { answers[it] ?: PageAnswer(404) }
    private val leads = mutableMapOf<String, List<String>>()

    private val fetcher = TitleFetcher(
        resolver = { host ->
            val ips = leads[host] ?: listOf("93.184.216.34")
            ips.map { InetAddress.getByName(it) }
        },
        client = { url ->
            asked.add(url)
            onAsk(url)
        }
    )

    private fun title(url: String) = runBlocking { fetcher.fetchTitle(url) }

    private fun redirect(to: String) = PageAnswer(302, location = to)

    @Test
    fun anOrdinaryPageGivesItsTitle() {
        answers["https://example.com/"] = PageAnswer(200, title = "  Example Domain ")
        assertEquals("Example Domain", title("https://example.com/"))
    }

    @Test
    fun aMissingOrBlankTitleGivesNothing() {
        answers["https://example.com/"] = PageAnswer(200, title = "   ")
        assertNull(title("https://example.com/"))
        assertNull(title("https://example.com/missing"))
    }

    @Test
    fun aRedirectToAPublicPageIsFollowed() {
        answers["https://example.com/"] = redirect("/next")
        answers["https://example.com/next"] = redirect("https://www.example.org/final")
        answers["https://www.example.org/final"] = PageAnswer(200, title = "Final")
        assertEquals("Final", title("https://example.com/"))
        assertEquals(
            listOf("https://example.com/", "https://example.com/next", "https://www.example.org/final"),
            asked
        )
    }

    @Test
    fun aRedirectToAPrivateAddressIsNotFollowed() {
        for (target in listOf("http://192.168.1.1/", "http://localhost/", "http://[::1]/", "http://[fd00::1]/")) {
            asked.clear()
            answers["https://example.com/"] = redirect(target)
            answers[target] = PageAnswer(200, title = "Router")
            assertNull(title("https://example.com/"))
            assertEquals(listOf("https://example.com/"), asked)
        }
    }

    @Test
    fun aRedirectToANameThatLeadsToAPrivateAddressIsNotFollowed() {
        leads["intranet.example.net"] = listOf("10.0.0.5")
        answers["https://example.com/"] = redirect("https://intranet.example.net/")
        answers["https://intranet.example.net/"] = PageAnswer(200, title = "Intranet")
        assertNull(title("https://example.com/"))
        assertEquals(listOf("https://example.com/"), asked)
    }

    @Test
    fun aRedirectChainIsStoppedAfterFiveRedirects() {
        onAsk = { redirect("${it}x") }
        assertNull(title("https://example.com/"))
        assertEquals(6, asked.size)
    }

    @Test
    fun fiveRedirectsAreStillFollowed() {
        onAsk = { url ->
            if (url.length < "https://example.com/xxxxx".length) redirect("${url}x") else PageAnswer(200, title = "Done")
        }
        assertEquals("Done", title("https://example.com/"))
        assertEquals(6, asked.size)
    }

    @Test
    fun aNameThatLeadsToAPrivateAddressIsNeverContacted() {
        val unsafe = listOf(
            "127.0.0.1", "10.1.2.3", "172.16.0.1", "192.168.0.9", "169.254.1.1", "0.0.0.0",
            "::1", "fd00::1", "fc00::1", "fe80::1", "ff02::1"
        )
        for (ip in unsafe) {
            leads["sneaky.example.com"] = listOf(ip)
            assertNull(ip, title("https://sneaky.example.com/"))
        }
        assertTrue(asked.isEmpty())
    }

    @Test
    fun oneBadAnswerAmongGoodOnesRefusesTheName() {
        leads["mixed.example.com"] = listOf("93.184.216.34", "127.0.0.1")
        assertNull(title("https://mixed.example.com/"))
        assertTrue(asked.isEmpty())
    }

    @Test
    fun aNameThatDoesNotResolveIsNeverContacted() {
        val failing = TitleFetcher(
            resolver = HostResolver { throw UnknownHostException(it) },
            client = PageClient { asked.add(it); PageAnswer(200, title = "No") }
        )
        assertNull(runBlocking { failing.fetchTitle("https://nowhere.example.com/") })
        assertTrue(asked.isEmpty())
    }

    @Test
    fun aRefusedAddressIsNeverContacted() {
        assertNull(title("http://192.168.1.10/admin"))
        assertNull(title("http://[::1]/"))
        assertTrue(asked.isEmpty())
    }

    @Test
    fun anErrorOrANonPageAnswerGivesNothing() {
        answers["https://example.com/"] = PageAnswer(500, title = "Oops")
        assertNull(title("https://example.com/"))
        answers["https://example.com/"] = PageAnswer(302)
        assertNull(title("https://example.com/"))
    }
}
