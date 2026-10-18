package com.cocode.linkqrwallet.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cocode.linkqrwallet.data.SortOption

@Composable
internal fun EmptyLibraryState(sortOption: SortOption) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No links yet",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "Add your first URL and start building your wallet.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Sorting by ${sortOption.label}",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
internal fun NoMatchesState(query: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No matches",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "No saved link matches \"$query\".",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
