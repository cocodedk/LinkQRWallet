package com.cocode.linkqrwallet.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.cocode.linkqrwallet.BuildConfig
import com.cocode.linkqrwallet.R

@Composable
private fun SectionTitle(@StringRes title: Int) = Text(
    text = stringResource(title),
    style = MaterialTheme.typography.titleLarge,
    modifier = Modifier.padding(top = 12.dp).semantics { heading() }
)

@Composable
private fun Body(@StringRes text: Int) =
    Text(text = stringResource(text), style = MaterialTheme.typography.bodyLarge)

@Composable
private fun LinkButton(@StringRes label: Int, onClick: () -> Unit) =
    Button(onClick = onClick) { Text(stringResource(label)) }

private fun openLink(context: Context, url: String): Boolean = try {
    context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    true
} catch (_: ActivityNotFoundException) {
    false
}

/** The About page: the sections of the cocode-apps standard, in its order. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit, targets: AboutTargets = appAboutTargets) {
    val context = LocalContext.current
    var noBrowser by rememberSaveable { mutableStateOf(false) }
    val open = { link: AboutLink ->
        val url = aboutUrl(link, targets)
        noBrowser = url != null && !openLink(context, url)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text(stringResource(R.string.action_back)) }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Name and version, with the update button.
            SectionTitle(R.string.app_name)
            Text(
                text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                style = MaterialTheme.typography.bodyLarge
            )
            LinkButton(R.string.about_check_updates) { open(AboutLink.Update) }
            Body(R.string.about_update_note)

            // 2. What the app does.
            SectionTitle(R.string.about_what_title)
            Body(R.string.about_what_body)

            // 3. Privacy.
            SectionTitle(R.string.about_privacy_title)
            Body(R.string.about_privacy_local)
            Body(R.string.about_privacy_camera)
            Body(R.string.about_privacy_internet)
            Body(R.string.about_privacy_backup)
            if (aboutUrl(AboutLink.Privacy, targets) != null) {
                LinkButton(R.string.about_privacy_link) { open(AboutLink.Privacy) }
            }

            // 4. Links.
            SectionTitle(R.string.about_links_title)
            LinkButton(R.string.about_website) { open(AboutLink.Website) }
            LinkButton(R.string.about_source) { open(AboutLink.Source) }
            LinkButton(R.string.about_report) { open(AboutLink.Issues) }
            if (noBrowser) Body(R.string.about_no_browser)

            // 5. Credits and licenses.
            SectionTitle(R.string.about_credits)
            Body(R.string.about_license)

            // 6. Made by Cocode.
            SectionTitle(R.string.about_made_by)
            Body(R.string.about_made_by_name)
            Body(R.string.about_made_by_email)

            // 7. Support: left empty until the Support phase (cocode-apps standard/support.md).
        }
    }
}
