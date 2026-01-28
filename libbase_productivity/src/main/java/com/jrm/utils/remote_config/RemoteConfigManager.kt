package com.jrm.utils.remote_config

import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.Log
import android.util.Patterns
import com.ads.nomyek_admob.utils.AdsInterMultiPreload
import com.ads.nomyek_admob.utils.AdsNativeMultiPreload
import com.ads.nomyek_admob.utils.AdsRewardMultiPreload
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.jrm.BuildConfig
import com.jrm.R
import com.jrm.utils.BaseConstants
import com.jrm.utils.InternetUtil
import com.jrm.utils.BaseUtils
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class RemoteConfigManager {
    private var remoteConfig: FirebaseRemoteConfig? = null
    private var isLoading = false
    var numberScreenObd: Long = 5;
    var timeReloadBanner: Long = 30;
    var timeReloadNative: Long = 30;
    var timeOutSplash: Long = 10000
    var retryHigh: Boolean = true
    var languageOrder: String = ""
    var disableObdAds: Boolean = false
    var disableAllAds: Boolean = false
    var adIdsOrder202: String = "Max,High,All"
    var adIdsOrder302: String = "Max,High,All"

    // New remote config flags for ads
    var interSpl: Boolean = true
    var interHighSpl: Boolean = true
    var nativeSpl: Boolean = true
    var nativeHighSpl: Boolean = true
    var bannerHighSpl: Boolean = true
    var bannerSpl: Boolean = true
    var lfo1: Boolean = true
    var lfo1High: Boolean = true
    var lfo2: Boolean = true
    var lfo2High: Boolean = true
    var lfo2Max: Boolean = true
    var ob1: Boolean = true
    var ob1High: Boolean = true
    var ob2: Boolean = true
    var ob2High: Boolean = true
    var ob2Max: Boolean = true
    var ob3: Boolean = true
    var ob3High: Boolean = true
    var ob3Max: Boolean = true
    var ob4: Boolean = true
    var ob4High: Boolean = true
    var ob5: Boolean = true
    var ob5High: Boolean = true
    var ob6: Boolean = true
    var ob6High: Boolean = true
    var interstitialRule: String = "1/2"
    var formatTypeSplash = "native"
    var preloadInterFinishObdIndex: Long = 3
    var formatBannerHome = "collab"
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
    var rewardHighIds :String =""
    var nativeDrawIds:String = ""
    var nativeClDrawIds: String = ""
    var isSubmiting: Boolean = false
    var boostFNativeObd: Boolean = false
    var boostCollapHome: Boolean = false
    var boostCollapDraw: Boolean = false
    var enableClPreview1: Boolean = false
    var enableClPreview2: Boolean = false
    var numberFinishObd: Long = 2
    var fsnAfterInter: Boolean = false
    var interClickIds: String = ""
    var fsnClickIds: String = ""
    var homeNativeIds: String = ""
    var homeNabanerIds: String = ""
    var timeReloadCollapDraw: Long = 60
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
                numberScreenObd = 4
                // Load enable ID Session2 for each ad type
                enableIdSession2 = config.getBoolean("enable_id_session2_new")
                timeReloadBanner = config.getLong("time_reload_banner")
                timeReloadNative = config.getLong("time_reload_native")
                timeOutSplash = config.getLong("time_out_splash")
                retryHigh = config.getBoolean("retry_high")
                languageOrder = config.getString("language_order")
                disableAllAds = config.getBoolean("disable_all_ads")
                disableObdAds = config.getBoolean("disable_obd_ads")
                // Load new remote config flags
                interSpl = config.getBoolean("inter_spl")
                interHighSpl = config.getBoolean("inter_high_spl")
                nativeSpl = config.getBoolean("native_spl")
                nativeHighSpl = config.getBoolean("native_high_spl")
                bannerSpl = config.getBoolean("banner_spl")
                bannerHighSpl = config.getBoolean("banner_high_spl")
                lfo1 = config.getBoolean("lfo1")
                lfo1High = config.getBoolean("lfo1_high")
                lfo2 = config.getBoolean("lfo2")
                lfo2High = config.getBoolean("lfo2_high")
                lfo2Max = config.getBoolean("lfo2_max")
                ob1 = config.getBoolean("ob1")
                ob1High = config.getBoolean("ob1_high")
                ob2 = config.getBoolean("ob2")
                ob2High = config.getBoolean("ob2_high")
                ob2Max = config.getBoolean("ob2_max")
                ob3 = config.getBoolean("ob3")
                ob3High = config.getBoolean("ob3_high")
                ob3Max = config.getBoolean("ob3_max")
                ob4 = config.getBoolean("ob4")
                ob4High = config.getBoolean("ob4_high")
                ob5 = config.getBoolean("ob5")
                ob5High = config.getBoolean("ob5_high")
                ob6 = config.getBoolean("ob6")
                ob6High = config.getBoolean("ob6_high")
                adIdsOrder202 = config.getString("ad_ids_order_202")
                adIdsOrder302 = config.getString("ad_ids_order_302")
                interstitialRule = config.getString("interstitial_rule")
                formatTypeSplash = config.getString("format_splash")
                preloadInterFinishObdIndex = config.getLong("preload_inter_obd_index")
                formatBannerHome = config.getString("format_banner_home")
                timeReloadNativeBanner = config.getLong("time_reload_native_banner")
                upgradePopup = config.getString("upgrade_popup")
                minTimeSplash = config.getLong("min_time_splash")


                enableBtnContinueSplash = config.getBoolean("enable_btn_continue_splash")
                disableAdsSplash = config.getString("disable_ads_splash")
                disableInterSplash = config.getString("disable_inter_splash")
                interObd = config.getBoolean("inter_obd")
                isSubmiting = config.getBoolean("is_submit")

                // Load waterfall configs
                fullScreenSplashWaterfall = config.getS2AdsKeys("full_screen_splash_waterfall") //101
                nativeSplIds = config.getS2AdsKeys("native_spl_ids") // 102
                nativeL1Ids = config.getS2AdsKeys("native_l1_ids") // 201
                nativeL2Ids = config.getS2AdsKeys("native_l2_ids")  // 202
                nativeObd1Ids = config.getS2AdsKeys("native_obd1_ids") //301
                nativeObd2Ids = config.getS2AdsKeys("native_obd2_ids") //302
                nativeObd3Ids = config.getS2AdsKeys("native_obd3_ids") //303
                nativeObd4Ids = config.getS2AdsKeys("native_obd4_ids") //304
//                nativeObd5Ids = config.getS2AdsKeys("native_obd5_ids") //305
                intersObd6Ids = config.getS2AdsKeys("inter_obd6_ids") //306
                rewardHighIds = config.getS2AdsKeys("reward_high_ids") //406 TODO
                nativeClDrawIds = config.getS2AdsKeys("native_collapsible") //509
                numberFinishObd = config.getLong("number_finish_obd")
                fsnAfterInter = config.getBoolean("fsn_after_inter")
                interClickIds = config.getS2AdsKeys("inter_click_ids")
                fsnClickIds = config.getS2AdsKeys("fsn_click_ids")
                homeNativeIds = config.getS2AdsKeys("home_native_ids")
                homeNabanerIds = config.getS2AdsKeys("home_native_banner_ids")
                timeReloadCollapDraw = config.getLong("time_reload_collap_draw")
                Log.d("RemoteConfigManager", "Remote config loaded")
                if (BuildConfig.FLAVOR == "appDev") {
                    fullScreenSplashWaterfall = "INTER:ca-app-pub-3940256099942544/1033173712,FULL_NATIVE:ca-app-pub-3940256099942544/2247696110,OPEN:ca-app-pub-3940256099942544/9257395921"
                    nativeSplIds =  "MAX_NATIVE:,NATIVE:ca-app-pub-3940256099942544/2247696110,NATIVE:ca-app-pub-3940256099942544/2247696110"
                    nativeL1Ids = "MAX_NATIVE:,NATIVE:ca-app-pub-3940256099942544/2247696110,NATIVE:ca-app-pub-3940256099942544/2247696110"
                    nativeL2Ids = "MAX_NATIVE:,NATIVE:ca-app-pub-3940256099942544/2247696110,NATIVE:ca-app-pub-3940256099942544/2247696110"
                    nativeObd1Ids = "MAX_NATIVE:,NATIVE:ca-app-pub-3940256099942544/2247696110,NATIVE:ca-app-pub-3940256099942544/2247696110"
                    nativeObd2Ids = "MAX_NATIVE:,NATIVE:ca-app-pub-3940256099942544/2247696110,NATIVE:ca-app-pub-3940256099942544/2247696110"
                    nativeObd3Ids = "MAX_NATIVE:,NATIVE:ca-app-pub-3940256099942544/2247696110,NATIVE:ca-app-pub-3940256099942544/2247696110"
                    nativeObd4Ids = "MAX_NATIVE:,NATIVE:ca-app-pub-3940256099942544/2247696110,NATIVE:ca-app-pub-3940256099942544/2247696110"
//                    nativeObd5Ids = "MAX_NATIVE:,NATIVE:ca-app-pub-3940256099942544/2247696110,NATIVE:ca-app-pub-3940256099942544/2247696110"
                    intersObd6Ids = "ca-app-pub-3940256099942544/1033173712"
                    rewardHighIds = "MAX_NATIVE:,NATIVE:ca-app-pub-3940256099942544/2247696110,NATIVE:ca-app-pub-3940256099942544/2247696110"
                    nativeDrawIds = "MAX_NATIVE:,NATIVE:ca-app-pub-3940256099942544/2247696110,NATIVE:ca-app-pub-3940256099942544/2247696110"
                    nativeClDrawIds = "MAX_NATIVE:,NATIVE:ca-app-pub-3940256099942544/2247696110,NATIVE:ca-app-pub-3940256099942544/2247696110"
                    homeNabanerIds = "ca-app-pub-3940256099942544/2247696110,ca-app-pub-3940256099942544/2247696110"
                    homeNativeIds = "ca-app-pub-3940256099942544/2247696110,ca-app-pub-3940256099942544/2247696110"
                    disableObdAds = false
                    disableAllAds = false
                    enableBtnContinueSplash = true
                    interClickIds = "ca-app-pub-3940256099942544/1033173712,ca-app-pub-3940256099942544/1033173712"
                    fsnClickIds = "ca-app-pub-3940256099942544/2247696110,ca-app-pub-3940256099942544/2247696110"
                    timeReloadCollapDraw = 15
                }

                boostFNativeObd = config.getBoolean("boost_fnative_obd")
                boostCollapHome = config.getBoolean("boost_collap_home")
                boostCollapDraw = config.getBoolean("boost_collap_draw")
                enableClPreview1 = config.getBoolean("enable_cl_preview1")
                enableClPreview2 = config.getBoolean("enable_cl_preview2")
            }
        }
    }

    fun getListAdIdRewardFromRemote(adPlace: String, ids: String): List<AdsRewardMultiPreload.AdIdModel> {
        val listAdIds = ids.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        return listAdIds.mapIndexed { index, adId ->
            AdsRewardMultiPreload.AdIdModel().apply {
                this.adId = adId
                this.adName = "${adPlace}_${index}"
            }
        }
    }
    fun FirebaseRemoteConfig.getS2AdsKeys(base: String): String {
        val result = if (enableIdSession2 && BaseUtils.getSessionNumber() > 1) getString(base + "_v2")
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

    fun getListAdIdInterFromRemote(adPlace: String, ids: String): List<AdsInterMultiPreload.AdIdModel> {
        val listAdIds = ids.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        return listAdIds.mapIndexed { index, adId ->
            AdsInterMultiPreload.AdIdModel().apply {
                this.adId = adId
                this.adName = "${adPlace}_${index}"
            }
        }
    }

    fun getBannerSplAdId(): String {
        return if (bannerSpl) (if (BaseUtils.getSessionNumber() > 1 && enableIdSession2) BuildConfig._103_v2_spl_banner else BuildConfig._103_spl_banner) else ""
    }

    fun getBannerHighSplAdId(): String {
        return if (bannerHighSpl) (if (BaseUtils.getSessionNumber() > 1 && enableIdSession2) BuildConfig._103_v2_spl_banner_high else BuildConfig._103_spl_banner_high) else ""
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
                Log.e("RemoteConfigManager", "Error parsing language order: ${e.message}")
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    fun loadTimeOutSplash(activity: Activity, callback: NumberCallback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!InternetUtil.isNetworkAvailable(activity)) {
                callback.onResult(10000)
                return
            }
        }
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
                        callback.onResult(remoteConfig!!.getLong("time_out_splash"))
                    }
                }
            }.start()
        } else {
            if (remoteConfig != null) {
                callback.onResult(remoteConfig!!.getLong("time_out_splash"))
            }
        }
    }

    fun loadConfigCallback(activity: Activity, callback: BooleanCallback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!InternetUtil.isNetworkAvailable(activity)) {
                callback.onResult(false)
                return
            }
        }
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
                        callback.onResult(true)
                    }
                }
            }.start()
        } else {
            if (remoteConfig != null) {
                callback.onResult(true)
            }
        }
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