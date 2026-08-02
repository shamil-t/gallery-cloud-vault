package com.shamil.cloudvault.utils

import android.content.Context
import com.shamil.cloudvault.R

/**
 * Sealed class for handling application errors
 */
sealed class AppError(
    val messageResId: Int,
    val throwable: Throwable? = null,
    val code: Int = -1
) {
    data class PermissionDenied(val permission: String) : AppError(
        messageResId = R.string.error_permission_denied,
        code = Constants.ERROR_CODE_PERMISSION_DENIED
    )

    data class FileNotFound(val filePath: String) : AppError(
        messageResId = R.string.error_file_not_found,
        code = Constants.ERROR_CODE_FILE_NOT_FOUND
    )

    data class InvalidFile(val reason: String) : AppError(
        messageResId = R.string.error_unknown,
        code = Constants.ERROR_CODE_INVALID_FILE
    )

    data class ShareFailed(val error: Throwable? = null) : AppError(
        messageResId = R.string.error_share_failed,
        throwable = error,
        code = 102
    )

    data class DeleteFailed(val error: Throwable? = null) : AppError(
        messageResId = R.string.error_delete_failed,
        throwable = error,
        code = 103
    )

    data class Unknown(val error: Throwable? = null) : AppError(
        messageResId = R.string.error_unknown,
        throwable = error,
        code = Constants.ERROR_CODE_UNKNOWN
    )

    fun getLocalizedMessage(context: Context): String {
        return try {
            context.getString(messageResId)
        } catch (e: Exception) {
            context.getString(R.string.error_unknown)
        }
    }
}

/**
 * Result wrapper for operations
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Failure(val error: AppError) : Result<Nothing>()
    object Loading : Result<Nothing>()

    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    fun getErrorOrNull(): AppError? = when (this) {
        is Failure -> error
        else -> null
    }
}

/**
 * Extension functions for Result handling
 */
inline fun <T, R> Result<T>.mapSuccess(transform: (T) -> R): Result<R> = when (this) {
    is Result.Success -> Result.Success(transform(data))
    is Result.Failure -> this
    is Result.Loading -> Result.Loading
}

inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> = apply {
    if (this is Result.Success) action(data)
}

inline fun <T> Result<T>.onFailure(action: (AppError) -> Unit): Result<T> = apply {
    if (this is Result.Failure) action(error)
}

