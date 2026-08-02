package com.shamil.cloudvault.model

import android.net.Uri

data class AlbumItem(
    val id: Long,
    val name: String,
    val cover: Uri,
    val count: Int,
    val isVideo: Boolean = false
)
