package com.jrm.onboarding.splash

import com.jrm.utils.AdsHelper
import com.jrm.utils.BaseUtils

/**
 * Makes navigation decisions based on app state and ad loading status
 * Easy to extend with new navigation logic
 */
class SplashNavigationDecisionMaker(
    private val adStateManager: SplashAdStateManager,
    private val isTwoScreen: Boolean
) {

    fun determineTarget(): SplashViewModel.NavigationTarget {
        val isFinishObd = BaseUtils.isFinishObd()
        val isDisableObdAd = AdsHelper.isDisableObdAd()

        // Priority 1: If OBD is finished or disabled, go to Home
        if (isFinishObd || isDisableObdAd) {
            return SplashViewModel.NavigationTarget.Home
        }

        // Priority 2: Check language screen conditions
        if (adStateManager.isL1HighLoaded() && isTwoScreen) {
            return SplashViewModel.NavigationTarget.Language1
        }

        if (adStateManager.isL2HighLoaded()) {
            return SplashViewModel.NavigationTarget.Language2
        }

        // Priority 3: Check onboarding condition
        if (adStateManager.isL1L2Ob1NativePreloaded()) {
            return SplashViewModel.NavigationTarget.Onboarding
        }

        // Default: Go to Home
        return SplashViewModel.NavigationTarget.Home
    }
}

