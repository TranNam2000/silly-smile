package com.nomyek.myapplication.ui.splash

import android.os.Bundle
import com.jrm.model.DataPage
import com.jrm.onboarding.splash.BaseSplashActivity
import com.jrm.utils.enum.BackgroundType
import com.nomyek.myapplication.R
import com.nomyek.myapplication.databinding.ContentSplashBinding
import com.nomyek.myapplication.ui.main.MainActivity

/**
 * SplashActivity for the application
 *
 * This activity extends the base SplashActivity from the core library
 * and provides app-specific customizations such as background setup.
 *
 * The base SplashActivity handles:
 * - Network checking
 * - Consent dialog
 * - Remote config loading
 * - Ad initialization and preloading
 * - Progress tracking
 * - Navigation to next screen (Language/Onboarding/Home)
 *
 */
class SplashActivity : BaseSplashActivity<ContentSplashBinding>() {

    /**
     * Provide the layout resource ID for the custom content view
     * This layout will be inflated into the content container using data binding
     *
     * @return Layout resource ID for the content (0 if no custom content needed)
     */
    override fun getLayoutContentActivity(): Int {
        return R.layout.content_splash
    }

    /**
     * Called when the activity is first created.
     * This is where you can customize the splash screen appearance.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupBackground()

        // Access custom content views using data binding
        // Example: viewBindingContent.customTextView?.text = "Custom Text"
    }
    private fun setupBackground() {
        setBackgroundView(
            backgroundType = BackgroundType.COLOR,
            colorStart = resources.getColor(R.color.black)
        )
    }

    /**
     * Override this method if you need to customize the behavior
     * when the splash screen is initialized
     */
    override fun initViews() {
        super.initViews()
        startAppNameAnimation(
            viewBindingContent.appNameText
        )
        setOnboardingPages(
            listOf(
                DataPage(
                    image = com.jrm.R.drawable.obd1,
                    title = com.jrm.R.string.obd_title1,
                    detail = com.jrm.R.string.obd_detail1
                ),
                DataPage(
                    image = com.jrm.R.drawable.obd2,
                    title = R.string.obd_title2,
                    detail = R.string.obd_detail2
                ),
                DataPage(
                    image = com.jrm.R.drawable.obd3,
                    title = R.string.obd_title3,
                    detail = R.string.obd_detail3
                )
            )
        )
        setScreenHome(MainActivity::class.java)
    }

}
