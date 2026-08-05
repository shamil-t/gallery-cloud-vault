package com.shamil.cloudvault.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "media_items",
    indices = [
        Index(value = ["date"], name = "idx_media_date"),
        Index(value = ["folder"], name = "idx_media_folder"),
        Index(value = ["isFavorite"], name = "idx_media_favorite"),
        Index(value = ["isHidden"], name = "idx_media_hidden"),
        Index(value = ["date"], name = "idx_media_date_desc"),
        Index(value = ["syncGeneration"], name = "idx_media_sync_gen")
    ]
)
data class MediaEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val uri: String,
    val folder: String,
    val date: Long,
    val isVideo: Boolean,
    val size: Long,
    val path: String,
    val mimeType: String,
    val width: Int,
    val height: Int,
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val syncGeneration: Long = 0
)
