package com.cocode.linkqrwallet.ui.screens

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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cocode.linkqrwallet.ui.components.rememberQrBitmap
import com.cocode.linkqrwallet.ui.viewmodel.AddLinkViewModel
import com.cocode.linkqrwallet.ui.viewmodel.AppViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLinkScreen(
    viewModelFactory: AppViewModelFactory,
    initialUrl: String,
    onDone: (Long) -> Unit,
    onCancel: () -> Unit
) {
    val viewModel: AddLinkViewModel = viewModel(factory = viewModelFactory)
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(initialUrl) {
        if (initialUrl.isNotBlank() && state.rawUrl.isBlank()) {
            viewModel.updateUrl(initialUrl)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Link") },
                navigationIcon = {
                    TextButton(onClick = onCancel) {
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
            OutlinedTextField(
                value = state.rawUrl,
                onValueChange = viewModel::updateUrl,
                label = { Text("URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            if (state.errorMessage != null) {
                Text(
                    text = state.errorMessage ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::updateTitle,
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.normalizedUrl != null) {
                    val qrBitmap = rememberQrBitmap(state.normalizedUrl ?: "", 220)
                    Image(
                        bitmap = qrBitmap,
                        contentDescription = "QR preview",
                        modifier = Modifier.size(140.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.domain.ifBlank { "Domain will appear here" },
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            state.normalizedUrl?.let { viewModel.fetchTitle(it) }
                        },
                        enabled = state.normalizedUrl != null
                    ) {
                        if (state.isFetchingTitle) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                        }
                        Text("Fetch Title")
                    }
                }
            }
            Button(
                onClick = {
                    viewModel.validateAndSave(
                        onSaved = onDone,
                        onDuplicate = { }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.normalizedUrl != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Save")
            }
        }
    }

    if (state.duplicateId != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearDuplicatePrompt() },
            title = { Text("Already saved") },
            text = { Text("This URL is already in your wallet. Add another copy?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearDuplicatePrompt()
                        viewModel.saveDuplicateAllowed(onDone)
                    }
                ) {
                    Text("Add duplicate")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.clearDuplicatePrompt() }) {
                    Text("Cancel")
                }
            }
        )
    }
}
