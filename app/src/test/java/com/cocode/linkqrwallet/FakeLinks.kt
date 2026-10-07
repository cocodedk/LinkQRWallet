package com.cocode.linkqrwallet

import androidx.sqlite.db.SupportSQLiteQuery
import com.cocode.linkqrwallet.data.LinkItem
import com.cocode.linkqrwallet.data.LinkItemDao
import com.cocode.linkqrwallet.data.TitleFetcher
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** A saved-links table kept in memory, so the tests can look at what was stored. */
class FakeLinkDao : LinkItemDao {
    val items = mutableListOf<LinkItem>()

    override fun observeLinks(query: SupportSQLiteQuery): Flow<List<LinkItem>> = flowOf(items.toList())

    override fun observeById(id: Long): Flow<LinkItem?> = flowOf(items.firstOrNull { it.id == id })

    override suspend fun findByUrl(url: String): LinkItem? = items.firstOrNull { it.url == url }

    override suspend fun insert(item: LinkItem): Long {
        val saved = item.copy(id = items.size + 1L)
        items.add(saved)
        return saved.id
    }

    override suspend fun update(item: LinkItem) {
        val index = items.indexOfFirst { it.id == item.id }
        if (index >= 0) items[index] = item
    }

    override suspend fun delete(item: LinkItem) {
        items.removeAll { it.id == item.id }
    }

    override suspend fun replaceTitle(id: Long, expected: String, savedAt: Long, title: String) {
        val index = items.indexOfFirst { it.id == id && it.title == expected && it.updatedAt == savedAt }
        if (index >= 0) items[index] = items[index].copy(title = title)
    }
}

/** Records every address it is asked for. Set [gate] to hold the answer back until the test lets it go. */
class RecordingTitleFetcher(
    private val answer: String? = "Example Domain",
    private val gate: CompletableDeferred<Unit>? = null
) : TitleFetcher() {
    val asked = mutableListOf<String>()

    override suspend fun fetchTitle(url: String): String? {
        asked.add(url)
        gate?.await()
        return answer
    }
}
