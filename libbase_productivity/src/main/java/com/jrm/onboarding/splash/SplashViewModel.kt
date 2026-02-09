package com.jrm.onboarding.splash

import android.app.Activity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ads.nomyek_admob.ads_components.YNMAds
import com.jrm.base.BaseViewModel
import com.jrm.onboarding.language.Language2Activity
import com.jrm.utils.AdsHelper
import com.jrm.utils.BaseUtils
import com.jrm.utils.Logger
import com.jrm.utils.remote_config.RemoteConfigManager
import kotlinx.coroutines.delay

/**
 * ViewModel for Splash Screen
 * Handles all business logic, ad loading, and state management
 * Extends BaseViewModel for common functionality
 */
class SplashViewModel() : BaseViewModel() {

    private val _uiEvent = MutableLiveData<SplashUiEvent>()
    val uiEvent: LiveData<SplashUiEvent> = _uiEvent

    private val remoteConfigManager = RemoteConfigManager.instance

    // Configurable from RemoteConfig (defaults as fallback)
    private var minSplashTime = 0L
    var maxSplashTime = 0L
    private var enableBtnContinue = false
    var isTwoScreen = false

    // State management
    var isProgressCompleted = false
    private val _canNextScreenEvent = MutableLiveData<Boolean>()
    val canNextScreenEvent: LiveData<Boolean> = _canNextScreenEvent

    // Helper managers
    private val adStateManager = SplashAdStateManager()
    private val timingManager = SplashTimingManager(viewModelScope)
    private val navigationDecisionMaker: SplashNavigationDecisionMaker by lazy {
        SplashNavigationDecisionMaker(adStateManager, isTwoScreen)
    }

    private var hasTriggeredAdsLoaded = false


    // ========== Logic Methods ==========

    /**
     * User Click Continue
     * Logic: Check ads config -> Choose action
     */
    fun onContinueClicked() {
        val isDisableAllAd = AdsHelper.isDisableAllAd()
        val isDisableObdAd = AdsHelper.isDisableObdAd()
        // Show interstitial if ads are NOT disabled (both must be false)
        if (!isDisableAllAd && !isDisableObdAd) {
            _uiEvent.value = SplashUiEvent.ShowInterstitialSplash
        } else {
            _uiEvent.value = SplashUiEvent.AutoOpen
        }
    }

    fun checkNetwork(activity: Activity, onNetworkCheckCompleted: () -> Unit) {
        checkNetwork(
            activity = activity,
            onNetworkCheckCompleted = onNetworkCheckCompleted,
            onNetworkUnavailable = {
                _uiEvent.value = SplashUiEvent.ShowNotNetwork
            }
        )
    }

    fun initLoadAds(activity: Activity) {
        executeWithErrorHandling(
            onError = { e ->
                Logger.e("🔴 [SPLASH_VM] Error in initLoadAds", e)
            }
        ) {
            val configLoaded = remoteConfigManager?.loadConfigCallback(activity) == true

            if (configLoaded) {
                minSplashTime = remoteConfigManager.minTimeSplash
                maxSplashTime = remoteConfigManager.timeOutSplash
                enableBtnContinue = remoteConfigManager.enableBtnContinueSplash
                isTwoScreen =
                    remoteConfigManager.adConfig?.screenObd?.language?.isTwoScreen ?: false

                _uiEvent.value = SplashUiEvent.ProgressTime

                val isDisableObd = AdsHelper.isDisableObdAd()

                if (!isDisableObd) {
                    timingManager.setStartTime(System.currentTimeMillis())

                    YNMAds.getInstance().setInitCallback {
                        _uiEvent.value = SplashUiEvent.LoadNativeOrBanner
                        _uiEvent.value = SplashUiEvent.PreLoadSplashInterstitial
                        val screenObd = remoteConfigManager.adConfig?.screenObd
                        val isFinishObd = BaseUtils.isFinishObd()

                        if (!isFinishObd) {
                            val langEnable = screenObd?.language?.enable == true

                            if (langEnable) {
                                _uiEvent.value =
                                    if (isTwoScreen)
                                        SplashUiEvent.LoadL1NativeAd
                                    else SplashUiEvent.LoadL2NativeAd
                            } else {
                                val obEnable = screenObd?.onboarding?.enable == true
                                if (obEnable) {
                                    _uiEvent.value = SplashUiEvent.LoadObAds
                                }
                            }
                        } else {
                            val obEnable = screenObd?.onboarding?.enable == true
                            if (obEnable) {
                                _uiEvent.value = SplashUiEvent.LoadObAds
                            }
                        }
                        checkAndShowContinueButton()
                    }
                } else {
                    timingManager.setStartTime(System.currentTimeMillis())
                    delay(minSplashTime)
                    onTimeoutReached()
                }
            } else {
                val currentTime = System.currentTimeMillis()
                logTimeEvent(
                    eventName = SplashConstants.EVENT_SPLASH_YNMADS_INIT_COMPLETED,
                    currentTime = currentTime,
                    duration = 0,
                    extraParams = mapOf("ads_disabled" to "true")
                )

                adStateManager.markAllAdsLoaded()
                checkAdsLoadCompletion()
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
        adStateManager.setSplashNativeLoaded(true)
        checkAdsLoadCompletion()
    }

    /**
     * Called when splash interstitial preloaded
     */
    fun onSplashInterPreloaded() {
        adStateManager.setSplashInterPreloaded(true)
        checkAdsLoadCompletion()
    }

    /**
     * Called when L1 native ad loads
     */
    fun onL1Loaded(success: Boolean) {
        if (success) {
            adStateManager.setL1HighLoaded(true)
            checkAdsLoadCompletion()
        } else {
            Logger.w("🔴 [SPLASH_VM] L1 load failed")
        }
    }

    /**
     * Called when L2 native ad loads
     */
    fun onL2Loaded(success: Boolean) {
        if (success) {
            adStateManager.setL2HighLoaded(true)
            Language2Activity.adPreloadIsLoading.postValue(false)
            Language2Activity.isLoadDoneSplash = true
            checkAdsLoadCompletion()
        } else {
            Logger.w("🔴 [SPLASH_VM] L2 load failed")
        }
    }

    /**
     * Called when Ob1 ad loads (end of chain)
     */
    fun onOb1Loaded() {
        adStateManager.setL1L2Ob1NativePreloaded(true)
        checkAndShowContinueButton()
    }

    /**
     * Check if all ads finished loading
     */
    private fun checkAdsLoadCompletion() {
        if (adStateManager.areAllAdsLoaded() && !hasTriggeredAdsLoaded) {
            hasTriggeredAdsLoaded = true
            timingManager.setAdsLoadedTime(System.currentTimeMillis())
            checkAndShowContinueButton()
        }
    }

    /**
     * Check and show continue button
     * Requirements:
     * 1. Must wait minimum time even if ads load early
     * 2. Show button when: (all ads loaded AND min time reached) OR timeout
     */
    fun checkAndShowContinueButton() {
        // Case 1: Timeout reached
        if (isProgressCompleted) {
            handleContinueAction()
            return
        }

        val currentTime = System.currentTimeMillis()
        val elapsedTime = timingManager.getElapsedTime(currentTime)
        val isMinTimeMet = timingManager.isMinTimeMet(elapsedTime, minSplashTime)
        val allAdsLoaded = adStateManager.areAllAdsLoaded()

        // Case 2: All ads loaded and min time met
        if (allAdsLoaded && isMinTimeMet) {
            handleContinueAction()
            return
        }

        // Case 3: Ads loaded early - wait for min time
        if (allAdsLoaded) {
            val remainingTime = timingManager.calculateRemainingTime(elapsedTime, minSplashTime)
            timingManager.waitForRemainingTime(
                remainingTime = remainingTime,
                onComplete = {
                    if (!isProgressCompleted) {
                        handleContinueAction()
                    }
                },
                shouldCancel = { isProgressCompleted }
            )
        }
    }

    private fun handleContinueAction() {
        if (enableBtnContinue) {
            _uiEvent.value = SplashUiEvent.ShowButtonContinue
        } else {
            onContinueClicked()
        }
        _canNextScreenEvent.value = true
    }

    /**
     * Navigation target decision
     */
    fun getNavTarget(): NavigationTarget {
        return navigationDecisionMaker.determineTarget()
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
