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
        for (target in listOf("http://192.168.1.1/", "http://localhost/", "http://[::1]/", "http://[fd00::1]/")) {
            asked.clear()
            answers["https://example.com/"] = redirect(target)
            answers[target] = PageAnswer(200, title = "Router")
            assertNull(title("https://example.com/"))
            assertEquals(listOf("https://example.com/"), asked)
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
        for (target in listOf("http://2130706433/", "http://0177.0.0.1/", "http://0x7f.0.0.1/", "http://127.1/")) {
            asked.clear()
            answers["https://example.com/"] = redirect(target)
            assertNull(target, title("https://example.com/"))
            assertEquals(target, listOf("https://example.com/"), asked)
        }
    }
}
