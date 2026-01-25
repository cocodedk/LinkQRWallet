package com.cocode.linkqrwallet.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class TitleFetcher {
    suspend fun fetchTitle(url: String): String? = withContext(Dispatchers.IO) {
        try {
            Jsoup.connect(url)
                .userAgent("LinkQRWallet/1.0")
                .timeout(8000)
                .get()
                .title()
                .trim()
                .takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }
}
