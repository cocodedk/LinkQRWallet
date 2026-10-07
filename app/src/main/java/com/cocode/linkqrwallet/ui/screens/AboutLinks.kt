package com.cocode.linkqrwallet.ui.screens

import com.cocode.linkqrwallet.BuildConfig

enum class AboutLink { Update, Website, Privacy, Source, Issues }

/** The facts that decide where the About page links point. */
data class AboutTargets(
    val applicationId: String,
    /** True once the app is live on F-Droid; until then the update button opens the GitHub release. */
    val fdroidLive: Boolean,
    /** The privacy policy page, or null while none is published (the page then leaves the link out). */
    val privacyUrl: String?
)

private const val SITE = "https://qr.cocode.dk"
private const val REPO = "https://github.com/cocodedk/LinkQRWallet"

/** The targets this build uses. Set [AboutTargets.fdroidLive] to true once the app is live on F-Droid. */
val appAboutTargets = AboutTargets(
    applicationId = BuildConfig.APPLICATION_ID,
    fdroidLive = false,
    privacyUrl = "https://qr.cocode.dk/privacy/"
)

/** Where [link] points, or null when it has no target (the privacy policy before it is published). */
fun aboutUrl(link: AboutLink, targets: AboutTargets): String? = when (link) {
    AboutLink.Update ->
        if (targets.fdroidLive) {
            "https://f-droid.org/packages/${targets.applicationId}/"
        } else {
            "$REPO/releases/latest"
        }
    AboutLink.Website -> SITE
    AboutLink.Privacy -> targets.privacyUrl
    AboutLink.Source -> REPO
    AboutLink.Issues -> "$REPO/issues"
}
