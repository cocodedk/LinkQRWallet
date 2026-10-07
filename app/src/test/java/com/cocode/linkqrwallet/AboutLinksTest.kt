package com.cocode.linkqrwallet

import com.cocode.linkqrwallet.ui.screens.AboutLink
import com.cocode.linkqrwallet.ui.screens.AboutTargets
import com.cocode.linkqrwallet.ui.screens.aboutUrl
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

    @Test
    fun websiteSourceAndIssuesDoNotDependOnTheTargets() {
        for (targets in listOf(notOnFdroid, onFdroid)) {
            assertEquals("https://qr.cocode.dk", aboutUrl(AboutLink.Website, targets))
            assertEquals("https://github.com/cocodedk/LinkQRWallet", aboutUrl(AboutLink.Source, targets))
            assertEquals("https://github.com/cocodedk/LinkQRWallet/issues", aboutUrl(AboutLink.Issues, targets))
        }
    }
}
