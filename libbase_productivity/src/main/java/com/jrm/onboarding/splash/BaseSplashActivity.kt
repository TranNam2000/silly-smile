package com.jrm.onboarding.splash

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import com.jrm.utils.Logger
import androidx.activity.viewModels
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.Observer
import com.ads.nomyek_admob.event.YNMAirBridge
import com.ads.nomyek_admob.utils.AdsNativeMultiPreload
import com.jrm.R
import com.jrm.base.BaseActivity
import com.jrm.base.BaseEventLogger
import com.jrm.databinding.ActivitySplashScreenBinding
import com.jrm.model.DataPage
import com.jrm.onboarding.consent_dialog.ConsentDialogManager
import com.jrm.onboarding.navigation.BaseNavigator
import com.jrm.onboarding.onboarding.OnboardingActivity
import com.jrm.utils.AnimationUtils
import com.jrm.utils.BaseConstants
import com.jrm.utils.BaseUtils
import com.jrm.utils.LocaleHelper
import com.jrm.utils.SharedPref
import com.jrm.utils.enum.BackgroundType
import com.jrm.utils.remote_config.RemoteConfigManager
import com.jrm.view.gradient.GradientTextView
import java.util.Locale

@SuppressLint("CustomSplashScreen")
abstract class BaseSplashActivity<VB : ViewDataBinding> :
    BaseActivity<ActivitySplashScreenBinding>() {

    // ========== ViewModel (MVVM) ==========
    private val viewModel: SplashViewModel by viewModels()

    // ========== Helper Classes ==========
    private val adLoader: SplashAdLoader by lazy { SplashAdLoader(this, viewModel) }
    private val adLoadingChain: SplashAdLoadingChain by lazy { SplashAdLoadingChain(adLoader) }
    private val navigator: SplashNavigator by lazy { SplashNavigator(this) }

    protected lateinit var viewBindingContent: VB


    override fun getLayoutActivity(): Int {
        return R.layout.activity_splash_screen
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (getIntent().extras != null) {
            handleNotificationClick(getIntent())
        }
    }

    override fun initViews() {
        val contentLayoutId = getLayoutContentActivity()
        if (contentLayoutId != 0) {
            try {
                // Inflate the content layout into the container
                val contentView =
                    layoutInflater.inflate(contentLayoutId, viewBinding.contentContainer, false)
                viewBinding.contentContainer.addView(contentView)
                viewBindingContent = androidx.databinding.DataBindingUtil.bind(contentView)
                    ?: throw IllegalStateException("Failed to bind content view")
            } catch (e: Exception) {
            }
        }
        viewModel.checkNetwork(this) {
            initAction()
        }

        BaseEventLogger.logCustomEvent(SplashConstants.EVENT_APP_OPEN)

        // Set default language based on device locale if first time opening
        if (!BaseUtils.isFinishObd()) {
            setDefaultLanguageBasedOnDeviceLocale()
        }
        // Apply shake animation to app name text for emphasis

        if (intent.extras != null) {
            handleNotificationClick(intent)
        }

        BaseUtils.checkAndPushRetentionEvents(this@BaseSplashActivity)

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.uiEvent.observe(this) { event ->
            when (event) {
                is SplashViewModel.SplashUiEvent.LoadL1NativeAd -> {
                    adLoadingChain.executeL1Chain()
                }
                is SplashViewModel.SplashUiEvent.LoadL2NativeAd -> {
                    adLoadingChain.executeL2Chain()
                }
                is SplashViewModel.SplashUiEvent.LoadObAds -> {
                    adLoadingChain.executeOb1Chain()
                }
                is SplashViewModel.SplashUiEvent.PreLoadSplashInterstitial -> {
                    adLoader.preloadSplashInterstitial()
                }
                is SplashViewModel.SplashUiEvent.LoadNativeOrBanner -> {
                    adLoader.loadSplashNativeOrBanner(viewBinding.nativeOnboarding)
                }
                is SplashViewModel.SplashUiEvent.ShowInterstitialSplash -> {
                    adLoader.showSplashInterstitial { startNextActivity() }
                }
                is SplashViewModel.SplashUiEvent.ShowNotNetwork -> {
                    showNoNetworkDialog { initAction() }
                }
                is SplashViewModel.SplashUiEvent.ShowButtonContinue -> {
                    showContinueButton()
                }
                is SplashViewModel.SplashUiEvent.ProgressTime -> {
                    viewBinding.splashProgressBar.start(
                        viewModel.maxSplashTime,
                        object : SplashProgressBar.ProgressCallback {
                            override fun onProgressCompleted() {
                                viewModel.isProgressCompleted = true
                                viewModel.checkAndShowContinueButton()
                            }
                        }
                    )
                }
                is SplashViewModel.SplashUiEvent.AutoOpen -> startNextActivity()
            }
        }
    }

    private fun initAction() {
        ConsentDialogManager.instance?.showDialogConsentMonkey(
            activity = this@BaseSplashActivity,
            object : ConsentDialogManager.ConsentDialogListener {
                override fun onConsentFormDismissed(state: ConsentDialogManager.ConsentDialogState) {
                    if (state == ConsentDialogManager.ConsentDialogState.ACCEPTED || state == ConsentDialogManager.ConsentDialogState.NOT_REQUIRED) {
                        initAndShowAd()
                        return
                    }
                    startNextActivity();
                }
            }
        )
    }

    private fun initAndShowAd() {
        // Start tracking RemoteConfig load and Ads
        viewModel.initLoadAds(this)
    }


    private fun showContinueButton() {
        val enableBtnContinue = RemoteConfigManager.instance?.enableBtnContinueSplash ?: true

        if (enableBtnContinue) {
            if (!viewModel.isProgressCompleted) {
                viewBinding.splashProgressBar.forceComplete()
            }
            // Show continue button as before
            viewBinding.layoutContinue.apply {
                visibility = View.VISIBLE
            }
            viewBinding.btnContinue.apply {
                setOnClickListener {
                    onContinueButtonClicked()
                }
            }
        } else {
            // Auto proceed after min time using ViewModel event
            viewModel.canNextScreenEvent.observe(this, Observer { canNext ->
                if (canNext) {
                    if (!viewModel.isProgressCompleted) {
                        viewBinding.splashProgressBar.forceComplete()
                    }
                    onContinueButtonClicked()
                    viewModel.canNextScreenEvent.removeObservers(this)
                }
            })
        }
    }

    private fun onContinueButtonClicked() {

        // Hide continue button
        viewBinding.btnContinue.visibility = View.GONE

        // Delegate decision to ViewModel
        viewModel.onContinueClicked()
    }

    private fun startNextActivity() {
        viewBinding.splashProgressBar.end()
        navigator.navigateTo(viewModel.getNavTarget())
    }

    private fun handleNotificationClick(intent: Intent?) {
        if (intent?.extras != null) {
            val notificationId = intent.getStringExtra("notification_id")
            if (notificationId != null) {
                val params = Bundle()
                params.putString("notification_id", notificationId)
                YNMAirBridge.getInstance().logCustomEvent("notification_clicked", "", notificationId)
                YNMAirBridge.getInstance().logCustomEvent("open_from_noti", "", notificationId)
                YNMAirBridge.getInstance().logCustomEvent("open_from_noti", "", notificationId)
                YNMAirBridge.setTagTest("noti")
            }
        }
    }

    private fun setDefaultLanguageBasedOnDeviceLocale() {
        // Get device locale
        var deviceLocale = getDeviceLanguage()
        // List of supported languages from LanguageModel
        val supportedLanguages = listOf("en", "es", "pt", "hi", "ar", "vi", "fr", "ja", "ko", "it")

        // Check if device language is supported
        val defaultLang = if (supportedLanguages.contains(deviceLocale)) {
            deviceLocale
        } else {
            "en" // Default to English if device language not supported
        }

        // Save the detected language as default (only if no language is already set)
        val currentSavedLang = SharedPref.readString(BaseConstants.LANG_CODE_STORE, "")
        if (currentSavedLang.isEmpty()) {
            SharedPref.saveString(BaseConstants.LANG_CODE_STORE, defaultLang)


            // Update locale and text immediately without recreating activity
            updateLocaleAndText(defaultLang)
        }
    }
    
    private fun updateLocaleAndText(language: String) {
        // Update locale for current activity
        val localizedContext = LocaleHelper.setLocale(this, language)

        // Update button text using localized context
        viewBinding.btnContinue.text = localizedContext.getString(R.string.continue_btn)
    }

    /**
     * Start shake animation for app name text to create emphasis
     * Animation repeats periodically to draw attention
     */
    fun startAppNameAnimation(view: GradientTextView) {
        Handler(Looper.getMainLooper()).postDelayed({
            animateAppName(view)
        }, SplashConstants.ANIMATION_INITIAL_DELAY_MS)
    }

    /**
     * Animate app name with shake effect and schedule next animation
     */
    private fun animateAppName(view: View) {
        AnimationUtils.bounceView(view) {
            Handler(Looper.getMainLooper()).postDelayed({
                if (!isFinishing && !isDestroyed) {
                    animateAppName(view)
                }
            }, SplashConstants.ANIMATION_REPEAT_INTERVAL_MS)
        }
    }

    override fun attachBaseContext(newBase: Context?) {
        val savedLang = SharedPref.readString(BaseConstants.LANG_CODE_STORE, "en")
        val context = if (newBase != null) LocaleHelper.setLocale(newBase, savedLang) else newBase
        super.attachBaseContext(context)
    }

    override fun onDestroy() {
        super.onDestroy()
        AdsNativeMultiPreload.destroyPreloadedAd(BaseConstants.NATIVE_SPLASH)
    }

    override fun onResume() {
        super.onResume()
    }

    fun getDeviceLanguage(): String {
        val locale: Locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Resources.getSystem().configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            Resources.getSystem().configuration.locale
        }
        return locale.language
    }

    /**
     * Provide onboarding pages data
     * Override this method in your app's SplashActivity to customize onboarding content
     *
     * @return List of DataPage for onboarding screens, or null to use default
     */
    protected open fun setOnboardingPages(list: List<DataPage>) {
        OnboardingActivity.setListDataPage(list)
    }

    protected abstract fun getLayoutContentActivity(): Int

    /**
     * Set the home screen activity class
     * Override this to specify which activity should be the home screen
     *
     * @param screenHome The Activity class to use as home screen
     */
    protected open fun setScreenHome(screenHome: Class<*>) {
        BaseNavigator.getInstance().setHomeActivity(screenHome)
    }


    /**
     * Set splash screen background
     * Supports multiple background types: drawable resource, color, or gradient
     *
     * @param view Target view to set background (default: root view)
     * @param backgroundType Type of background
     * @param resourceId Drawable resource ID (for DRAWABLE type)
     * @param colorStart Start color (for COLOR or GRADIENT type)
     * @param colorEnd End color (for GRADIENT type, optional)
     * @param orientation Gradient orientation (default: TOP_BOTTOM)
     */
    fun setBackgroundView(
        view: View? = null,
        backgroundType: BackgroundType = BackgroundType.DRAWABLE,
        resourceId: Int? = null,
        colorStart: Int? = null,
        colorEnd: Int? = null,
        orientation: android.graphics.drawable.GradientDrawable.Orientation =
            android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM
    ) {
        try {
            val targetView = view ?: viewBinding.view

            when (backgroundType) {
                BackgroundType.DRAWABLE -> {
                    // Set background from drawable resource
                    resourceId?.let {
                        targetView.setBackgroundResource(it)
                    } ?: run {
                        Logger.w( "Drawable resource ID is null")
                    }
                }

                BackgroundType.COLOR -> {
                    // Set solid color background
                    colorStart?.let {
                        targetView.setBackgroundColor(it)
                    } ?: run {
                        Logger.w( "Color value is null")
                    }
                }

                BackgroundType.GRADIENT -> {
                    // Set gradient background
                    if (colorStart != null && colorEnd != null) {
                        val gradientDrawable = android.graphics.drawable.GradientDrawable(
                            orientation,
                            intArrayOf(colorStart, colorEnd)
                        )
                        targetView.background = gradientDrawable
                    } else {
                        Logger.w("Gradient colors are null")
                    }
                }
            }
        } catch (e: Exception) {
            Logger.e("Error setting background", e)
        }
    }
}

