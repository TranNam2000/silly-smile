package com.jrm.base

import android.app.Activity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jrm.utils.InternetUtil
import com.jrm.utils.Logger
import kotlinx.coroutines.launch

/**
 * Base ViewModel class with common functionality
 * Provides event handling, timing tracking, network checking, and logging utilities
 */
abstract class BaseViewModel : ViewModel() {

    // Timing tracking
    protected var timeInitActionStart: Long = 0
    protected var timeNetworkCheckStart: Long = 0

    /**
     * Check network availability
     * @param activity Activity context
     * @param onNetworkCheckCompleted Callback when network check completes
     * @param onNetworkUnavailable Callback when network is unavailable (optional)
     */
    protected fun checkNetwork(
        activity: Activity,
        onNetworkCheckCompleted: () -> Unit,
        onNetworkUnavailable: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                timeInitActionStart = System.currentTimeMillis()
                timeNetworkCheckStart = System.currentTimeMillis()

                val networkAvailable = InternetUtil.isNetworkAvailable(activity)
                val currentTime = System.currentTimeMillis()
                val networkCheckDuration = currentTime - timeNetworkCheckStart

                logTimeEvent(
                    eventName = "network_check_completed",
                    currentTime = currentTime,
                    duration = networkCheckDuration,
                    extraParams = mapOf("network_available" to networkAvailable.toString())
                )

                if (!networkAvailable) {
                    onNetworkUnavailable?.invoke()
                    return@launch
                }

                onNetworkCheckCompleted()
            } catch (e: Throwable) {
                Logger.e("Error checking network", e)
                onNetworkUnavailable?.invoke()
            }
        }
    }

    /**
     * Log time tracking events
     * @param eventName Name of the event to log
     * @param currentTime Current time in milliseconds
     * @param duration Duration in milliseconds
     * @param extraParams Additional parameters to include in the event
     */
    protected fun logTimeEvent(
        eventName: String,
        currentTime: Long,
        duration: Long,
        extraParams: Map<String, String> = emptyMap()
    ) {
        val currentTimeFromAppStart = currentTime - timeInitActionStart

        val durationSeconds = duration / 1000.0
        val timeFromAppStartSeconds = currentTimeFromAppStart / 1000.0

        val eventParams = mutableMapOf<String, Any?>()
        eventParams["duration_seconds"] = durationSeconds
        eventParams["time_from_app_start_seconds"] = timeFromAppStartSeconds

        extraParams.forEach { (key, value) ->
            eventParams[key] = value
        }

        try {
            BaseEventLogger.logCustomEvent(eventName, custom = eventParams)
        } catch (e: Exception) {
            Logger.e("Failed to log event: $eventName", e)
        }
    }

    /**
     * Execute a coroutine with error handling
     * @param onError Error handler (optional)
     * @param block The coroutine block to execute
     */
    protected fun executeWithErrorHandling(
        onError: ((Throwable) -> Unit)? = null,
        block: suspend () -> Unit
    ) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: Throwable) {
                Logger.e("Error in ViewModel", e)
                onError?.invoke(e)
            }
        }
    }

    /**
     * Get elapsed time from init action start
     */
    protected fun getElapsedTimeFromInit(): Long {
        return if (timeInitActionStart > 0) {
            System.currentTimeMillis() - timeInitActionStart
        } else {
            0L
        }
    }
}

