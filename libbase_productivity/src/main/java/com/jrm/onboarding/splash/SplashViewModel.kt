package com.jrm.onboarding.splash

import android.app.Activity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ads.nomyek_admob.ads_components.YNMAds
import com.jrm.base.BaseEventLogger
import com.jrm.onboarding.language.Language2Activity
import com.jrm.utils.AdsHelper
import com.jrm.utils.BaseUtils
import com.jrm.utils.InternetUtil
import com.jrm.utils.Logger
import com.jrm.utils.remote_config.RemoteConfigManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ViewModel for Splash Screen
 * Handles all business logic, ad loading, and state management
 */

class SplashViewModel() : ViewModel() {

    val isNativeSplashClicked: Boolean = false

    private val _uiEvent = MutableLiveData<SplashUiEvent>()
    val uiEvent: LiveData<SplashUiEvent> = _uiEvent

    companion object {
        private const val TAG = "SplashViewModel"
    }

    private val remoteConfigManager = RemoteConfigManager.instance

    // Configurable from RemoteConfig (defaults as fallback)
    private var minSplashTime = 0L  // Default 6s
    var maxSplashTime = 0L // Default 12s
    private var enableBtnContinue = false // Default Config
    var isTwoScreen = false


    // ========== State Flags (Moved from Activity) ==========
    private var isSplashNativeLoaded = false
    private var isSplashInterPreloaded = false
    private var isL1L2Ob1NativePreloaded = false
    var isProgressCompleted = false

    // Min Time Flag
    private val _canNextScreenEvent = MutableLiveData<Boolean>()
    val canNextScreenEvent: LiveData<Boolean> = _canNextScreenEvent

    // Navigation Decision Flags
    private var isL1HighLoaded = false
    private var isL2HighLoaded = false

    // Navigation State
    private var _isNavigationCalled = false

    private var timeInitActionStart: Long = 0
    private var timeNetworkCheckStart: Long = 0
    private var timeRemoteConfigLoadStart: Long = 0
    private var timeYNMAdsInitStart: Long = 0

    // Timing tracking
    private var splashStartTime: Long = 0
    private var adsLoadedTime: Long = 0
    private var isMinTimeReached = false


    // ========== Logic Methods ==========

    /**
     * User Click Continue
     * Logic: Check ads config -> Choose action
     */
    fun onContinueClicked() {
        if (!AdsHelper.isDisableAllAd() || !AdsHelper.isDisableObdAd()) {
            _uiEvent.value = SplashUiEvent.ShowInterstitialSplash
        } else {
            _uiEvent.value = SplashUiEvent.AutoOpen
        }
    }

    fun checkNetwork(activity: Activity, onNetworkCheckCompleted: () -> Unit) {
        viewModelScope.launch {
            try {
                timeInitActionStart = System.currentTimeMillis()

                timeNetworkCheckStart = System.currentTimeMillis()
                val networkAvailable = InternetUtil.isNetworkAvailable(activity)

                val currentTime = System.currentTimeMillis()

                // Log network check duration
                val networkCheckDuration = currentTime - timeNetworkCheckStart
                logTimeEvent(
                    "splash_network_check_completed",
                    currentTime,
                    networkCheckDuration,
                    mapOf("network_available" to networkAvailable.toString())
                )
                Logger.d("networkAvailable: " + networkAvailable)

                if (!networkAvailable) {
                    _uiEvent.value = SplashUiEvent.ShowNotNetwork
                    return@launch
                }
                onNetworkCheckCompleted()
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    fun initLoadAds(activity: Activity) {
        viewModelScope.launch {
            try {
                if (remoteConfigManager?.loadConfigCallback(activity) == true) {
                    timeRemoteConfigLoadStart = System.currentTimeMillis()
                    minSplashTime = remoteConfigManager.minTimeSplash
                    maxSplashTime = remoteConfigManager.timeOutSplash
                    enableBtnContinue = remoteConfigManager.enableBtnContinueSplash
                    isTwoScreen =
                        remoteConfigManager.adConfig?.screenObd?.language?.isTwoScreen ?: false
                    _uiEvent.value = SplashUiEvent.ProgressTime

                    // Start timers logic (Moved here since startSplashTimers was removed)
                    splashStartTime = System.currentTimeMillis()
                    if (!AdsHelper.isDisableObdAd()) {
                        splashStartTime = System.currentTimeMillis()
                        timeYNMAdsInitStart = System.currentTimeMillis()
                        YNMAds.getInstance().setInitCallback {
                            _uiEvent.value = SplashUiEvent.LoadNativeOrBanner
                            _uiEvent.value = SplashUiEvent.PreLoadSplashInterstitial
                            val screenObd = remoteConfigManager.adConfig?.screenObd
                            Logger.d("screenObd: $screenObd")
                            if (!BaseUtils.isFinishObd()) {
                                if (screenObd?.language?.enable == true) {
                                    _uiEvent.value =
                                        if (remoteConfigManager.adConfig?.screenObd?.language?.isTwoScreen == true)
                                            SplashUiEvent.LoadL1NativeAd
                                        else SplashUiEvent.LoadL2NativeAd
                                } else {
                                    if (screenObd?.onboarding?.enable == true) {
                                        _uiEvent.value = SplashUiEvent.LoadObAds
                                    }
                                }
                            } else {
                                if (screenObd?.onboarding?.enable == true) {
                                    _uiEvent.value = SplashUiEvent.LoadObAds
                                }
                            }
                            checkAndShowContinueButton()

                        }
                    } else {
                        Logger.d("vào đây")
                        splashStartTime = System.currentTimeMillis()
                        delay(minSplashTime)
                        onTimeoutReached()
                    }
                } else {
                    val currentTime = System.currentTimeMillis()
                    Logger.d("vào đây 2")
                    // Log when ads are disabled
                    logTimeEvent(
                        "splash_ynmads_init_completed",
                        currentTime,
                        0,
                        mapOf("ads_disabled" to "true")
                    )

                    isSplashNativeLoaded = true;
                    isSplashInterPreloaded = true;
                    isL1L2Ob1NativePreloaded = true
                    markAllAdsLoaded()
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Called when max timeout (12s) is reached
     */
    fun onTimeoutReached() {
        isProgressCompleted = true
        checkAndShowContinueButton()
    }

    /**
     * Called when splash native ad loads
     */
    fun onSplashNativeLoaded(success: Boolean) {
        isSplashNativeLoaded = true
        checkAdsLoadCompletion()
    }

    /**
     * Called when splash interstitial preloaded
     */
    fun onSplashInterPreloaded() {
        isSplashInterPreloaded = true
        checkAdsLoadCompletion()
    }

    /**
     * Called when L1 native ad loads
     */
    fun onL1Loaded(success: Boolean) {
        if (success) {
            isL1HighLoaded = true
            isL1L2Ob1NativePreloaded = true
            checkAdsLoadCompletion()
        }
    }

    /**
     * Called when L2 native ad loads
     */
    fun onL2Loaded(success: Boolean) {
        if (success) {
            isL2HighLoaded = true
            isL1L2Ob1NativePreloaded = true
            Language2Activity.adPreloadIsLoading.postValue(false)
            Language2Activity.isLoadDoneSplash = true;
            checkAdsLoadCompletion()
        }
    }

    /**
     * Called when Ob1 ad loads (end of chain)
     */
    fun onOb1Loaded() {
        isL1L2Ob1NativePreloaded = true;
        checkAndShowContinueButton()
    }

    /**
     * Helper when ads are disabled
     */
    fun markAllAdsLoaded() {
        isSplashNativeLoaded = true
        isSplashInterPreloaded = true
        isL1L2Ob1NativePreloaded = true
        checkAdsLoadCompletion()
    }

    /**
     * Check if all ads finished loading
     */
    private fun checkAdsLoadCompletion() {
        val allAdsLoaded =
            isSplashNativeLoaded && isSplashInterPreloaded && isL1L2Ob1NativePreloaded

        if (allAdsLoaded && adsLoadedTime == 0L) {
            adsLoadedTime = System.currentTimeMillis()
            val loadDuration = adsLoadedTime - splashStartTime
            Logger.d("All ads loaded in ${loadDuration}ms")
            checkAndShowContinueButton()
        }
    }

    /**
     * NEW LOGIC: Check and show continue button
     * Requirements:
     * 1. Must wait minimum time even if ads load early
     * 2. Show button when: (all ads loaded AND min time reached) OR timeout
     */
    fun checkAndShowContinueButton() {
        // Case 1: Timeout reached - always show
        if (isProgressCompleted) {
            Logger.d("Showing button: Timeout (${maxSplashTime}ms) reached")
            _uiEvent.value = SplashUiEvent.ShowButtonContinue
            _canNextScreenEvent.value = true
            return
        }

        val allAdsLoaded =
            isSplashNativeLoaded && isSplashInterPreloaded && isL1L2Ob1NativePreloaded

        // Use Ad Init time if available (User request), otherwise fallback to Splash Start time
        val referenceTime = splashStartTime
        val elapsedTime = System.currentTimeMillis() - referenceTime
        val isMinTimeMet = elapsedTime >= minSplashTime

        if (allAdsLoaded && isMinTimeMet) {
            Logger.d(
                "Showing button: All ads loaded + Min time (${minSplashTime}ms) reached (Elapsed: ${elapsedTime}ms)"
            )
            if (enableBtnContinue) {
                _uiEvent.value = SplashUiEvent.ShowButtonContinue
            } else {
                _uiEvent.value = SplashUiEvent.AutoOpen
            }
            _canNextScreenEvent.value = true
            return
        }

        // Case 3: Ads loaded early - wait for min time
        if (allAdsLoaded) {
            val remainingTime = minSplashTime - elapsedTime

            if (remainingTime > 0) {
                Logger.d(
                    "Ads loaded early (Elapsed: ${elapsedTime}ms), waiting ${remainingTime}ms for min time"
                )
                // Wait until min time is reached before showing button
                viewModelScope.launch {
                    delay(remainingTime)
                    // Re-check to ensure we don't double fire if timeout happened during delay
                    if (!isProgressCompleted) {
                        Logger.d("Min wait completed after early ad load")
                        if (enableBtnContinue) {
                            _uiEvent.value = SplashUiEvent.ShowButtonContinue
                        } else {
                            _uiEvent.value = SplashUiEvent.AutoOpen
                        }
                        _canNextScreenEvent.value = true
                    }
                }
            } else {
                // Edge case: min time just passed during calculation
                if (enableBtnContinue) {
                    _uiEvent.value = SplashUiEvent.ShowButtonContinue
                } else {
                    _uiEvent.value = SplashUiEvent.AutoOpen
                }
                _canNextScreenEvent.value = true
            }
        }
    }

    /**
     * Navigation target decision
     */
    fun getNavTarget(): NavigationTarget {
        if (BaseUtils.isFinishObd() || AdsHelper.isDisableObdAd()) {
            return NavigationTarget.Home
        }
        return if (isL1HighLoaded) {
            NavigationTarget.Language1
        } else if (isL2HighLoaded && isTwoScreen) {
            NavigationTarget.Language2
        } else if (isL1L2Ob1NativePreloaded) {
            NavigationTarget.Onboarding
        } else {
            NavigationTarget.Home
        }
    }

    /**
     * Helper function to log time tracking events
     * @param eventName Name of the event to log
     * @param currentTime Current time in milliseconds
     * @param duration Duration in milliseconds
     * @param extraParams Additional parameters to include in the event
     */
    private fun logTimeEvent(
        eventName: String,
        currentTime: Long,
        duration: Long,
        extraParams: Map<String, String> = emptyMap()
    ) {
        val currentTimeFromAppStart = currentTime - timeInitActionStart

        val durationSeconds = duration / 1000.0
        val timeFromAppStartSeconds = currentTimeFromAppStart / 1000.0

        val logMessage = StringBuilder()
        logMessage.append("Event: $eventName")
        logMessage.append(" | Duration: ${String.format("%.2f", durationSeconds)}s")
        logMessage.append(
            " | Time from app start: ${
                String.format(
                    "%.2f",
                    timeFromAppStartSeconds
                )
            }s"
        )

        extraParams.forEach { (key, value) ->
            logMessage.append(" | $key: $value")
        }

        Logger.d(logMessage.toString())

        // Create event parameters with correct type Map<String, Any?>
        val eventParams = mutableMapOf<String, Any?>()
        eventParams["duration_seconds"] = durationSeconds
        eventParams["time_from_app_start_seconds"] = timeFromAppStartSeconds

        // Add extra parameters
        extraParams.forEach { (key, value) ->
            eventParams[key] = value
        }

        try {
            BaseEventLogger.logCustomEvent(eventName, custom = eventParams)
        } catch (e: Exception) {
            Logger.e("Failed to log event: $eventName", e)
        }
    }

    sealed class NavigationTarget {
        object Language1 : NavigationTarget()
        object Language2 : NavigationTarget()
        object Onboarding : NavigationTarget()
        object Home : NavigationTarget()
    }

    sealed class SplashUiEvent {
        object LoadL1NativeAd : SplashUiEvent()
        object LoadL2NativeAd : SplashUiEvent()
        object LoadObAds : SplashUiEvent()
        object PreLoadSplashInterstitial : SplashUiEvent()
        object LoadNativeOrBanner : SplashUiEvent()
        object ShowInterstitialSplash : SplashUiEvent()
        object ShowNotNetwork : SplashUiEvent()
        object ShowButtonContinue : SplashUiEvent()
        object ProgressTime : SplashUiEvent()
        object AutoOpen : SplashUiEvent()
    }
}
