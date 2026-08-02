package com.shamil.cloudvault.model

import android.net.Uri

data class GalleryItem(
    val id: Long,
    val name: String,
    val uri: Uri,
    val folder: String,
    val date: Long,
    val isVideo: Boolean,
    val size: Long,
    val path: String,
    val mimeType: String,
    val width: Int,
    val height: Int,
    val isFavorite: Boolean = false
)
