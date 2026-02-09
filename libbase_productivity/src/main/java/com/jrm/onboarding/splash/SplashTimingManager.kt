package com.jrm.onboarding.splash

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Manages timing logic for splash screen
 * Handles min time, max time, and timeout scenarios
 */
class SplashTimingManager(
    private val viewModelScope: CoroutineScope
) {
    private var splashStartTime: Long = 0
    private var adsLoadedTime: Long = 0

    fun setStartTime(time: Long) {
        splashStartTime = time
    }

    fun setAdsLoadedTime(time: Long) {
        adsLoadedTime = time
    }

    fun getAdsLoadedTime(): Long = adsLoadedTime

    fun getElapsedTime(currentTime: Long): Long {
        return currentTime - splashStartTime
    }

    fun isMinTimeMet(elapsedTime: Long, minTime: Long): Boolean {
        return elapsedTime >= minTime
    }

    fun calculateRemainingTime(elapsedTime: Long, minTime: Long): Long {
        return minTime - elapsedTime
    }

    /**
     * Wait for remaining time and execute callback
     */
    fun waitForRemainingTime(
        remainingTime: Long,
        onComplete: () -> Unit,
        shouldCancel: () -> Boolean
    ) {
        if (remainingTime > 0) {
            viewModelScope.launch {
                delay(remainingTime)
                if (!shouldCancel()) {
                    onComplete()
                }
            }
        } else {
            onComplete()
        }
    }
}

