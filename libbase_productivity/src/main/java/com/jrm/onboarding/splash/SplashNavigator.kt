package com.jrm.onboarding.splash

import android.content.Intent
import com.jrm.base.BaseActivity
import com.jrm.onboarding.language.Language2Activity
import com.jrm.onboarding.language.LanguageActivity
import com.jrm.onboarding.navigation.BaseNavigator
import com.jrm.onboarding.onboarding.OnboardingActivity
import com.jrm.utils.BaseExtension

/**
 * Helper class to handle navigation from splash screen
 * Makes it easy to extend and modify navigation logic
 */
class SplashNavigator(
    private val activity: BaseActivity<*>
) {

    /**
     * Navigate to target screen based on NavigationTarget
     */
    fun navigateTo(target: SplashViewModel.NavigationTarget) {
        when (target) {
            SplashViewModel.NavigationTarget.Language1 -> {
                val intent = Intent(activity, LanguageActivity::class.java)
                activity.startActivity(intent)
            }

            SplashViewModel.NavigationTarget.Language2 -> {
                val intent = Intent(activity, Language2Activity::class.java)
                activity.startActivity(intent)
            }

            SplashViewModel.NavigationTarget.Onboarding -> {
                BaseExtension.showActivity(
                    activity,
                    OnboardingActivity::class.java,
                    null
                )
            }

            SplashViewModel.NavigationTarget.Home -> {
                BaseNavigator.getInstance().navigateToHome(activity)
            }
        }
    }
}

