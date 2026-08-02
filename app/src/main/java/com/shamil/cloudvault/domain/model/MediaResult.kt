package com.shamil.cloudvault.domain.model

sealed class MediaResult<out T> {
    object Loading : MediaResult<Nothing>()
    data class Success<out T>(val data: T) : MediaResult<T>()
    data class Error(val exception: Throwable) : MediaResult<Nothing>()
}
