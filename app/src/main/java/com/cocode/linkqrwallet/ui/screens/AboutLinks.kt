package com.cocode.linkqrwallet.ui.screens

import com.cocode.linkqrwallet.BuildConfig

enum class AboutLink { Update, Website, Privacy, Source, Issues }

/** The facts that decide where the About page links point. */
data class AboutTargets(
    val applicationId: String,
    /** True once the app is live on F-Droid; until then the update button opens the GitHub release. */
    val fdroidLive: Boolean,
    /** False while no privacy policy page is published (the About page then leaves the link out). */
    val privacyPublished: Boolean
)

private const val SITE = "https://qr.cocode.dk"
private const val REPO = "https://github.com/cocodedk/LinkQRWallet"

/**
 * Languages the site has both a home page and a privacy page for, at `<site>/<code>/` and
 * `<site>/<code>/privacy/`. Any other language opens the English pages.
 */
private val SITE_LANGUAGES = setOf("da")

private fun sitePage(language: String, path: String = ""): String = when {
    language in SITE_LANGUAGES -> "$SITE/$language/$path"
    path.isEmpty() -> SITE
    else -> "$SITE/$path"
}

/** The targets this build uses. Set [AboutTargets.fdroidLive] to true once the app is live on F-Droid. */
val appAboutTargets = AboutTargets(
    applicationId = BuildConfig.APPLICATION_ID,
    fdroidLive = false,
    privacyPublished = true
)

/**
 * Where [link] points, or null when it has no target (the privacy policy before it is published).
 * The website and privacy links follow [language] (a code such as "da" from the app's current
 * locale) and open the English pages when the site has none in that language.
 */
fun aboutUrl(link: AboutLink, targets: AboutTargets, language: String): String? = when (link) {
    AboutLink.Update ->
        if (targets.fdroidLive) {
            "https://f-droid.org/packages/${targets.applicationId}/"
        } else {
            "$REPO/releases/latest"
        }
    AboutLink.Website -> sitePage(language)
    AboutLink.Privacy -> if (targets.privacyPublished) sitePage(language, "privacy/") else null
    AboutLink.Source -> REPO
    AboutLink.Issues -> "$REPO/issues"
}
