package com.jrm.utils.remote_config

import android.app.Activity
import android.content.Context
import android.os.Build
import com.ads.nomyek_admob.utils.AdsInterMultiPreload
import com.ads.nomyek_admob.utils.AdsNativeMultiPreload
import com.ads.nomyek_admob.utils.AdsRewardMultiPreload
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.gson.Gson
import com.jrm.BuildConfig
import com.jrm.R
import com.jrm.model.AdConfigModel
import com.jrm.model.IdRegistryModel
import com.jrm.utils.BaseUtils
import com.jrm.utils.InternetUtil
import com.jrm.utils.Logger
import kotlinx.coroutines.delay

class RemoteConfigManager {
    private var remoteConfig: FirebaseRemoteConfig? = null
    private var isLoading = false
    var numberScreenObd: Int = 5;
    var timeReloadNative: Long = 30;
    var timeOutSplash: Long = 10000
    var languageOrder: String = ""
    var disableObdAds: Boolean = false
    var disableAllAds: Boolean = false

    // New remote config flags for ads
    var bannerHighSpl: Boolean = true
    var bannerSpl: Boolean = true
    var interstitialRule: String = "1/2"
    var timeInterstitialCooldown: Long = 30
    var timeOutFullScreenAd: Long = 3000
    var timeOutReward: Long = 5
    var formatTypeSplash = "native"
    var preloadInterFinishObdIndex: Long = 3
    var timeReloadNativeBanner: Long = 30
    var upgradePopup: String = "hide"
    var minTimeSplash: Long = 8000
    var disableInterSplash: String = ""
    var disableAdsSplash: String = ""

    // Enable ID Session2 for each ad type
    var enableIdSession2: Boolean = false
    var enableBtnContinueSplash: Boolean = true

    // Ad config for AR Draw waterfall
    var interObd: Boolean = false
    var fullScreenSplashWaterfall: String = ""
    var nativeL1Ids: String = ""
    var nativeL2Ids: String = ""
    var nativeObd1Ids: String = ""
    var nativeObd2Ids: String = ""
    var nativeObd3Ids: String = ""
    var nativeObd4Ids: String = ""
    var intersObd6Ids: String = ""
    var nativeSplIds: String = ""
    var rewardHighIds: String = ""
    var nativeDrawIds: String = ""
    var nativeClDrawIds: String = ""
    var boostFNativeObd: Boolean = false
    var numberFinishObd: Long = 2
    var fsnAfterInter: Boolean = false
    var interClickIds: String = ""
    var fsnClickIds: String = ""
    var homeNativeIds: String = ""
    var homeNabanerIds: String = ""
    var adConfig: AdConfigModel? = null
    var idRegistry: IdRegistryModel? = null

    fun init(context: Context) {
        loadRemote(context) {
            val rawMinTime = adConfig?.screenObd?.splash?.minTimeSplash ?: 8
            val rawTimeOut = adConfig?.screenObd?.splash?.timeOutSplash ?: 12

            timeOutSplash = rawTimeOut * 1000L
            minTimeSplash = rawMinTime * 1000L
            numberScreenObd = adConfig?.screenObd?.onboarding?.numberScreen ?: 5
            numberFinishObd = adConfig?.configs?.numberFinishObd?.toLong() ?: 2
            disableAllAds = adConfig?.configs?.disableAllAd ?: false
            disableObdAds = adConfig?.configs?.disableObdAd ?: false
            interstitialRule = adConfig?.configs?.interstitialRule ?: "1/2"
            timeInterstitialCooldown = adConfig?.configs?.timeInterstitialCooldown ?: 30
            timeOutFullScreenAd = adConfig?.configs?.timeOutFullScreenAd ?: 5
            timeOutReward = (adConfig?.configs?.timeOutReward ?: 5) * 1000L
            enableBtnContinueSplash = adConfig?.screenObd?.splash?.enableBtnContinue ?: false
        }
    }

    fun loadRemote(context: Context, onFinishDataJson: (() -> Unit)? = null) {
        if (isLoading) {
            return
        }
        isLoading = true

        val config = FirebaseRemoteConfig.getInstance()
        val configSettings =
            FirebaseRemoteConfigSettings.Builder().setMinimumFetchIntervalInSeconds(0).build()
        config.setConfigSettingsAsync(configSettings)
        config.setDefaultsAsync(R.xml.default_config)
        config.fetch().addOnCompleteListener {
            FirebaseRemoteConfig.getInstance().activate().addOnCompleteListener {
                isLoading = false
                remoteConfig = FirebaseRemoteConfig.getInstance()
                try {
                    var adConfigJson = config.getString("ad_config")
                    if (adConfigJson.isEmpty()) {
                        try {
                            val inputStream =
                                context.resources.openRawResource(R.raw.ad_config_quran_android_1)
                            adConfigJson = inputStream.bufferedReader().use { it.readText() }
                        } catch (e: Exception) {
                            Logger.e("Error reading local ad config", e)
                        }
                    }

                    if (adConfigJson.isNotEmpty()) {
                        adConfig = Gson().fromJson(adConfigJson, AdConfigModel::class.java)

                        if (adConfig?.sessionConfigs.isNullOrEmpty()) {
                            try {
                                adConfigJson =
                                    context.resources.openRawResource(R.raw.ad_config_quran_android_1)
                                        .bufferedReader().use { it.readText() }
                                adConfig = Gson().fromJson(adConfigJson, AdConfigModel::class.java)
                            } catch (e2: Exception) {
                                Logger.e("Error loading ad config from raw fallback", e2)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Logger.e("Error parsing ad config", e)
                    try {
                        val rawJson =
                            context.resources.openRawResource(R.raw.ad_config_quran_android_1)
                                .bufferedReader().use { it.readText() }
                        adConfig = Gson().fromJson(rawJson, AdConfigModel::class.java)
                    } catch (e2: Exception) {
                        Logger.e("Error loading ad config from raw", e2)
                    }
                }

                try {
                    var idRegistryJson = config.getString("id_registry")
                    if (idRegistryJson.isEmpty()) {
                        try {
                            val inputStream =
                                context.resources.openRawResource(R.raw.id_registry_quran_android_1)
                            idRegistryJson = inputStream.bufferedReader().use { it.readText() }
                        } catch (e: Exception) {
                            Logger.e("Error reading local id registry", e)
                        }
                    }

                    if (idRegistryJson.isNotEmpty()) {
                        idRegistry = Gson().fromJson(idRegistryJson, IdRegistryModel::class.java)
                    }
                } catch (e: Exception) {
                    Logger.e("Error parsing id registry", e)
                }

                if (BuildConfig.FLAVOR == "appDev") {
                    fullScreenSplashWaterfall =
                        "OPEN:ca-app-pub-3940256099942544/9257395921,INTER:ca-app-pub-3940256099942544/1033173712,FULL_NATIVE:ca-app-pub-3940256099942544/2247696110"
                    nativeSplIds =
                        "MAX_NATIVE:,NATIVE:ca-app-pub-3940256099942544/2247696110,NATIVE:ca-app-pub-3940256099942544/2247696110"
                    nativeL1Ids =
                        "MAX_NATIVE:,NATIVE:ca-app-pub-3940256099942544/2247696110,NATIVE:ca-app-pub-3940256099942544/2247696110"
                    nativeL2Ids =
                        "MAX_NATIVE:,NATIVE:ca-app-pub-3940256099942544/2247696110,NATIVE:ca-app-pub-3940256099942544/2247696110"
                    nativeObd1Ids =
                        "MAX_NATIVE:,NATIVE:ca-app-pub-3940256099942544/2247696110,NATIVE:ca-app-pub-3940256099942544/2247696110"
                    nativeObd2Ids =
                        "MAX_NATIVE:,NATIVE:ca-app-pub-3940256099942544/2247696110,NATIVE:ca-app-pub-3940256099942544/2247696110"
                    nativeObd3Ids =
                        "MAX_NATIVE:,NATIVE:ca-app-pub-3940256099942544/2247696110,NATIVE:ca-app-pub-3940256099942544/2247696110"
                    nativeObd4Ids =
                        "MAX_NATIVE:,NATIVE:ca-app-pub-3940256099942544/2247696110,NATIVE:ca-app-pub-3940256099942544/2247696110"
                    intersObd6Ids = "ca-app-pub-3940256099942544/1033173712"
                    rewardHighIds =
                        "MAX_NATIVE:,NATIVE:ca-app-pub-3940256099942544/2247696110,NATIVE:ca-app-pub-3940256099942544/2247696110"
                    nativeDrawIds =
                        "MAX_NATIVE:,NATIVE:ca-app-pub-3940256099942544/2247696110,NATIVE:ca-app-pub-3940256099942544/2247696110"
                    nativeClDrawIds =
                        "MAX_NATIVE:,NATIVE:ca-app-pub-3940256099942544/2247696110,NATIVE:ca-app-pub-3940256099942544/2247696110"
                    homeNabanerIds =
                        "ca-app-pub-3940256099942544/2247696110,ca-app-pub-3940256099942544/2247696110"
                    homeNativeIds =
                        "ca-app-pub-3940256099942544/2247696110,ca-app-pub-3940256099942544/2247696110"
                    disableObdAds = false
                    disableAllAds = false
                    enableBtnContinueSplash = true
                    interClickIds =
                        "ca-app-pub-3940256099942544/1033173712,ca-app-pub-3940256099942544/1033173712"
                    fsnClickIds =
                        "ca-app-pub-3940256099942544/2247696110,ca-app-pub-3940256099942544/2247696110"
                }
                adConfig?.currentSession = BaseUtils.getSessionNumber()
                onFinishDataJson?.invoke()
            }
        }
    }


    fun getListAdIdRewardFromRemote(
        adPlace: String,
        ids: String
    ): List<AdsRewardMultiPreload.AdIdModel> {
        val listAdIds = ids.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        return listAdIds.mapIndexed { index, adId ->
            AdsRewardMultiPreload.AdIdModel().apply {
                this.adId = adId
                this.adName = "${adPlace}_${index}"
            }
        }
    }

    fun FirebaseRemoteConfig.getS2AdsKeys(base: String): String {
        val result =
            if (enableIdSession2 && BaseUtils.getSessionNumber() > 1) getString(base + "_v2")
            else getString(base)
        return result
    }

    fun getListAdIdNativeFromRemote(
        adPlace: String,
        ids: String
    ): List<AdsNativeMultiPreload.AdIdModel> {
        val listAdIds = ids.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        return listAdIds.mapIndexed { index, adId ->
            AdsNativeMultiPreload.AdIdModel().apply {
                this.adId = adId
                this.adName = "${adPlace}_${index}"
            }
        }
    }

    fun getListAdIdInterFromRemote(
        adPlace: String,
        ids: String
    ): List<AdsInterMultiPreload.AdIdModel> {
        val listAdIds = ids.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        return listAdIds.mapIndexed { index, adId ->
            AdsInterMultiPreload.AdIdModel().apply {
                this.adId = adId
                this.adName = "${adPlace}_${index}"
            }
        }
    }


    fun getStartIndexInter(): Int {
        return try {
            val parts = interstitialRule?.split("/") ?: return 0
            if (parts.size >= 1) {
                parts[0].toIntOrNull() ?: 0
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    }

    fun getDeltaIndexInter(): Int {
        return try {
            val parts = interstitialRule?.split("/") ?: return 1
            if (parts.size >= 2) {
                parts[1].toIntOrNull() ?: 1
            } else {
                1
            }
        } catch (e: Exception) {
            1
        }
    }


    fun loadIsShowConsent(activity: Activity, callback: BooleanCallback) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            if (!InternetUtil.isNetworkAvailable(activity)) {
//                callback.onResult(false)
//                return
//            }
//        }
        if (isLoading && remoteConfig == null) {
            Thread {
                while (isLoading || remoteConfig == null) {
                    try {
                        Thread.sleep(100)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                }
                activity.runOnUiThread {
                    if (remoteConfig != null) {
                        callback.onResult(remoteConfig!!.getBoolean(IS_SHOW_CONSENT))
                    }
                }
            }.start()
        } else {
            if (remoteConfig != null) {
                callback.onResult(remoteConfig!!.getBoolean(IS_SHOW_CONSENT))
            }
        }
    }


    fun loadReshowGDPRSplashCount(activity: Activity, callback: NumberCallback) {
        if (isLoading && remoteConfig == null) {
            Thread {
                while (isLoading || remoteConfig == null) {
                    try {
                        Thread.sleep(100)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                }
                activity.runOnUiThread { callback.onResult(remoteConfig!!.getLong(RESHOW_GDPR_SPLASH)) }
            }.start()
        } else {
            callback.onResult(remoteConfig!!.getLong(RESHOW_GDPR_SPLASH))
        }
    }

    val isShowConsent: Boolean
        get() = remoteConfig != null && remoteConfig!!.getBoolean(IS_SHOW_CONSENT)

    fun limitFunctionClickCount(): Long {
        return if (remoteConfig == null) {
            0
        } else {
            remoteConfig!!.getLong(LIMIT_FUNCTION_IN_APP)
        }
    }

    fun getLanguageOrderList(): List<String> {
        return if (languageOrder.isNotEmpty()) {
            try {
                languageOrder.split(",").map { it.trim() }
            } catch (e: Exception) {
                Logger.e("Error parsing language order: ${e.message}")
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    suspend fun loadConfigCallback(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!InternetUtil.isNetworkAvailable(context)) {
                return false
            }
        }
        while (isLoading) {
            delay(100)
        }

        return remoteConfig != null
    }

    interface BooleanCallback {
        fun onResult(value: Boolean)
    }

    interface NumberCallback {
        fun onResult(value: Long)
    }

    interface StringCallback {
        fun onResult(value: String?)
    }

    companion object {
        private const val IS_SHOW_CONSENT = "is_show_consent"
        private const val LIMIT_FUNCTION_IN_APP = "limit_function_in_app"
        private const val RESHOW_GDPR_SPLASH = "reshow_gdpr_splash"
        private const val NUMBER_OBD_SCREEN = "obd_number_screen"
        private var INSTANCE: RemoteConfigManager? = null

        @JvmStatic
        val instance: RemoteConfigManager?
            get() {
                if (INSTANCE == null) {
                    INSTANCE = RemoteConfigManager()
                }
                return INSTANCE
            }
    }
}