package com.shamil.cloudvault.utils

import androidx.compose.ui.unit.dp

/**
 * Application-wide constants
 */
object Constants {
    // Database
    const val DATABASE_NAME = "gallery_database"
    const val MEDIA_TABLE_NAME = "media_items"

    // Cache
    const val MEMORY_CACHE_SIZE_PERCENT = 0.15
    const val DISK_CACHE_SIZE_PERCENT = 0.10
    const val IMAGE_CACHE_DIR = "image_cache"
    const val THUMBNAIL_SIZE = 256

    // Grid
    val GRID_ITEM_SIZE = 120.dp
    val ALBUM_ITEM_SIZE = 160.dp
    val GRID_SPACING = 4.dp
    val ALBUM_SPACING = 8.dp

    // Image Loading
    const val VIDEO_FRAME_MICROS = 1000000L
    const val IMAGE_LOAD_TIMEOUT_MS = 10000L

    // Permission
    const val PERMISSION_REQUEST_DELAY_MS = 500L

    // Media Viewer
    const val PAGER_SPACING_DP = 16f
    const val MIN_ZOOM = 1f
    const val MAX_ZOOM = 5f
    const val ZOOM_ANIMATION_DURATION_MS = 200

    // Pagination
    const val PAGE_SIZE = 50
    const val INITIAL_LOAD_SIZE = 100
    const val PREFETCH_DISTANCE = 25

    // Preferences
    const val PREFERENCES_NAME = "settings"
    const val PREF_THEME_KEY = "app_theme"
    const val PREF_THEME_STYLE_KEY = "app_theme_style"
    const val PREF_APP_ICON_STYLE_KEY = "app_icon_style"
    const val PREF_DYNAMIC_COLOR_KEY = "dynamic_color"
    const val PREF_BIOMETRIC_LOCK_KEY = "biometric_lock"
    const val PREF_GRID_COLUMN_COUNT_KEY = "grid_column_count"

    // Grid Limits
    const val DEFAULT_GRID_COLUMN_COUNT = 3
    const val MIN_GRID_COLUMNS = 2
    const val MAX_GRID_COLUMNS = 6

    // Timeouts
    const val COROUTINE_TIMEOUT_MS = 5000L
    const val NETWORK_TIMEOUT_MS = 30000L

    // UI
    const val SPLASH_SCREEN_DURATION_MS = 1000L
    const val ANIMATION_DURATION_MS = 300

    // File Operations
    const val MAX_FILE_SIZE_MB = 500
    const val SUPPORTED_IMAGE_TYPES = "image/*"
    const val SUPPORTED_VIDEO_TYPES = "video/*"

    // Logging
    const val TAG_GALLERY = "GalleryScreen"
    const val TAG_REPOSITORY = "GalleryRepository"
    const val TAG_VIEW_MODEL = "GalleryViewModel"
    const val TAG_MEDIA_VIEWER = "MediaViewer"

    // Error Codes
    const val ERROR_CODE_PERMISSION_DENIED = 100
    const val ERROR_CODE_FILE_NOT_FOUND = 101
    const val ERROR_CODE_INVALID_FILE = 102
    const val ERROR_CODE_UNKNOWN = 999
}

