package com.jrm.onboarding.splash

import android.view.View
import com.jrm.base.BaseActivity
import com.jrm.utils.BaseConstants
import com.jrm.utils.Logger

/**
 * Helper class to manage splash screen ad loading operations
 * Makes it easy to extend and modify ad loading logic
 */
class SplashAdLoader(
    private val activity: BaseActivity<*>,
    private val viewModel: SplashViewModel
) {

    /**
     * Load and show splash native/banner ad
     */
    fun loadSplashNativeOrBanner(adView: View?) {
        activity.loadAds(
            placementId = BaseConstants.PLACEMENT_SPLASH_AD_VIEW,
            adView = adView,
            onSuccess = {
                viewModel.onSplashNativeLoaded(true)
            },
            onFailure = {
                Logger.w("❌ [SPLASH] Splash Native/Banner Load failed")
                viewModel.onSplashNativeLoaded(false)
            }
        )
    }

    /**
     * Preload L1 (Language 1) native ad
     */
    fun preloadL1Native(onFailure: () -> Unit) {
        activity.preloadAds(
            placementId = BaseConstants.PLACEMENT_LANGUAGE_1_AD_VIEW,
            onSuccess = {
                viewModel.onL1Loaded(true)
            },
            onFailure = onFailure
        )
    }

    /**
     * Preload L2 (Language 2) native ad
     */
    fun preloadL2Native(onFailure: () -> Unit) {
        activity.preloadAds(
            placementId = BaseConstants.PLACEMENT_LANGUAGE_2_AD_VIEW,
            onSuccess = {
                viewModel.onL2Loaded(true)
            },
            onFailure = onFailure
        )
    }

    /**
     * Preload Onboarding 1 native ad
     */
    fun preloadOb1() {
        activity.preloadAds(
            BaseConstants.PLACEMENT_ONBOARDING_1,
            onSuccess = { viewModel.onOb1Loaded() },
            onFailure = { viewModel.onOb1Loaded() }
        )
    }

    /**
     * Preload splash interstitial ad
     */
    fun preloadSplashInterstitial() {
        activity.preloadAds(
            placementId = BaseConstants.PLACEMENT_FS_SPLASH,
            onSuccess = {
                viewModel.onSplashInterPreloaded()
            },
            onFailure = {
                Logger.w("❌ [SPLASH] Splash interstitial preload failed")
                viewModel.onSplashInterPreloaded()
            }
        )
    }

    /**
     * Show splash interstitial ad
     */
    fun showSplashInterstitial(onComplete: () -> Unit) {
        activity.loadAds(
            placementId = BaseConstants.PLACEMENT_FS_SPLASH,
            onSuccess = onComplete,
            isShow = true,
            onFailure = {
                Logger.w("❌ [SPLASH] Interstitial failed, starting next activity anyway")
                onComplete()
            }
        )
    }
}

