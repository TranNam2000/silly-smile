package com.jrm.utils

import android.util.Log
import com.jrm.BuildConfig

/**
 * Centralized app logger. No tag in API; uses a single default tag internally.
 * Logging follows [enabled] per environment (debug/release).
 *
 * Usage:
 * ```
 * Logger.d("message")
 * Logger.e("error", throwable)
 * ```
 *
 * [enabled] is initialized from [BuildConfig.DEBUG] (logs only in debug builds). Override if needed.
 */
object Logger {

    private const val TAG = "Logger"

    /** When false, all log calls are no-op. Initialized from BuildConfig.DEBUG. */
    @Volatile
    var enabled: Boolean = BuildConfig.DEBUG
        set(value) { field = value }


    @JvmStatic
    fun d(message: String) {
        if (enabled) Log.d(TAG, message)
    }

    @JvmStatic
    fun i(message: String) {
        if (enabled) Log.i(TAG, message)
    }

    @JvmStatic
    fun w(message: String) {
        if (enabled) Log.w(TAG, message)
    }

    @JvmStatic
    fun w(message: String, throwable: Throwable?) {
        if (enabled) Log.w(TAG, message, throwable)
    }

    @JvmStatic
    fun e(message: String) {
        if (enabled) Log.e(TAG, message)
    }

    @JvmStatic
    fun e(message: String, throwable: Throwable?) {
        if (enabled) Log.e(TAG, message, throwable)
    }
}
