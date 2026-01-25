package com.cocode.linkqrwallet.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cocode.linkqrwallet.data.LinkItem
import com.cocode.linkqrwallet.ui.components.generateQrBitmap
import com.cocode.linkqrwallet.ui.components.rememberQrBitmap
import com.cocode.linkqrwallet.ui.viewmodel.AppViewModelFactory
import com.cocode.linkqrwallet.ui.viewmodel.DetailViewModel
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    viewModelFactory: AppViewModelFactory,
    linkId: Long,
    onBack: () -> Unit
) {
    val viewModel: DetailViewModel = viewModel(factory = viewModelFactory)
    val link by viewModel.observeLink(linkId).collectAsState(initial = null)
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Link Detail") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (link == null) {
                Text("Loading...")
            } else {
                DetailContent(
                    item = link,
                    clipboardManager = clipboardManager,
                    onOpen = { uriHandler.openUri(link!!.url) },
                    onShare = { shareQr(context, link!!) },
                    onDelete = { showDeleteDialog = true },
                    onUpdateTitle = { newTitle ->
                        viewModel.update(link!!.copy(title = newTitle))
                    }
                )
            }
        }
    }

    if (showDeleteDialog && link != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete link?") },
            text = { Text("This will remove the link from your wallet.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.delete(link!!) { onBack() }
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DetailContent(
    item: LinkItem?,
    clipboardManager: ClipboardManager,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onUpdateTitle: (String) -> Unit
) {
    if (item == null) return
    val qrBitmap = rememberQrBitmap(item.url, 480)

    Image(
        bitmap = qrBitmap,
        contentDescription = "QR code",
        modifier = Modifier.size(240.dp)
    )
    OutlinedTextField(
        value = item.title,
        onValueChange = onUpdateTitle,
        label = { Text("Title") },
        modifier = Modifier.fillMaxWidth()
    )
    Text(text = item.url, style = MaterialTheme.typography.bodyMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onOpen) { Text("Open") }
        Button(onClick = {
            clipboardManager.setText(AnnotatedString(item.url))
        }) { Text("Copy") }
        Button(onClick = onShare) { Text("Share QR") }
    }
    Spacer(modifier = Modifier.height(8.dp))
    TextButton(onClick = onDelete) {
        Text("Delete", color = MaterialTheme.colorScheme.error)
    }
}

private fun shareQr(context: Context, item: LinkItem) {
    val bitmap = generateQrBitmap(item.url, 1024)
    val cacheDir = File(context.cacheDir, "shares").apply { mkdirs() }
    val file = File(cacheDir, "qr_${item.id}.png")
    FileOutputStream(file).use { output ->
        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output)
    }
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, item.url)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share QR"))
}
