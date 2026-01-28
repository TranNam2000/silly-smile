package com.jrm.onboarding.splash

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageView
import com.ads.nomyek_admob.ads_components.YNMAds
import com.ads.nomyek_admob.ads_components.YNMInitCallback
import com.ads.nomyek_admob.event.YNMAirBridge
import com.ads.nomyek_admob.utils.AdsNativeMultiPreload
import com.jrm.base.BaseActivity
import com.jrm.onboarding.consent_dialog.ConsentDialogManager
import com.jrm.onboarding.language.Language2Activity
import com.jrm.onboarding.language.LanguageActivity
import com.jrm.utils.remote_config.RemoteConfigManager
import com.jrm.R
import com.jrm.base.BaseEventLogger
import com.jrm.databinding.ActivitySplashScreenBinding
import com.jrm.utils.BaseConstants
import com.jrm.utils.InternetUtil
import com.jrm.utils.LocaleHelper
import com.jrm.utils.SharedPref
import com.jrm.utils.BaseUtils
import com.jrm.utils.AdsHelper
import com.jrm.utils.purchase.IAPHelper
import com.jrm.onboarding.navigation.BaseNavigator
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.jrm.ads.WaterfallNativeAdManager
import com.jrm.onboarding.onboarding.OnboardingActivity
import java.util.Locale
import com.jrm.utils.AnimationUtils
import com.jrm.utils.BaseExtension
import com.jrm.utils.WaterfallManager

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity<ActivitySplashScreenBinding>() {

    private var isStartNextActivityCalled = false
    private val canNextScreen = MutableLiveData(false)
    private var timeOutLoading: Long = 15000

    // Tracking conditions for showing continue button
    private var isSplashNativeLoaded = false
    private var isSplashInterPreloaded = false
//    private var isL1NativePreloaded = false
    private var isL1L2Ob1NativePreloaded = false

    private var isL1HighLoaded = false
    private var isL2HighLoaded = false
    private var isOb1HighLoaded = false
    private var isProgressCompleted = false
    private var canShowContinueButton = false
    private var isNativeSplashClicked = false
    
    // Handler để quản lý timeout
    private val timeoutHandler = Handler(Looper.getMainLooper())
    private val timeoutRunnable = Runnable {
        BaseEventLogger.logCustomEvent("open_splash_timeout")
        onTimeoutReached()
    }
    
    // Time tracking variables
    private var timeInitActionStart: Long = 0
    private var timeNetworkCheckStart: Long = 0
    private var timeConsentDialogStart: Long = 0
    private var timeRemoteConfigLoadStart: Long = 0
    private var timeYNMAdsInitStart: Long = 0
    private var disableInterSplash: Boolean = false
    private var disableAdsSplash: Boolean = false

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
        initDefine()
        initAction()
        BaseEventLogger.logCustomEvent("app_open")

        // Set default language based on device locale if first time opening
        if (!BaseUtils.isFinishObd()) {
            setDefaultLanguageBasedOnDeviceLocale()
        }
        // Apply shake animation to app name text for emphasis
        startAppNameAnimation()

        // Theo dõi click vào thông báo
        if (intent.extras != null) {
            handleNotificationClick(intent)
        }
        // Load GIF using Glide
        RemoteConfigManager.instance!!.loadTimeOutSplash(this, object : RemoteConfigManager.NumberCallback {
            override fun onResult(value: Long) {
                BaseUtils.checkAndPushRetentionEvents(this@SplashActivity)
                timeOutLoading = value
                // Start progress bar with callback
                viewBinding?.splashProgressBar?.start(timeOutLoading.toLong(), object : SplashProgressBar.ProgressCallback {
                    override fun onProgressCompleted() {
                        isProgressCompleted = true
                        checkAndShowContinueButton()
                    }
                })
                // Set timeout
                timeoutHandler.postDelayed(timeoutRunnable, timeOutLoading.toLong())

                // Set min time splash
                val minTimeSplash = RemoteConfigManager.instance?.minTimeSplash ?: 8000
                Handler(Looper.getMainLooper()).postDelayed({
                    canNextScreen.postValue(true)
                }, minTimeSplash)
            }
        })
    }

    private fun initDefine() {
        AdsHelper.setUpSplashApp()
    }

    private fun initAction() {
        timeInitActionStart = System.currentTimeMillis()
        Log.d("IAPNE", "IAP: ${IAPHelper.isPremium()}")
        
        // Start tracking network check
        timeNetworkCheckStart = System.currentTimeMillis()
        val networkAvailable = InternetUtil.isNetworkAvailable(this)
        val currentTime = System.currentTimeMillis()
        
        // Log network check duration
        val networkCheckDuration = currentTime - timeNetworkCheckStart
        logTimeEvent("splash_network_check_completed", 
            currentTime, 
            networkCheckDuration,
            mapOf("network_available" to networkAvailable.toString()))
        
        if (!networkAvailable) {
            showNoNetworkDialog()
            return
        }
        
        // Start tracking consent dialog
        timeConsentDialogStart = System.currentTimeMillis()
        
        ConsentDialogManager.instance!!.showDialogConsentMonkey(
            this,
            object : ConsentDialogManager.ConsentDialogListener {
                override fun onConsentFormDismissed(state: ConsentDialogManager.ConsentDialogState) {
                    val currentTime = System.currentTimeMillis()
                    
                    // Log consent dialog duration
                    val consentDialogDuration = currentTime - timeConsentDialogStart
                    logTimeEvent("splash_consent_dialog_completed", 
                        currentTime, 
                        consentDialogDuration,
                        mapOf("consent_state" to state.toString()))
                    
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
        // Start tracking RemoteConfig load
        timeRemoteConfigLoadStart = System.currentTimeMillis()
        
        RemoteConfigManager.instance!!.loadTimeOutSplash(this, object : RemoteConfigManager.NumberCallback {
            override fun onResult(value: Long) {
                disableInterSplash = AdsHelper.isDisableInterSplash()
                disableAdsSplash = AdsHelper.isDisableAdsSplash()
                val currentTime = System.currentTimeMillis()
                // Log RemoteConfig load duration
                val remoteConfigDuration = currentTime - timeRemoteConfigLoadStart
                logTimeEvent("splash_remote_config_load_completed", 
                    currentTime, 
                    remoteConfigDuration,
                    mapOf("timeout_value" to value.toString()))
                
                Thread {
                    // Initialize the Google Mobile Ads SDK on a background thread.
                    runOnUiThread {
                        // Preload native ads for splash screen
                        if (!AdsHelper.isDisableObdAd()) {
                            // Start tracking YNMAds init
                            timeYNMAdsInitStart = System.currentTimeMillis()

                            YNMAds.getInstance().setInitCallback(YNMInitCallback {
                                val currentTime = System.currentTimeMillis()

                                // Log YNMAds init duration
                                val ynmAdsInitDuration = currentTime - timeYNMAdsInitStart
                                logTimeEvent("splash_ynmads_init_completed",
                                    currentTime,
                                    ynmAdsInitDuration,
                                    mapOf("ads_disabled" to "false"))

                                if (!disableAdsSplash) {
                                    if (RemoteConfigManager.instance!!.formatTypeSplash.compareTo("native") == 0) {
                                        viewBinding.nativeOnboarding.visibility = View.VISIBLE
                                        showSplashNativeAds()
                                    } else {
                                        viewBinding.bannerView.visibility = View.VISIBLE
                                        setListBannerId(listOf(RemoteConfigManager.instance!!.getBannerHighSplAdId(), RemoteConfigManager.instance!!.getBannerSplAdId()))
                                        setRefreshBannerTime(RemoteConfigManager.instance!!.timeReloadBanner.toInt())
                                        showRefreshMultiIdBanner()
                                    }
                                }

                                // Show interstitial ads
                                preloadSplashInterstial()

                                if (!BaseUtils.isFinishObd()) {
                                    preloadL1NativeAds()
                                } else {
                                    isL1L2Ob1NativePreloaded = true
                                    checkAndShowContinueButton()
                                }
                            })
                        } else {
                            val currentTime = System.currentTimeMillis()

                            // Log when ads are disabled
                            logTimeEvent("splash_ynmads_init_completed",
                                currentTime,
                                0,
                                mapOf("ads_disabled" to "true"))

                            isSplashNativeLoaded = true;
                            isSplashInterPreloaded = true;
                            isL1L2Ob1NativePreloaded = true
                            checkAndShowContinueButton()
                        }
                    }
                }.start()
            }
        })
    }

    private fun showSplashNativeAds() {
        isNativeSplashClicked = false
        WaterfallNativeAdManager.preload(
            context = this,
            activityName = activityName,
            adPlace = BaseConstants.NATIVE_SPLASH,
            configString = RemoteConfigManager.instance!!.nativeSplIds,
            layoutAdmob = R.layout.custom_native_admob_large,
            layoutMax = R.layout.custom_native_admob_large_max
        ) { result ->
            WaterfallNativeAdManager.show(
                this@SplashActivity,
                adView = viewBinding.nativeOnboarding,
                adPlace = BaseConstants.NATIVE_SPLASH,
                waitForLoad = true // true: chờ nếu đang loading, false: fail ngay
            )
        }
    }

    private fun preloadL1NativeAds() {
        WaterfallNativeAdManager.preload(
            context = this,
            activityName = activityName,
            adPlace = BaseConstants.NATIVE_LANGUAGE1,
            configString = RemoteConfigManager.instance!!.nativeL1Ids,
            layoutAdmob = R.layout.custom_native_admob_large_language,
            layoutMax = R.layout.custom_native_admob_large_language_max
        ) { result ->
            if (result.success) {
                // Ad loaded - result.adType = "max_native" hoặc "admob_native"
                Log.d("SplashActivity", "L1 Native ad loaded successfully")
                isL1L2Ob1NativePreloaded = true
                isL1HighLoaded = true
                checkAndShowContinueButton()
            } else {
                // Tất cả ads đều fail, thử waterfall tiếp theo
                preloadL2NativeAds()
            }
        }
    }

    private fun preloadObd1() {
        WaterfallNativeAdManager.preload(
            context = this,
            activityName = activityName,
            adPlace = BaseConstants.NATIVE_ONBOARD_1,
            configString = RemoteConfigManager.instance!!.nativeObd1Ids,
            layoutAdmob = R.layout.custom_native_admob_large,
            layoutMax = R.layout.custom_native_admob_large_max
        ) { result ->
            if (result.success) {
                isL1L2Ob1NativePreloaded = true;
                isOb1HighLoaded = true
                checkAndShowContinueButton()
            } else {
                isL1L2Ob1NativePreloaded = true;
                checkAndShowContinueButton()
            }
        }
    }

    private fun preloadL2NativeAds() {
        WaterfallNativeAdManager.preload(
            context = this,
            activityName = activityName,
            adPlace = BaseConstants.NATIVE_LANGUAGE2,
            configString = RemoteConfigManager.instance!!.nativeL2Ids,
            layoutAdmob = R.layout.custom_native_admob_large_language,
            layoutMax = R.layout.custom_native_admob_large_language_max
        ) { result ->
            if (result.success) {
                // Ad loaded - result.adType = "max_native" hoặc "admob_native"
                Log.d("SplashActivity", "L1 Native ad loaded successfully")
                isL1L2Ob1NativePreloaded = true;
                isL2HighLoaded = true
                Language2Activity.adPreloadIsLoading.postValue(false)
                Language2Activity.isLoadDoneSplash = true;
                checkAndShowContinueButton()
            } else {
                // Tất cả ads đều fail, thử waterfall tiếp theo
                preloadObd1()
            }
        }
    }

    private fun preloadSplashInterstial() {
        if (disableInterSplash || disableAdsSplash) {
            isSplashInterPreloaded = true
            checkAndShowContinueButton()
            return
        }
        
        // Get waterfall config from Remote Config
        val waterfallConfig = RemoteConfigManager.instance?. fullScreenSplashWaterfall ?: ""
        
        if (waterfallConfig.isBlank()) {
            Log.w("SplashActivity", "No waterfall config found, marking as preloaded")
            isSplashInterPreloaded = true
            checkAndShowContinueButton()
            return
        }
        
        // Preload using WaterfallManager
        WaterfallManager.preload(
            context = this@SplashActivity,
            activityName = activityName,
            adPlace = BaseConstants.INTER_SPLASH,
            configString = waterfallConfig
        ) { result ->
            Log.d("SplashActivity", "Waterfall preload result: success=${result.success}, format=${result.format}")
            isSplashInterPreloaded = true
            checkAndShowContinueButton()
        }
    }

    private fun onTimeoutReached() {
        Log.d("SplashActivity", "Timeout reached")
        checkAndShowContinueButton()
    }
    
    private fun checkAndShowContinueButton() {
        Log.d("SplashActivity", "Checking conditions - Progress: $isProgressCompleted, Native: $isSplashNativeLoaded, Inter: $isSplashInterPreloaded, L1: $isL1L2Ob1NativePreloaded")
        
        // Check if we should show continue button
        val shouldShowButton = isProgressCompleted || 
                (isSplashNativeLoaded && isSplashInterPreloaded && isL1L2Ob1NativePreloaded)

        Log.d("SplashActivity", "shouldShowButton: $shouldShowButton isProgressCompleted: $isProgressCompleted isSplashNativeLoaded: $isSplashNativeLoaded isSplashInterPreloaded: $isSplashInterPreloaded isL1NativePreloaded: $isL1L2Ob1NativePreloaded")
        
        if (shouldShowButton && !canShowContinueButton) {
            canShowContinueButton = true
            showContinueButton()
        }
    }
    
    private fun showContinueButton() {
        Log.d("SplashActivity", "Showing continue button")
        
        // Force complete progress bar if not already completed
       
        
        val enableBtnContinue = RemoteConfigManager.instance?.enableBtnContinueSplash ?: true
        
        if (enableBtnContinue) {
            if (!isProgressCompleted) {
                viewBinding?.splashProgressBar?.forceComplete()
            }
            // Show continue button as before
            viewBinding?.layoutContinue?.apply {
                visibility = View.VISIBLE
            }
            viewBinding?.btnContinue?.apply {
                setOnClickListener {
                    onContinueButtonClicked()
                }
            }
        } else {
            // Auto proceed after min time using canNextScreen
            canNextScreen.observe(this, Observer { canNext ->
                if (canNext) {
                    if (!isProgressCompleted) {
                        viewBinding?.splashProgressBar?.forceComplete()
                    }
                    Log.d("SplashActivity", "Min time passed, auto proceed")
                    onContinueButtonClicked()
                    canNextScreen.removeObservers(this)
                }
            })
        }
    }
    
    private fun onContinueButtonClicked() {
        Log.d("SplashActivity", "Continue button clicked")

        // Hide continue button
        viewBinding?.btnContinue?.visibility = View.GONE

        // Show interstitial ad
        if (!AdsHelper.isDisableObdAd()) {
            showSplashInterstitialAd()
        } else {
            startNextActivity()
        }
    }
    
    private fun showSplashInterstitialAd() {
        if (disableInterSplash || disableAdsSplash) {
            startNextActivity()
            isStartNextActivityCalled = true
            return
        }
        
        // Check if waterfall ad is preloaded
        if (!WaterfallManager.isPreloaded(adPlace = BaseConstants.INTER_SPLASH,)) {
            Log.w("SplashActivity", "No waterfall ad preloaded, proceeding to next activity")
            startNextActivity()
            isStartNextActivityCalled = true
            return
        }
        
        // Show preloaded waterfall ad
        WaterfallManager.showPreload(
            activity = this@SplashActivity,
            activityName = activityName,
            adPlace = BaseConstants.INTER_SPLASH
        ) { isShown ->
            Log.d("SplashActivity", "Waterfall ad shown: $isShown")
            if (!isStartNextActivityCalled) {
                startNextActivity()
                isStartNextActivityCalled = true
            }
        }
    }

    private fun startNextActivity() {
        if (isStartNextActivityCalled) return
        isStartNextActivityCalled = true

        viewBinding?.splashProgressBar?.end()

        // Check if opened from Word of the Day

        // Original logic if not from Word of the Day
        RemoteConfigManager.instance!!.loadConfigCallback(this,object : RemoteConfigManager.BooleanCallback {
            override fun onResult(value: Boolean) {
                if (!BaseUtils.isFinishObd() && !AdsHelper.isDisableObdAd()) {
                    if (isL1HighLoaded) {
                        val mainIntent = Intent(this@SplashActivity, LanguageActivity::class.java)
                        startActivity(mainIntent)
                    } else {
                        if (isL2HighLoaded) {
                            val mainIntent = Intent(this@SplashActivity, Language2Activity::class.java)
                            startActivity(mainIntent)
                        } else {
                            BaseExtension.showActivity(this@SplashActivity, OnboardingActivity::class.java, null)

                        }
                    }
                } else {
                    BaseNavigator.getInstance()?.navigateToHome(this@SplashActivity)
                }
            }
        })
    }


    private fun showNoNetworkDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("No Internet Connection")
            .setMessage("Please check your internet connection and try again")
            .setCancelable(false)
            .setPositiveButton("Retry") { _: DialogInterface, _: Int ->
                // Kiểm tra lại kết nối khi nhấn Thử lại
                if (InternetUtil.isNetworkAvailable(this@SplashActivity)) {
                    initAction()
                } else {
                    // Vẫn không có mạng, hiển thị lại popup
                    showNoNetworkDialog()
                }
            }

        val dialog = builder.create()
        dialog.show()
    }

    private fun handleNotificationClick(intent: Intent?) {
        if (intent?.extras != null) {
            val notificationId = intent.getStringExtra("notification_id")
            if (notificationId != null) {
                val params = Bundle()
                params.putString("notification_id", notificationId)
                YNMAirBridge.getInstance().logCustomEvent("notification_clicked", "", notificationId)
                YNMAirBridge.getInstance().logCustomEvent("open_from_noti", "", notificationId)
                Log.d("NOTI", "tu noti ne: $notificationId")
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

            Log.d("SplashActivity", "Default language set to: $defaultLang based on device locale: ${Locale.getDefault().language}")

            // Update locale and text immediately without recreating activity
            updateLocaleAndText(defaultLang)
        }
    }
    
    private fun updateLocaleAndText(language: String) {
        // Update locale for current activity
        val localizedContext = LocaleHelper.setLocale(this, language)
        
        // Update button text using localized context
        viewBinding?.btnContinue?.text = localizedContext.getString(R.string.continue_btn)
        
        Log.d("SplashActivity", "Updated locale and text to language: $language")
    }

    /**
     * Start shake animation for app name text to create emphasis
     * Animation repeats periodically to draw attention
     */
    private fun startAppNameAnimation() {
        viewBinding?.appNameText?.let { appNameView ->
            // Initial delay before first animation
            Handler(Looper.getMainLooper()).postDelayed({
                animateAppName(appNameView)
            }, 500) // Start after 500ms
        }
    }

    /**
     * Animate app name with shake effect and schedule next animation
     */
    private fun animateAppName(view: View) {
        // Apply shake animation
        AnimationUtils.bounceView(view) {
            // Schedule next animation after a delay
            Handler(Looper.getMainLooper()).postDelayed({
                // Only continue animation if activity is not finishing
                if (!isFinishing && !isDestroyed) {
                    animateAppName(view)
                }
            }, 3000) // Repeat every 3 seconds
        }
    }

    override fun attachBaseContext(newBase: Context?) {
        val savedLang = SharedPref.readString(BaseConstants.LANG_CODE_STORE, "en")
        val context = if (newBase != null) LocaleHelper.setLocale(newBase, savedLang) else newBase
        super.attachBaseContext(context)
    }

    override fun onDestroy() {
        // Hủy timeout handler khi activity bị destroy
        timeoutHandler.removeCallbacks(timeoutRunnable)
        super.onDestroy()
        AdsNativeMultiPreload.destroyPreloadedAd(BaseConstants.NATIVE_SPLASH)
    }

    override fun onResume() {
        super.onResume()
        if (isNativeSplashClicked && !isStartNextActivityCalled) {
            showSplashNativeAds();
        }
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
        
        // Convert to seconds with 2 decimal places
        val durationSeconds = duration / 1000.0
        val timeFromAppStartSeconds = currentTimeFromAppStart / 1000.0
        
        val logMessage = StringBuilder()
        logMessage.append("Event: $eventName")
        logMessage.append(" | Duration: ${String.format("%.2f", durationSeconds)}s")
        logMessage.append(" | Time from app start: ${String.format("%.2f", timeFromAppStartSeconds)}s")
        
        extraParams.forEach { (key, value) ->
            logMessage.append(" | $key: $value")
        }
        
        Log.d("SplashTimeTracking", logMessage.toString())
        
        // Create event parameters with correct type Map<String, Any?>
        val eventParams = mutableMapOf<String, Any?>()
        eventParams["duration_seconds"] = durationSeconds
        eventParams["time_from_app_start_seconds"] = timeFromAppStartSeconds
        
        // Add extra parameters
        extraParams.forEach { (key, value) ->
            eventParams[key] = value
        }
        
        // Log to Firebase Analytics via BaseEventLogger
        try {
            BaseEventLogger.logCustomEvent(eventName, custom = eventParams)
        } catch (e: Exception) {
            Log.e("SplashTimeTracking", "Failed to log event: $eventName", e)
        }
    }
}
