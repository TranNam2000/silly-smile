package com.jrm.utils

import android.app.Activity
import android.content.Context
import android.util.DisplayMetrics
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.ads.nomyek_admob.ads_components.YNMAds
import com.ads.nomyek_admob.ads_components.YNMAdsCallbacks
import com.ads.nomyek_admob.ads_components.ads_banner.YNMBannerAdView
import com.ads.nomyek_admob.ads_components.ads_native.YNMNativeAdView
import com.ads.nomyek_admob.ads_components.wrappers.AdsError
import com.ads.nomyek_admob.ads_components.wrappers.AdsRewardItem
import com.ads.nomyek_admob.event.YNMAirBridge
import com.ads.nomyek_admob.event.YNMLogEventManager
import com.ads.nomyek_admob.utils.AdsCallback
import com.ads.nomyek_admob.utils.AdsInterMultiPreload
import com.ads.nomyek_admob.utils.AdsNativeMultiPreload
import com.ads.nomyek_admob.utils.AdsRewardMultiPreload
import com.ads.nomyek_admob.utils.TypeAds
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.nativead.NativeAd
import com.jrm.BuildConfig
import com.jrm.R
import com.jrm.ads.CollapsibleNativeAdManager
import com.jrm.utils.purchase.IAPHelper
import com.jrm.utils.remote_config.RemoteConfigManager

object AdsHelper {

    @JvmStatic
    fun isDisableObdAd(): Boolean {
        return IAPHelper.isPremium() || 
               RemoteConfigManager.instance?.disableAllAds == true ||
               RemoteConfigManager.instance?.disableObdAds == true
    }
    
    @JvmStatic
    fun isDisableAllAd(): Boolean {
        return IAPHelper.isPremium() || 
               RemoteConfigManager.instance?.disableAllAds == true
    }

    @JvmStatic
    fun preloadInterClick(context: Context, activityName: String) {
        if (AdsHelper.isDisableAllAd()) {
            return
        }

        var isBlock = true
        val delta = YNMAds.getInstance().adConfig.currentInterIndex - YNMAds.getInstance().adConfig.interStartIndex

        if (delta >= 0) {
            if (delta == 0 || YNMAds.blockCount >= YNMAds.getInstance().adConfig.interDeltaIndex) {
                isBlock = false
            }
        }

        if (!YNMAds.isGoHome) isBlock = false

        if (!isBlock) {
            val listAdId: List<AdsInterMultiPreload.AdIdModel> = RemoteConfigManager.instance!!.getListAdIdInterFromRemote(BaseConstants.CLICK_INTER,
                RemoteConfigManager.instance!!.interClickIds)
            AdsInterMultiPreload.preloadMultipleInterAds(
                context,
                YNMAirBridge.AppData(activityName, "inter_splash"),
                listAdId,
                BaseConstants.CLICK_INTER,
                object : YNMAdsCallbacks(YNMAirBridge.AppData(activityName, BaseConstants.INTER_SPLASH), YNMAds.INTERSTITIAL) {
                    override fun onAdLoaded() {
                        super.onAdLoaded()
                    }

                    override fun onAdFailedToLoad(adError: AdsError?) {
                        super.onAdFailedToLoad(adError)
                    }
                }
            )
        }
    }
    @JvmStatic
    fun isInterBlocking() : Boolean {
        var isBlock = true
        val delta = YNMAds.getInstance().adConfig.currentInterIndex - YNMAds.getInstance().adConfig.interStartIndex

        if (delta >= 0) {
            if (delta == 0 || YNMAds.blockCount >= YNMAds.getInstance().adConfig.interDeltaIndex) {
                isBlock = false
            }
        }
        return isBlock
    }

    @JvmStatic
    fun showInterPreload(context: Activity, activityName: String, callback: Runnable) {
        if (AdsHelper.isDisableAllAd()) {
            callback.run()
            return
        }
        if (!isInterBlocking() && AdsInterMultiPreload.isAdLoaded(BaseConstants.INTER_SPLASH)) {
            AdsInterMultiPreload.showPreloadedInterAdWithLoading(
                context,
                BaseConstants.INTER_SPLASH,
                10000,
                object : YNMAdsCallbacks(YNMAirBridge.AppData(activityName, BaseConstants.INTER_SPLASH), YNMAds.INTERSTITIAL) {
                    override fun onNextAction(isShown: Boolean) {
                        super.onNextAction(isShown)
                        if (isShown) {
                            showPreloadedNativeFullScreen(context, "", "fsn_inter", callback)
                        } else {
                            callback.run()
                        }
                        if (!AdsHelper.isInterBlocking() && YNMAds.isGoHome) {
                            AdsHelper.preloadInterClick(context, activityName)
                        }
                    }

                    override fun onAdClosed() {
                        super.onAdClosed()
                        // Clean up after showing
                        com.ads.nomyek_admob.utils.AdsInterMultiPreload.destroyPreloadedAd(BaseConstants.INTER_SPLASH)
                    }
                }
            )
            return
        }
        if (!AdsHelper.isInterBlocking() && YNMAds.isGoHome) {
            var state = AdsInterMultiPreload.getPreloadState(BaseConstants.CLICK_INTER)
            if (state != AdsInterMultiPreload.PreloadState.LOADED && state != AdsInterMultiPreload.PreloadState.LOADING) {
                AdsHelper.preloadInterClick(context, activityName)
            }
        }
        AdsInterMultiPreload.showPreloadedInterAdWithLoading(
            context,
            BaseConstants.CLICK_INTER,
            10000,
            object : YNMAdsCallbacks(YNMAirBridge.AppData(activityName, BaseConstants.CLICK_INTER), YNMAds.INTERSTITIAL) {
                override fun onNextAction(isShown: Boolean) {
                    super.onNextAction(isShown)
                    if (isShown) {
                        showPreloadedNativeFullScreen(context, "", "fsn_inter", callback)
                    } else {
                        callback.run()
                    }
                    if (!AdsHelper.isInterBlocking() && YNMAds.isGoHome) {
                        AdsHelper.preloadInterClick(context, activityName)
                    }
                }

                override fun onAdClosed() {
                    super.onAdClosed()
                    // Clean up after showing
                    AdsInterMultiPreload.destroyPreloadedAd(BaseConstants.INTER_SPLASH)
                }
            }
        )
    }

    

    @JvmStatic
    fun showPreloadedNativeFullScreen(
        activity: Activity,
        activityName: String,
        adPlaceConstant: String,
        callback: Runnable
    ) {
        val TAG = "showPreloadedNativeFullScreen"
        if (!RemoteConfigManager.instance!!.fsnAfterInter) {
            callback.run()
            return
        }
        try {
            val listAdId = RemoteConfigManager.instance!!.getListAdIdNativeFromRemote(adPlaceConstant,
                RemoteConfigManager.instance!!.fsnClickIds)

            // Inflate the full screen layout
            val inflater = android.view.LayoutInflater.from(activity)
            val fullScreenView = inflater.inflate(
                R.layout.layout_native_fullscreen_waterfall,
                null
            )

            // Get the YNMNativeAdView
            val nativeAdView = fullScreenView.findViewById<YNMNativeAdView>(R.id.native_onboarding_full)

            // Get the next button
            val btnNext = fullScreenView.findViewById<ImageView>(
                R.id.btn_next
            )

            // Get the container
            val container = fullScreenView.findViewById<View>(
                activity.resources.getIdentifier("fullscreen_native_container", "id", activity.packageName)
            )

            // Add to activity's root view
            val rootView = activity.window.decorView.findViewById<android.view.ViewGroup>(android.R.id.content)
            rootView.addView(fullScreenView)

            // Show the container
            container.visibility = View.VISIBLE

            // Show the native ad using YNMNativeAdView's built-in method
            // YNMNativeAdView will automatically load and display the ad
            AdsNativeMultiPreload.preloadMultipleNativeAds(
                activity,
                YNMAirBridge.AppData("nativead", "nativead"),
                listAdId,
                adPlaceConstant,
                object : YNMAdsCallbacks() {
                    override fun onNativeAdLoaded(nativeAd: NativeAd) {
                        super.onNativeAdLoaded(nativeAd)
                        AdsNativeMultiPreload.showPreloadedNativeAd(
                            activity,
                            nativeAdView,
                            adPlaceConstant,
                            R.layout.custom_full_screen_native_ads,
                            R.layout.custom_full_screen_native_ads,
                        )
                    }

                    override fun onAdFailedToLoad(adError: AdsError?) {
                        super.onAdFailedToLoad(adError)
                        // Remove the full screen view
                        rootView.removeView(fullScreenView)
                        // Destroy the ad
                        AdsNativeMultiPreload.destroyPreloadedAd(adPlaceConstant)
                        // Invoke callback
                        callback.run()
                    }
                }
            )


            // Handle next button click
            btnNext.setOnClickListener {
                // Remove the full screen view
                rootView.removeView(fullScreenView)
                // Destroy the ad
                AdsNativeMultiPreload.destroyPreloadedAd(adPlaceConstant)
                // Invoke callback
                callback.run()
            }

            Logger.d("Native full screen ad shown for $adPlaceConstant")
        } catch (e: Exception) {
            Logger.e("Error showing native full screen ad", e)
            callback.run()
        }
    }
    /**
     * Check if inter ad is loaded and show it, otherwise execute callback immediately
     * @param activity Activity context
     * @param activityName Name of the activity for tracking
     * @param adPlace Ad placement constant (e.g., BaseConstants.INTER_SPLASH)
     * @param callback Callback to execute after ad is shown or if ad is not available
     */
    @JvmStatic
    fun checkAndShowInterMissing(activity: Activity, activityName: String, adPlace: String, callback: Runnable) {
        if (AdsInterMultiPreload.isAdLoaded(adPlace)) {
            AdsInterMultiPreload.showPreloadedInterAdWithLoading(
                activity,
                adPlace,
                10000,
                object : YNMAdsCallbacks(YNMAirBridge.AppData(activityName, adPlace), YNMAds.INTERSTITIAL) {
                    override fun onNextAction(isShown: Boolean) {
                        super.onNextAction(isShown)
                        callback.run()
                    }

                    override fun onInterstitialShow() {
                        super.onInterstitialShow()
                    }

                    override fun onAdClosed() {
                        super.onAdClosed()
                        // Clean up after showing
                        com.ads.nomyek_admob.utils.AdsInterMultiPreload.destroyPreloadedAd(adPlace)
                    }

                    override fun onAdFailedToLoad(adError: com.ads.nomyek_admob.ads_components.wrappers.AdsError?) {
                        super.onAdFailedToLoad(adError)
                    }
                }
            )
        } else {
            callback.run()
        }
    }


    @JvmStatic
    fun setUpSplashApp() {
        // Empty implementation
    }
    
    /**
     * Parse config value and check if current session/day matches
     * @param configValue String value from remote config (e.g., "ss1,2,3" or "d1,2,3")
     * @return true if current session/day is in the list, false otherwise
     */
    private fun shouldDisableBasedOnConfig(configValue: String): Boolean {
        if (configValue.isBlank()) {
            return false
        }
        
        val trimmedValue = configValue.trim()
        
        // Check for session format (ss)
        if (trimmedValue.startsWith("ss", ignoreCase = true)) {
            val sessionsStr = trimmedValue.substring(2) // Remove "ss" prefix
            val sessions = parseNumberList(sessionsStr)
            if (sessions.isNotEmpty()) {
                val currentSession = BaseUtils.getSessionNumber()
                return sessions.contains(currentSession)
            }
        }
        // Check for day format (d)
        else if (trimmedValue.startsWith("d", ignoreCase = true)) {
            val daysStr = trimmedValue.substring(1) // Remove "d" prefix
            val days = parseNumberList(daysStr)
            if (days.isNotEmpty()) {
                val installDateStr = SharedPref.readString("install_date", "")
                if (installDateStr.isNotEmpty()) {
                    val currentDay = getDaysSinceInstall(installDateStr)
                    return days.contains(currentDay)
                }
            }
        }
        
        return false
    }
    
    /**
     * Parse comma-separated number list
     * @param numbersStr String like "1,2,3,4,5"
     * @return List of integers
     */
    private fun parseNumberList(numbersStr: String): List<Int> {
        return try {
            numbersStr.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .mapNotNull { it.toIntOrNull() }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Calculate days since install date
     * @param installDateStr Install date in "yyyy-MM-dd" format
     * @return Number of days since install
     */
    private fun getDaysSinceInstall(installDateStr: String): Int {
        return try {
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val installDate = dateFormat.parse(installDateStr)
            val currentDate = java.util.Date()
            
            if (installDate != null) {
                val diffInMillis = currentDate.time - installDate.time
                val diffInDays = diffInMillis / (1000 * 60 * 60 * 24)
                diffInDays.toInt()
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    }

    @JvmStatic
    fun checkAndShowNativeMissing(activity: Context, layoutId: Int, adPlace: List<String>, adView: YNMNativeAdView,  callback: Runnable, fallback: Runnable) {
        var showMissing = false
        for (place in adPlace) {
            if (AdsNativeMultiPreload.isAdLoaded(place)) {
                showMissing = true
                AdsNativeMultiPreload.showPreloadedNativeAd(
                    activity,
                    adView,
                    place,
                    layoutId,
                    layoutId
                )
                callback.run()
                return
            }
        }
        if (!showMissing) {
            fallback.run()
        }
    }

    fun showActivityMissingSplashAd(
        context: Activity,
        activityNew: Class<*>?,
        bundle: Bundle?
    ) {
        AdsHelper.checkAndShowInterMissing(
            context,
            "translate",
            BaseConstants.INTER_SPLASH
        , {
                val intent = Intent(context, activityNew)
                intent.putExtras(if (bundle != null) bundle else Bundle())
                context.startActivity(intent)
        })
    }

    @JvmStatic
    fun loadAndShowBanner(activity: Activity, layoutId: Int,  adId: String, adView: YNMBannerAdView,  callback: Runnable, fallback: Runnable) {
        YNMAds.getInstance().setInitCallback {
            adView?.loadBanner(activity, adId,object : YNMAdsCallbacks(YNMAirBridge.AppData(activity.localClassName, "banner_home"), YNMAds.BANNER) {
                override fun onAdLoaded() {
                    super.onAdLoaded()
                    callback.run()
                }

                override fun onAdFailedToLoad(adError: AdsError?) {
                    super.onAdFailedToLoad(adError)
                    fallback.run()
                }
            })
        }
    }

    /**
     * Load and show Inline Adaptive Banner Ad directly using AdMob SDK
     * This creates an inline adaptive banner that fits the container width with fixed height
     * 
     * @param activity Activity context
     * @param adId AdMob banner ad unit ID
     * @param container ViewGroup container (e.g., FrameLayout, LinearLayout)
     * @param fixedHeightDp Fixed height in dp (e.g., 150)
     * @param callback Success callback
     * @param fallback Failure callback
     */
    @JvmStatic
    fun loadAndShowInlineAdaptiveBanner(
        activity: Activity,
        adId: String,
        container: ViewGroup,
        fixedHeightDp: Int = 150,
        callback: Runnable,
        fallback: Runnable
    ) {
        YNMAds.getInstance().setInitCallback {
            try {
                Logger.d("Starting to load inline adaptive banner: $adId")
                
                // Get the ad size for inline adaptive banner
                val adSize = getInlineAdaptiveBannerAdSize(activity, fixedHeightDp)
//                val adSize = AdSize.getInlineAdaptiveBannerAdSize()
                // Create AdView programmatically
                val adView = AdView(activity).apply {
                    adUnitId = adId
                    setAdSize(adSize)
                }
                
                // Set ad listener
                adView.adListener = object : com.google.android.gms.ads.AdListener() {
                    override fun onAdLoaded() {
                        super.onAdLoaded()
                        Logger.d("Inline adaptive banner loaded successfully")
                        
                        // Clear container and add ad view
                        container.removeAllViews()
                        container.addView(adView)
                        container.visibility = View.VISIBLE
                        if (adView != null) {
                            adView.onPaidEventListener = OnPaidEventListener { adValue: AdValue? ->
                                Logger.d("OnPaidEvent banner:" + adValue!!.getValueMicros())
                                YNMLogEventManager.logPaidAdImpression(
                                    activity,
                                    adValue,
                                    adView.getAdUnitId(),
                                    adView.getResponseInfo(), TypeAds.BANNER
                                )
                            }
                        }
                        callback.run()
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        super.onAdFailedToLoad(adError)
                        Logger.e("Inline adaptive banner failed to load: ${adError.message} (code: ${adError.code})")
                        container.visibility = View.GONE
                        fallback.run()
                    }

                    override fun onAdClicked() {
                        super.onAdClicked()
                        Logger.d("Inline adaptive banner clicked")
                    }

                    override fun onAdImpression() {
                        super.onAdImpression()
                        Logger.d("Inline adaptive banner impression")
                    }

                    override fun onAdOpened() {
                        super.onAdOpened()
                        Logger.d("Inline adaptive banner opened")
                    }

                    override fun onAdClosed() {
                        super.onAdClosed()
                        Logger.d("Inline adaptive banner closed")
                    }
                }
                
                // Load the ad
                val adRequest = AdRequest.Builder().build()
                adView.loadAd(adRequest)
                
            } catch (e: Exception) {
                Logger.e("Error loading inline adaptive banner", e)
                container.visibility = View.GONE
                fallback.run()
            }
        }
    }

    /**
     * Get inline adaptive banner ad size
     * 
     * @param activity Activity context
     * @param fixedHeightDp Fixed height in dp
     * @return AdSize for inline adaptive banner
     */
    private fun getInlineAdaptiveBannerAdSize(activity: Activity, fixedHeightDp: Int): AdSize {
        // Get the screen width in dp
        val display = activity.windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)

        val density = outMetrics.density
        var adWidthPixels = outMetrics.widthPixels.toFloat()

        // If the ad is in a scrollable container, you may need to adjust the width
        // For full-width banner, use screen width
        val adWidth = (adWidthPixels / density).toInt()
        return AdSize.getInlineAdaptiveBannerAdSize(adWidth, fixedHeightDp)

        // Create inline adaptive banner with fixed height
//        return AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(activity, adWidth)
    }

    @JvmStatic
    fun getLayoutForMetaNativeAd(nativeAd: NativeAd?, admobResId: Int, metaResId: Int): Int {
        val adapterClassName = nativeAd?.responseInfo?.mediationAdapterClassName
        val isMeta = adapterClassName != null && (adapterClassName.contains(
            "Facebook",
            ignoreCase = true
        ) || adapterClassName.contains("Meta", ignoreCase = true))
        Logger.d("getLayoutForMetaNativeAd(NativeAd, admobResId, metaResId): adapterClassName=$adapterClassName, isMeta=$isMeta")
        return if (isMeta) {
            metaResId
        } else {
            admobResId
        }
    }

    //for splash and language
    @JvmStatic
    fun getLayoutForMetaNativeAd(nativeAd: NativeAd?): Int {
        Logger.d("getLayoutForMetaNativeAd(NativeAd)")
        return getLayoutForMetaNativeAd(
            nativeAd,
            R.layout.custom_native_admob_large,
            R.layout.custom_native_meta_large
        )
    }

    //for a specific target
    @JvmStatic
    fun getLayoutForMetaNativeAd(placeName: String): Int {
        Logger.d("getLayoutForMetaNativeAd(placeName=$placeName)")
        return getLayoutForMetaNativeAd(
            AdsNativeMultiPreload.getPreloadedAd(placeName),
            R.layout.custom_native_admob_large,
            R.layout.custom_native_meta_large
        )
    }

    //for a specific target
    @JvmStatic
    fun getLayoutForMetaNativeAd(placeName: String, target: String): Int {
        Logger.d("getLayoutForMetaNativeAd(placeName=$placeName, target=$target)")
        if (placeName == target)
            return getLayoutForMetaNativeAd(placeName)
        else return R.layout.custom_native_admob_large
    }

    @JvmStatic
    fun showReward(adPlace: String, activity: Activity, adIds:  List<AdsRewardMultiPreload.AdIdModel>, callback: Runnable) {
        var earn = false
        // Get and show (tương tự YNMRewardAds.getAndShowRewardAd)
        AdsRewardMultiPreload.getAndShowRewardAdWithMultiId(
            activity,
            adIds,
            adPlace,
            object : AdsCallback() {
                override fun onUserEarnedReward(rewardItem: AdsRewardItem) {
                    super.onUserEarnedReward(rewardItem)
                    earn = true
                }

                override fun onNextAction(isShown: Boolean) {
                    super.onNextAction(isShown)
                    if (earn) {
                        callback.run()
                    }
                }
            }
        )
    }

    @JvmStatic
    fun preloadCollapsibleNativeAdHome(context: Context, activityName:String) {
        val adPlace = "draw_cl"

        // Check preload status
        val isAlreadyPreloaded = CollapsibleNativeAdManager.isPreloaded(adPlace)
        val isCurrentlyLoading = CollapsibleNativeAdManager.isLoading(adPlace)

        when {
            isAlreadyPreloaded -> {
                Logger.d("[$adPlace] Ad already preloaded, skipping")
            }
            isCurrentlyLoading -> {
                Logger.d("[$adPlace] Ad is currently loading, skipping")
            }
            else -> {
                Logger.d("[$adPlace] Starting preload")
                CollapsibleNativeAdManager.preloadAds(
                    context,
                    RemoteConfigManager.instance?.nativeClDrawIds ?: "",
                    adPlace,
                    activityName,
                    com.jrm.R.layout.custom_native_admob_collap
                )
            }
        }
    }

}

