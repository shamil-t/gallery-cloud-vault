package com.shamil.cloudvault.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UpdateInfo(
    val versionCode: String,
    val versionName: String,
    val latestReleaseUrl: String,
    val releaseNotes: String? = null
)
