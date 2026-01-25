package com.cocode.linkqrwallet.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "link_items",
    indices = [
        Index(value = ["url"]),
        Index(value = ["createdAt"]),
        Index(value = ["title"]),
        Index(value = ["domain"])
    ]
)
data class LinkItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val url: String,
    val title: String,
    val domain: String,
    val createdAt: Long,
    val updatedAt: Long,
    val notes: String? = null,
    val isFavorite: Boolean = false
)
