package com.cocode.linkqrwallet

import com.cocode.linkqrwallet.ui.screens.AboutLink
import com.cocode.linkqrwallet.ui.screens.AboutTargets
import com.cocode.linkqrwallet.ui.screens.aboutUrl
import com.cocode.linkqrwallet.ui.screens.appAboutTargets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AboutLinksTest {
    private val id = "com.cocode.linkqrwallet"
    private val notOnFdroid = AboutTargets(id, fdroidLive = false, privacyPublished = false)
    private val onFdroid = AboutTargets(id, fdroidLive = true, privacyPublished = true)

    @Test
    fun updateOpensTheGithubReleaseUntilTheAppIsOnFdroid() {
        assertEquals(
            "https://github.com/cocodedk/LinkQRWallet/releases/latest",
            aboutUrl(AboutLink.Update, notOnFdroid, "en")
        )
    }

    @Test
    fun updateOpensTheFdroidPageOnceTheAppIsLive() {
        assertEquals(
            "https://f-droid.org/packages/com.cocode.linkqrwallet/",
            aboutUrl(AboutLink.Update, onFdroid, "en")
        )
    }

    @Test
    fun privacyIsAbsentWhileNoPolicyIsPublished() {
        for (language in listOf("en", "da")) {
            assertNull(aboutUrl(AboutLink.Privacy, notOnFdroid, language))
        }
    }

    @Test
    fun englishOpensTheEnglishPages() {
        assertEquals("https://qr.cocode.dk", aboutUrl(AboutLink.Website, onFdroid, "en"))
        assertEquals("https://qr.cocode.dk/privacy/", aboutUrl(AboutLink.Privacy, onFdroid, "en"))
    }

    @Test
    fun danishOpensTheDanishPages() {
        assertEquals("https://qr.cocode.dk/da/", aboutUrl(AboutLink.Website, onFdroid, "da"))
        assertEquals("https://qr.cocode.dk/da/privacy/", aboutUrl(AboutLink.Privacy, onFdroid, "da"))
    }

    @Test
    fun aLanguageTheSiteLacksOpensTheEnglishPages() {
        for (language in listOf("fa", "de")) {
            assertEquals("https://qr.cocode.dk", aboutUrl(AboutLink.Website, onFdroid, language))
            assertEquals("https://qr.cocode.dk/privacy/", aboutUrl(AboutLink.Privacy, onFdroid, language))
        }
    }

    private fun everyCase(link: AboutLink) = listOf(notOnFdroid, onFdroid).flatMap { targets ->
        listOf("en", "da", "fa").map { aboutUrl(link, targets, it) }
    }

    @Test
    fun sourceDoesNotDependOnTheTargetsOrTheLanguage() {
        assertEquals(List(6) { "https://github.com/cocodedk/LinkQRWallet" }, everyCase(AboutLink.Source))
    }

    @Test
    fun issuesDoNotDependOnTheTargetsOrTheLanguage() {
        assertEquals(List(6) { "https://github.com/cocodedk/LinkQRWallet/issues" }, everyCase(AboutLink.Issues))
    }

    @Test
    fun thisBuildLinksThePublishedPrivacyPolicy() {
        assertEquals("https://qr.cocode.dk/privacy/", aboutUrl(AboutLink.Privacy, appAboutTargets, "en"))
        assertEquals("https://qr.cocode.dk/da/privacy/", aboutUrl(AboutLink.Privacy, appAboutTargets, "da"))
    }
}
