package com.jrm.base

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import co.ab180.airbridge.Airbridge
import com.ads.nomyek_admob.admobs.Admob
import com.ads.nomyek_admob.admobs.AppOpenManager
import com.ads.nomyek_admob.ads_components.YNMAds
import com.ads.nomyek_admob.application.AdsApplication
import com.ads.nomyek_admob.config.AirBridgeConfig
import com.ads.nomyek_admob.config.YNMAdsConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.messaging.FirebaseMessaging
import com.jrm.BuildConfig
import com.jrm.onboarding.language.Language2Activity
import com.jrm.onboarding.language.LanguageActivity
import com.jrm.onboarding.onboarding.OnboardingActivity
import com.jrm.onboarding.splash.BaseSplashActivity
import com.jrm.utils.BaseConstants
import com.jrm.utils.BaseUtils
import com.jrm.utils.Logger
import com.jrm.utils.SharedPref
import com.jrm.utils.remote_config.RemoteConfigManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Base Application class that provides core functionality for the app including:
 * - Firebase initialization and analytics
 * - Ad network configuration (Admob, YNMAds)
 * - Airbridge integration for attribution
 * - FCM token management
 * - Activity lifecycle tracking
 *
 * Subclasses must implement [tokenAirBridge] and [appNameAirBridge] to provide
 * app-specific Airbridge configuration.
 */
abstract class BaseApplication : AdsApplication(), Application.ActivityLifecycleCallbacks {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // region Properties
    @VisibleForTesting
    internal var activeActivitiesCount = 0
        private set

    private val isInForeground: Boolean
        get() = activeActivitiesCount > 0
    // endregion

    // region Lifecycle Methods
    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)
        runCatching {
            initializeCore()
            initializeFirebase()
            initializeAds()
            tryRunYnmInitIfReady()
        }.onFailure { exception ->
            Logger.e("Error during application initialization", exception)
        }
    }

    /**
     * Called only in emulator or when process is explicitly killed; not called on real devices.
     * Cancel application scope so pending coroutines are cleaned up when we do get this callback.
     * On real devices the process can be killed anytime without callback; the scope is then
     * reclaimed with the process.
     */
    override fun onTerminate() {
        unregisterActivityLifecycleCallbacks(this)
        applicationScope.cancel()
        super.onTerminate()
    }
    // endregion

    // region Activity Lifecycle Callbacks
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        activeActivitiesCount++
        if (activeActivitiesCount == 1) {
            onAppMovedToForeground()
        }
    }

    override fun onActivityStarted(activity: Activity) {
        // No-op: Can be overridden by subclasses if needed
    }

    override fun onActivityResumed(activity: Activity) {
        // No-op: Can be overridden by subclasses if needed
    }

    override fun onActivityPaused(activity: Activity) {
        // No-op: Can be overridden by subclasses if needed
    }

    override fun onActivityStopped(activity: Activity) {
        activeActivitiesCount--
        if (activeActivitiesCount == 0) {
            onAppMovedToBackground()
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        // No-op: Can be overridden by subclasses if needed
    }

    override fun onActivityDestroyed(activity: Activity) {
        // No-op: Can be overridden by subclasses if needed
    }
    // endregion

    // region Initialization Methods
    /**
     * Initialize core dependencies like SharedPreferences and RemoteConfig
     */
    private fun initializeCore() {
        SharedPref.init(this)
        RemoteConfigManager.instance?.init(context = this)
        BaseEventLogger.initialize(this)
    }

    /**
     * Initialize Firebase services including FirebaseApp, Analytics, and FCM
     */
    private fun initializeFirebase() {
        FirebaseApp.initializeApp(this)
        initializeContext(this)
        initFirebaseAnalytics(this)
        fetchFcmToken()
    }

    /**
     * Initialize and configure the ads SDK (config only). YNMAds.init() is deferred to
     * [tryRunYnmInitIfReady] when the first Activity exists, to avoid NPE in AppOpenManager lifecycle.
     */
    private fun initializeAds() {
        SharedPref.saveBoolean(BaseConstants.ENABLE_ADS, true)
        val adsConfig = createAdsConfig()
        this.ynmAdsConfig = adsConfig
        configureAdTracking()
        applicationScope.launch {
            if (RemoteConfigManager.instance?.loadConfigCallback(this@BaseApplication) == true) {
                this@BaseApplication.ynmAdsConfig.intervalInterstitialAd =
                    (RemoteConfigManager.instance?.adConfig?.configs?.timeInterstitialCooldown
                        ?: 15).toInt() * 1000
            }
        }
    }

    /**
     * Runs on Main thread. Calls YNMAds.init() + Airbridge + configureAdBehavior only when
     * we have at least one Activity, so AppOpenManager's lifecycle observer does not NPE.
     */
    private fun tryRunYnmInitIfReady() {
        runCatching {
            YNMAds.getInstance().init(null, this, this.ynmAdsConfig)
            initializeAirbridge()
            configureAdBehavior()
        }.onFailure { exception ->
            Logger.e("Error during YNMAds init", exception)
        }
    }

    private fun runOnMainThread(block: () -> Unit) {
        android.os.Handler(android.os.Looper.getMainLooper()).post(block)
    }

    /**
     * Create and configure YNMAds configuration
     */
    private fun createAdsConfig(): YNMAdsConfig {
        val environment = if (BuildConfig.env_dev) {
            YNMAdsConfig.ENVIRONMENT_DEVELOP
        } else {
            YNMAdsConfig.ENVIRONMENT_PRODUCTION
        }

        return YNMAdsConfig(this, YNMAdsConfig.PROVIDER_ADMOB, environment).apply {
            applicationScope.launch {
                if (RemoteConfigManager.instance?.loadConfigCallback(this@BaseApplication) == true) {
                    val unitIdConfig =
                        RemoteConfigManager.instance?.idRegistry?.apps?.get(0)?.unitIds?.filter { it.unitId == "403_resume_open" }
                            ?.getOrNull(0)
                    if (unitIdConfig != null)
                        idAdResume = if (BuildConfig.DEBUG) {
                            unitIdConfig.unitIdTest
                        } else unitIdConfig.unitId

                }
            }
            listDeviceTest = mutableListOf(TEST_DEVICE_ID).also {
                listTestDevice.addAll(it)
            }
            intervalInterstitialAd = INTERSTITIAL_AD_INTERVAL
            maxKey = BuildConfig.key_max
            setAdTrackingList(
                listOf(
                    YNMAdsConfig.AdItem(BuildConfig._102_spl_native, AD_TRACKING_ITEM_KEY)
                )
            )
        }
    }

    /**
     * Configure ad tracking groups for analytics
     */
    private fun configureAdTracking() {
        YNMAdsConfig.AD_TRACKING_GROUPS = mapOf(
            SPLASH_HIGHFLOOR_PASS to listOf(
                BaseConstants.INTER_SPLASH_HIGHFLOOR,
                BaseConstants.NATIVE_SPLASH_HIGHFLOOR
            ), LANGUAGE_HIGHFLOOR_PASS to listOf(
                BaseConstants.NATIVE_LANGUAGE2_HIGHFLOOR
            ), ONBOARD_HIGHFLOOR_PASS to listOf(
                BaseConstants.NATIVE_OB1_HIGHFLOOR
            )
        )
    }

    /**
     * Initialize Airbridge for attribution tracking
     */
    private fun initializeAirbridge() {
        val airBridgeConfig = AirBridgeConfig().apply {
            isEnableAirBridge = true
            appNameAirBridge = this@BaseApplication.appNameAirBridge()
            tokenAirBridge = this@BaseApplication.tokenAirBridge()
            userState = BaseUtils.getUserState()
        }

        this.ynmAdsConfig.airBridgeConfig = airBridgeConfig
        BaseUtils.setFirstOpenApp(false)
    }

    /**
     * Configure ad behavior and exclusions for specific activities
     */
    private fun configureAdBehavior() {
        with(Admob.getInstance()) {
            isDisableAdResumeWhenClickAds = true
            setOpenActivityAfterShowInterAds(true)
        }

        with(AppOpenManager.getInstance()) {
            disableAppResumeWithActivity(BaseSplashActivity::class.java)
            disableAppResumeWithActivity(LanguageActivity::class.java)
            disableAppResumeWithActivity(Language2Activity::class.java)
            disableAppResumeWithActivity(OnboardingActivity::class.java)
        }
    }

// endregion

// region App State Callbacks
    /**
     * Called when app moves to foreground (first activity created)
     */
    protected open fun onAppMovedToForeground() {
        Logger.d("App moved to foreground")
        // Subclasses can override to add custom behavior
    }

    /**
     * Called when app moves to background (all activities stopped)
     */
    protected open fun onAppMovedToBackground() {
        Logger.d("App moved to background")
        YNMAds.getInstance().setInitCallback {
            BaseEventLogger.logCustomEvent(EVENT_LEAVE_APP)
        }
    }
// endregion

// region Abstract Methods
    /**
     * Provide the Airbridge token for this app
     * @return Airbridge token string
     */
    abstract fun tokenAirBridge(): String

    /**
     * Provide the Airbridge app name for this app
     * @return Airbridge app name
     */
    abstract fun appNameAirBridge(): String
// endregion

    // region Companion Object
    companion object {

        private const val TAG = "BaseApplication"
        private const val FCM_TOKEN_KEY = "pushedFCM"
        private const val TEST_DEVICE_ID = "6E865A9E874E712EADB42A6D03ACC501"
        private const val INTERSTITIAL_AD_INTERVAL = 25
        private const val AD_TRACKING_ITEM_KEY = "inter_splash_highfloor"

        // Tracking group keys
        private const val SPLASH_HIGHFLOOR_PASS = "splash_highfloor_pass"
        private const val LANGUAGE_HIGHFLOOR_PASS = "language_highfloor_pass"
        private const val ONBOARD_HIGHFLOOR_PASS = "onboard_highfloor_pass"

        // Event names
        private const val EVENT_LEAVE_APP = "leave_app"
        private val firebaseAnalytics: FirebaseAnalytics by lazy {
            throw IllegalStateException("Firebase Analytics not initialized. Call initFirebaseAnalytics() first.")
        }

        @Volatile
        private var firebaseAnalyticsInstance: FirebaseAnalytics? = null

        @Volatile
        private var contextApp: AdsApplication? = null

        /**
         * Get Firebase Analytics instance
         * @return FirebaseAnalytics instance
         * @throws IllegalStateException if not initialized
         */
        @JvmStatic
        fun getFireBaseAnalytic(): FirebaseAnalytics {
            return firebaseAnalyticsInstance
                ?: throw IllegalStateException("Firebase Analytics not initialized")
        }

        /**
         * Get application context
         * @return AdsApplication context
         * @throws IllegalStateException if not initialized
         */
        @JvmStatic
        fun getContext(): AdsApplication {
            return contextApp ?: throw IllegalStateException("Application context not initialized")
        }

        /**
         * Initialize application context
         * @param context The application context to store
         */
        @JvmStatic
        private fun initializeContext(context: AdsApplication) {
            contextApp = context
        }

        /**
         * Initialize Firebase Analytics
         * @param context Application context
         * @return FirebaseAnalytics instance
         */
        private fun initFirebaseAnalytics(context: Context): FirebaseAnalytics {
            if (firebaseAnalyticsInstance == null) {
                synchronized(this) {
                    if (firebaseAnalyticsInstance == null) {
                        firebaseAnalyticsInstance = FirebaseAnalytics.getInstance(context)
                    }
                }
            }
            return firebaseAnalyticsInstance!!
        }

        /**
         * Fetch FCM token and register with Airbridge
         * Handles token persistence to avoid duplicate API calls
         */
        @JvmStatic
        private fun fetchFcmToken() {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Logger.w("Fetching FCM registration token failed", task.exception)
                    return@addOnCompleteListener
                }

                task.result?.let { token ->
                    Logger.d("FCM Token retrieved: $token")

                    // Register token with Airbridge
                    runCatching {
                        Airbridge.registerPushToken(token)
                    }.onFailure { exception ->
                        Logger.e("Failed to register push token with Airbridge", exception)
                    }

                    // Save token if not already saved
                    val context = contextApp
                    if (context != null && !SharedPref.readBoolean(FCM_TOKEN_KEY, false)) {
                        SharedPref.saveBoolean(FCM_TOKEN_KEY, true)
                    }
                } ?: Logger.w("FCM token is null")
            }
        }

    }
// endregion

}