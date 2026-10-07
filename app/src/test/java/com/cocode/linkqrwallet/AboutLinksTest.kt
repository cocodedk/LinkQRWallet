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
    private val notOnFdroid = AboutTargets(id, fdroidLive = false, privacyUrl = null)
    private val onFdroid = AboutTargets(id, fdroidLive = true, privacyUrl = "https://qr.cocode.dk/privacy/")

    @Test
    fun updateOpensTheGithubReleaseUntilTheAppIsOnFdroid() {
        assertEquals(
            "https://github.com/cocodedk/LinkQRWallet/releases/latest",
            aboutUrl(AboutLink.Update, notOnFdroid)
        )
    }

    @Test
    fun updateOpensTheFdroidPageOnceTheAppIsLive() {
        assertEquals(
            "https://f-droid.org/packages/com.cocode.linkqrwallet/",
            aboutUrl(AboutLink.Update, onFdroid)
        )
    }

    @Test
    fun privacyIsAbsentWhileNoPolicyIsPublished() {
        assertNull(aboutUrl(AboutLink.Privacy, notOnFdroid))
    }

    @Test
    fun privacyOpensThePolicyOnceItIsPublished() {
        assertEquals("https://qr.cocode.dk/privacy/", aboutUrl(AboutLink.Privacy, onFdroid))
    }

    private fun bothTargets(link: AboutLink) =
        listOf(aboutUrl(link, notOnFdroid), aboutUrl(link, onFdroid))

    @Test
    fun websiteDoesNotDependOnTheTargets() {
        assertEquals(List(2) { "https://qr.cocode.dk" }, bothTargets(AboutLink.Website))
    }

    @Test
    fun sourceDoesNotDependOnTheTargets() {
        assertEquals(List(2) { "https://github.com/cocodedk/LinkQRWallet" }, bothTargets(AboutLink.Source))
    }

    @Test
    fun issuesDoNotDependOnTheTargets() {
        assertEquals(List(2) { "https://github.com/cocodedk/LinkQRWallet/issues" }, bothTargets(AboutLink.Issues))
    }

    @Test
    fun thisBuildLinksThePublishedPrivacyPolicy() {
        assertEquals("https://qr.cocode.dk/privacy/", aboutUrl(AboutLink.Privacy, appAboutTargets))
    }
}
