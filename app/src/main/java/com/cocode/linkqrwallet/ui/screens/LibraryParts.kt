package com.cocode.linkqrwallet.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cocode.linkqrwallet.R
import com.cocode.linkqrwallet.data.LinkItem
import com.cocode.linkqrwallet.data.SortOption
import com.cocode.linkqrwallet.ui.components.rememberQrBitmap
import java.text.DateFormat
import java.util.Date

@Composable
internal fun LinkRow(item: LinkItem, onClick: () -> Unit) {
    val qrBitmap = rememberQrBitmap(item.url, 120)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            bitmap = qrBitmap,
            contentDescription = stringResource(R.string.qr_code_description),
            modifier = Modifier.size(60.dp)
        )
        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.domain,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Text(
            text = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(item.createdAt)),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
internal fun EmptyLibraryState(sortOption: SortOption) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.library_empty_title),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = stringResource(R.string.library_empty_body),
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.library_sorted_by, stringResource(sortOption.label)),
            style = MaterialTheme.typography.bodySmall
        )
    }
}
