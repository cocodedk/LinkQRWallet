package com.cocode.linkqrwallet.data

import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow

class LinkRepository(private val dao: LinkItemDao) {
    fun observeLinks(query: String, sortOption: SortOption): Flow<List<LinkItem>> {
        val sql = buildQuery(query, sortOption)
        return dao.observeLinks(sql)
    }

    fun observeById(id: Long): Flow<LinkItem?> = dao.observeById(id)

    suspend fun findByUrl(url: String): LinkItem? = dao.findByUrl(url)

    suspend fun insert(item: LinkItem): Long = dao.insert(item)

    suspend fun update(item: LinkItem) = dao.update(item)

    suspend fun delete(item: LinkItem) = dao.delete(item)

    private fun buildQuery(query: String, sortOption: SortOption): SupportSQLiteQuery {
        val like = "%${query.trim().lowercase()}%"
        val sql = """
            SELECT * FROM link_items
            WHERE lower(title) LIKE ? OR lower(url) LIKE ? OR lower(domain) LIKE ?
            ORDER BY ${sortOption.orderByClause}
        """.trimIndent()
        val args = arrayOf(like, like, like)
        return SimpleSQLiteQuery(sql, args)
    }
}
