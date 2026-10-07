package com.cocode.linkqrwallet.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow

@Dao
interface LinkItemDao {
    @RawQuery(observedEntities = [LinkItem::class])
    fun observeLinks(query: SupportSQLiteQuery): Flow<List<LinkItem>>

    @Query("SELECT * FROM link_items WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<LinkItem?>

    @Query("SELECT * FROM link_items WHERE url = :url LIMIT 1")
    suspend fun findByUrl(url: String): LinkItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: LinkItem): Long

    @Update
    suspend fun update(item: LinkItem)

    @Delete
    suspend fun delete(item: LinkItem)

    /**
     * Swaps in a fetched title only while the link is exactly as it was saved: the title is still
     * [expected] and it was not changed since ([savedAt] is its updatedAt, which every rename bumps),
     * so a title the person typed or changed is never replaced.
     */
    @Query("UPDATE link_items SET title = :title WHERE id = :id AND title = :expected AND updatedAt = :savedAt")
    suspend fun replaceTitle(id: Long, expected: String, savedAt: Long, title: String)
}
