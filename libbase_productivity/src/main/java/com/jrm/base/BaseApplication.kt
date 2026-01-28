package com.jrm.base

import android.content.ContentValues.TAG
import android.content.Context
import android.util.Log
import co.ab180.airbridge.Airbridge
import com.ads.nomyek_admob.admobs.Admob
import com.ads.nomyek_admob.admobs.AppOpenManager
import com.ads.nomyek_admob.ads_components.YNMAds
import com.ads.nomyek_admob.application.AdsApplication
import com.ads.nomyek_admob.config.AirBridgeConfig
import com.ads.nomyek_admob.config.YNMAdsConfig
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.messaging.FirebaseMessaging
import com.jrm.onboarding.language.Language2Activity
import com.jrm.onboarding.language.LanguageActivity
import com.jrm.onboarding.onboarding.OnboardingActivity
import com.jrm.onboarding.splash.SplashActivity
import com.jrm.utils.BaseConstants
import com.jrm.utils.BaseUtils
import com.jrm.utils.SharedPref
import com.jrm.utils.remote_config.RemoteConfigManager

abstract class BaseApplication: AdsApplication() {
    override fun onCreate() {
        super.onCreate()
        SharedPref.init(this)
        FirebaseApp.initializeApp(this)
        fcmTestGetKey()
        initializeContext(this)
        initFirebaseAnalytics(this)
        SharedPref.saveBoolean(BaseConstants.ENABLE_ADS, true)
        BaseEventLogger.initialize(this)
        RemoteConfigManager.instance?.loadRemote(this)

        val environment =
            if (com.jrm.BuildConfig.env_dev) YNMAdsConfig.ENVIRONMENT_DEVELOP else YNMAdsConfig.ENVIRONMENT_PRODUCTION
        this.ynmAdsConfig = YNMAdsConfig(this, YNMAdsConfig.PROVIDER_ADMOB, environment)

        // Optional: setup Airbridge
        val airBridgeConfig = AirBridgeConfig()
        airBridgeConfig.isEnableAirBridge = true
        airBridgeConfig.appNameAirBridge = "sillysmilewallpaper"
        airBridgeConfig.tokenAirBridge = "2b74009ef07e4449a23a95ea1b981f32"
        airBridgeConfig.userState = BaseUtils.getUserState();
        BaseUtils.setFirstOpenApp(false);
        this.ynmAdsConfig.airBridgeConfig = airBridgeConfig

        // Optional: enable ads resume
        this.ynmAdsConfig.idAdResume = com.jrm.BuildConfig._403_resume_open

        // Optional: setup list device test - recommended to use
        this.listTestDevice.add("6E865A9E874E712EADB42A6D03ACC501")
        this.ynmAdsConfig.listDeviceTest = this.listTestDevice
        this.ynmAdsConfig.intervalInterstitialAd = 25
        this.ynmAdsConfig.maxKey = com.jrm.BuildConfig.key_max
        this.ynmAdsConfig.setAdTrackingList(
            listOf(
                YNMAdsConfig.AdItem(com.jrm.BuildConfig._102_spl_native, "inter_splash_highfloor"),
            )
        )

        YNMAdsConfig.AD_TRACKING_GROUPS = mapOf(
            "splash_highfloor_pass" to listOf(
                BaseConstants.INTER_SPLASH_HIGHFLOOR,
                BaseConstants.NATIVE_SPLASH_HIGHFLOOR
            ),
            "language_highfloor_pass" to listOf(
                BaseConstants.NATIVE_LANGUAGE2_HIGHFLOOR
            ),
            "onboard_highfloor_pass" to listOf(
                BaseConstants.NATIVE_OB1_HIGHFLOOR
            )
        )
        YNMAds.getInstance().init(null, this, this.ynmAdsConfig)

        // Auto disable ad resume after user click ads and back to app
        Admob.getInstance().setDisableAdResumeWhenClickAds(true)
        // If true -> onNextAction() is called right after Ad Interstitial showed
        Admob.getInstance().setOpenActivityAfterShowInterAds(true)
        AppOpenManager.getInstance().disableAppResumeWithActivity(SplashActivity::class.java)
        AppOpenManager.getInstance().disableAppResumeWithActivity(LanguageActivity::class.java)
        AppOpenManager.getInstance().disableAppResumeWithActivity(Language2Activity::class.java)
        AppOpenManager.getInstance().disableAppResumeWithActivity(OnboardingActivity::class.java)
    }

    companion object {
        private lateinit var firebaseAnalytics: FirebaseAnalytics
        private lateinit var contextApp: AdsApplication

        fun getFireBaseAnalytic(): FirebaseAnalytics {
            return firebaseAnalytics
        }

        fun getContext(): AdsApplication {
            return contextApp
        }

        private fun initializeContext(context: AdsApplication) {
            contextApp = context
        }

        private fun initFirebaseAnalytics(context: Context): FirebaseAnalytics {
            if (!::firebaseAnalytics.isInitialized) {
                firebaseAnalytics = FirebaseAnalytics.getInstance(context)
            }
            return firebaseAnalytics
        }

        private fun fcmTestGetKey() {
            FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                    return@OnCompleteListener
                }

                // Get new FCM registration token
                val token = task.result
                Log.d(TAG, "FCM Token: $token")
                Airbridge.registerPushToken(token)
                if (!SharedPref.readBoolean("pushedFCM", false)) {
                    SharedPref.saveBoolean("pushedFCM", true)
//                    FcmApiClient.saveFcmToken(contextApp, token)
                }
                // Log and toast
                Log.d("Fcm :", token);
            })
        }
    }
}