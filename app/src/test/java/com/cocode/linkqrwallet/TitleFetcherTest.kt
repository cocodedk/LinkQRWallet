package com.cocode.linkqrwallet

import com.cocode.linkqrwallet.data.PageAnswer
import com.cocode.linkqrwallet.data.PageClient
import com.cocode.linkqrwallet.data.TitleFetcher
import java.io.IOException
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

    private val fetcher = TitleFetcher { url ->
        asked.add(url)
        onAsk(url)
    }

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
        // same scheme as the start page, so only the check of the destination can refuse it
        for (scheme in listOf("https", "http")) {
            for (host in listOf("192.168.1.1", "localhost", "[::1]", "[fd00::1]", "[64:ff9b:1::1]", "100.64.0.1")) {
                val target = "$scheme://$host/"
                asked.clear()
                answers["$scheme://example.com/"] = redirect(target)
                answers[target] = PageAnswer(200, title = "Router")
                assertNull(target, title("$scheme://example.com/"))
                assertEquals(target, listOf("$scheme://example.com/"), asked)
            }
        }
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

    @Test
    fun aRefusedConnectionGivesNothing() {
        onAsk = { throw IOException("leads to an address the app refuses to contact") }
        assertNull(title("https://sneaky.example.com/"))
        assertEquals(listOf("https://sneaky.example.com/"), asked)
    }

    @Test
    fun oddSpellingsOfLocalAddressesInARedirectAreNotFollowed() {
        for (host in listOf("2130706433", "0177.0.0.1", "0127.0.0.1", "0x7f.0.0.1", "127.1")) {
            val target = "https://$host/"
            asked.clear()
            answers["https://example.com/"] = redirect(target)
            answers[target] = PageAnswer(200, title = "Local")
            assertNull(target, title("https://example.com/"))
            assertEquals(target, listOf("https://example.com/"), asked)
        }
    }

    @Test
    fun aRelativeRedirectIsResolvedTheWayBrowsersDo() {
        answers["https://example.com/dir/page?old=1"] = redirect("?next=2")
        answers["https://example.com/dir/page?next=2"] = redirect("../up")
        answers["https://example.com/up"] = PageAnswer(200, title = "Up")
        assertEquals("Up", title("https://example.com/dir/page?old=1"))
        assertEquals(
            listOf(
                "https://example.com/dir/page?old=1",
                "https://example.com/dir/page?next=2",
                "https://example.com/up"
            ),
            asked
        )
    }

    @Test
    fun aRedirectFromHttpsToHttpIsNotFollowed() {
        answers["https://example.com/"] = redirect("http://example.com/plain")
        answers["http://example.com/plain"] = PageAnswer(200, title = "Plain")
        assertNull(title("https://example.com/"))
        assertEquals(listOf("https://example.com/"), asked)
    }
}
