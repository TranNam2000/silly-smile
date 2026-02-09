package com.jrm.onboarding.splash

/**
 * Manages ad loading state for splash screen
 * Tracks which ads have been loaded
 */
class SplashAdStateManager {
    private var isSplashNativeLoaded = false
    private var isSplashInterPreloaded = false
    private var isL1L2Ob1NativePreloaded = false
    private var isL1HighLoaded = false
    private var isL2HighLoaded = false

    fun setSplashNativeLoaded(loaded: Boolean) {
        isSplashNativeLoaded = loaded
    }

    fun setSplashInterPreloaded(preloaded: Boolean) {
        isSplashInterPreloaded = preloaded
    }

    fun setL1L2Ob1NativePreloaded(preloaded: Boolean) {
        isL1L2Ob1NativePreloaded = preloaded
    }

    fun setL1HighLoaded(loaded: Boolean) {
        isL1HighLoaded = loaded
        if (loaded) {
            isL1L2Ob1NativePreloaded = true
        }
    }

    fun setL2HighLoaded(loaded: Boolean) {
        isL2HighLoaded = loaded
        if (loaded) {
            isL1L2Ob1NativePreloaded = true
        }
    }

    fun areAllAdsLoaded(): Boolean {
        return isSplashNativeLoaded && isSplashInterPreloaded && isL1L2Ob1NativePreloaded
    }

    fun isL1HighLoaded(): Boolean = isL1HighLoaded
    fun isL2HighLoaded(): Boolean = isL2HighLoaded
    fun isL1L2Ob1NativePreloaded(): Boolean = isL1L2Ob1NativePreloaded

    fun markAllAdsLoaded() {
        isSplashNativeLoaded = true
        isSplashInterPreloaded = true
        isL1L2Ob1NativePreloaded = true
    }
}

