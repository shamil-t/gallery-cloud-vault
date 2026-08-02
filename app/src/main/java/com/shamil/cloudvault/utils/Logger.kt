package com.shamil.cloudvault.utils

import androidx.media3.common.BuildConfig

/**
 * Logging utility for the application
 * In production builds, only logs errors and warnings
 * In debug builds, logs all levels
 */
object Logger {
    enum class LogLevel {
        VERBOSE, DEBUG, INFO, WARN, ERROR
    }

    private const val TAG = "CloudVault"

    fun v(tag: String = TAG, message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            android.util.Log.v(tag, message, throwable)
        }
    }

    fun d(tag: String = TAG, message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            android.util.Log.d(tag, message, throwable)
        }
    }

    fun i(tag: String = TAG, message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            android.util.Log.i(tag, message, throwable)
        } else {
            android.util.Log.i(tag, message)
        }
    }

    fun w(tag: String = TAG, message: String, throwable: Throwable? = null) {
        android.util.Log.w(tag, message, throwable)
    }

    fun e(tag: String = TAG, message: String, throwable: Throwable? = null) {
        android.util.Log.e(tag, message, throwable)
    }

    fun logException(tag: String = TAG, exception: Exception) {
        when {
            BuildConfig.DEBUG -> e(tag, "Exception occurred", exception)
            else -> e(tag, "Exception: ${exception.message}")
        }
    }

    fun logMethodCall(tag: String, methodName: String) {
        if (BuildConfig.DEBUG) {
            d(tag, "→ $methodName")
        }
    }

    fun logMethodReturn(tag: String, methodName: String, result: Any? = null) {
        if (BuildConfig.DEBUG) {
            d(tag, "← $methodName: $result")
        }
    }
}

