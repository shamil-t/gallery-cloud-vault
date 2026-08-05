package com.shamil.cloudvault.data.local

import androidx.room.ColumnInfo

data class MediaThumbnail(
    val id: Long,
    val uri: String,
    val isVideo: Boolean,
    val isFavorite: Boolean,
    val date: Long // Needed for sorting if used in queries
)

data class AlbumSummary(
    val folder: String,
    @ColumnInfo(name = "itemCount") val itemCount: Int,
    @ColumnInfo(name = "coverUri") val coverUri: String,
    @ColumnInfo(name = "isVideo") val isVideo: Boolean,
    @ColumnInfo(name = "latestId") val latestId: Long
)
