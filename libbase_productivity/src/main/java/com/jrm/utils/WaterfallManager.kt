package com.jrm.utils

import android.R
import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import com.ads.nomyek_admob.ads_components.YNMAds
import com.ads.nomyek_admob.ads_components.YNMAdsCallbacks
import com.ads.nomyek_admob.ads_components.ads_native.YNMNativeAdView
import com.ads.nomyek_admob.ads_components.wrappers.AdsError
import com.ads.nomyek_admob.event.YNMAirBridge
import com.ads.nomyek_admob.utils.AdsInterMultiPreload
import com.ads.nomyek_admob.utils.AdsNativeMultiPreload
import com.ads.nomyek_admob.utils.AdsAppOpenMultiPreload
import com.google.android.gms.ads.nativead.NativeAd

/**
 * Model classes for ad configuration
 */
data class AdConfig(
    val adPlace: String,
    val ads: List<AdItem>
)

data class AdItem(
    val format: String,
    val adId: String
)

/**
 * Waterfall ad preload result
 */
data class WaterfallPreloadResult(
    val success: Boolean,
    val format: String? = null,
    val adPlace: String? = null,
    val error: String? = null
)

/**
 * Manager for waterfall ad preloading and showing
 * Supports multiple ad formats in sequence (inter, app_open, full_native)
 */
object WaterfallManager {
    
    private const val TAG = "WaterfallManager"
    
    // Cache for loaded ad configs
    private val adConfigCache = mutableMapOf<String, AdConfig>()
    
    // Track preloaded ads by adPlace
    // Key: adPlace, Value: Pair(format, adPlace constant for preload system)
    private val preloadedAds = mutableMapOf<String, Pair<String, String>>()
    
    // Callbacks for preload completion
    private val preloadCallbacks = mutableMapOf<String, (WaterfallPreloadResult) -> Unit>()
    
    /**
     * Parse simple string format: "INTER:ad_id1,OPEN:ad_id2,FULL_NATIVE:ad_id3"
     * @param configString String in format "FORMAT:ad_id,FORMAT:ad_id,..."
     * @param adPlace Ad place identifier
     * @return AdConfig or null if parsing fails
     */
    private fun parseConfigString(configString: String, adPlace: String): AdConfig? {
        return try {
            val ads = mutableListOf<AdItem>()
            val items = configString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            
            for (item in items) {
                val parts = item.split(":", limit = 2)
                if (parts.size == 2) {
                    val format = parts[0].trim()
                    val adId = parts[1].trim()
                    
                    // Map format to standard format names
                    val normalizedFormat = when (format.uppercase()) {
                        "INTER", "INTERSTITIAL" -> "inter"
                        "OPEN", "APP_OPEN", "APPOPEN" -> "app_open"
                        "FULL_NATIVE", "NATIVE", "NATIVE_FULL" -> "full_native"
                        else -> format.lowercase()
                    }
                    
                    ads.add(AdItem(format = normalizedFormat, adId = adId))
                } else {
                    Log.w(TAG, "Invalid format item: $item (expected FORMAT:ad_id)")
                }
            }
            
            if (ads.isEmpty()) {
                Log.w(TAG, "No valid ads found in config string")
                return null
            }
            
            AdConfig(adPlace = adPlace, ads = ads)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing config string", e)
            null
        }
    }
    
    /**
     * Preload waterfall ads for a specific ad place
     * Tries each format in sequence until one succeeds
     * @param context Activity context
     * @param activityName Name of activity for tracking
     * @param adPlace Ad place identifier (e.g., "full_screen_splash")
     * @param configString Config string in format "INTER:ad_id,OPEN:ad_id,FULL_NATIVE:ad_id"
     * @param callback Optional callback when preload completes
     */
    fun preload(
        context: Context,
        activityName: String,
        adPlace: String,
        configString: String,
        callback: ((WaterfallPreloadResult) -> Unit)? = null
    ) {
        // Check if ads are disabled
        if (AdsHelper.isDisableAllAd() || AdsHelper.isDisableObdAd()) {
            Log.d(TAG, "Ads disabled, skipping preload for $adPlace")
            callback?.invoke(WaterfallPreloadResult(false, null, adPlace, "Ads disabled"))
            return
        }
        
        // Check if config string is empty
        if (configString.isBlank()) {
            Log.e(TAG, "Config string is empty for $adPlace")
            callback?.invoke(WaterfallPreloadResult(false, null, adPlace, "Empty config"))
            return
        }
        
        // Parse config string
        val config = parseConfigString(configString, adPlace)
        
        if (config == null || config.ads.isEmpty()) {
            Log.e(TAG, "Failed to parse config for place: $adPlace")
            callback?.invoke(WaterfallPreloadResult(false, null, adPlace, "Parse failed"))
            return
        }
        
        // Cache the config
        adConfigCache[adPlace] = config
        
        // Store callback
        if (callback != null) {
            preloadCallbacks[adPlace] = callback
        }
        
        // Check if already preloaded
        if (preloadedAds.containsKey(adPlace)) {
            val (format, _) = preloadedAds[adPlace]!!
            Log.d(TAG, "Ad already preloaded for $adPlace: $format")
            callback?.invoke(WaterfallPreloadResult(true, format, adPlace))
            return
        }
        
        // Start waterfall preload
        preloadWaterfall(context, activityName, config, 0)
    }
    
    /**
     * Recursive waterfall preload - tries each format sequentially
     */
    private fun preloadWaterfall(
        context: Context,
        activityName: String,
        config: AdConfig,
        index: Int
    ) {
        if (index >= config.ads.size) {
            // All formats failed
            Log.w(TAG, "All ad formats failed for ${config.adPlace}")
            val callback = preloadCallbacks.remove(config.adPlace)
            callback?.invoke(WaterfallPreloadResult(false, null, config.adPlace, "All formats failed"))
            return
        }
        
        val adItem = config.ads[index]
        val adPlaceConstant = "${config.adPlace}"
        
        Log.d(TAG, "Preloading format: ${adItem.format} (${adItem.adId}) for ${config.adPlace}")
        
        when (adItem.format.lowercase()) {
            "inter", "interstitial" -> {
                preloadInterstitial(context, activityName, config.adPlace, adPlaceConstant, adItem.adId, index, config)
            }
            "app_open", "appopen", "open" -> {
                preloadAppOpen(context, activityName, config.adPlace, adPlaceConstant, adItem.adId, index, config)
            }
            "full_native", "native", "native_full" -> {
                preloadNative(context, activityName, config.adPlace, adPlaceConstant, adItem.adId, index, config)
            }
            else -> {
                Log.w(TAG, "Unknown ad format: ${adItem.format}, skipping")
                preloadWaterfall(context, activityName, config, index + 1)
            }
        }
    }
    
    /**
     * Preload interstitial ad
     */
    private fun preloadInterstitial(
        context: Context,
        activityName: String,
        adPlace: String,
        adPlaceConstant: String,
        adId: String,
        currentIndex: Int,
        config: AdConfig
    ) {
        val listAdId = listOf(
            AdsInterMultiPreload.AdIdModel().apply {
                this.adId = adId
                adName = "${adPlace}_inter"
            }
        )
        
        AdsInterMultiPreload.preloadMultipleInterAds(
            context as? Activity ?: return,
            YNMAirBridge.AppData(activityName, adPlaceConstant),
            listAdId,
            adPlaceConstant,
            object : YNMAdsCallbacks(
                YNMAirBridge.AppData(activityName, adPlaceConstant),
                YNMAds.INTERSTITIAL
            ) {
                override fun onAdLoaded() {
                    super.onAdLoaded()
                    Log.d(TAG, "Interstitial ad preloaded successfully for $adPlace")
                    preloadedAds[adPlace] = Pair("inter", adPlaceConstant)
                    val callback = preloadCallbacks.remove(adPlace)
                    callback?.invoke(WaterfallPreloadResult(true, "inter", adPlace))
                }
                
                override fun onAdFailedToLoad(adError: AdsError?) {
                    super.onAdFailedToLoad(adError)
                    Log.w(TAG, "Interstitial ad failed for $adPlace: ${adError?.message}")
                    // Try next format
                    preloadWaterfall(context, activityName, config, currentIndex + 1)
                }
            }
        )
    }
    
    /**
     * Preload app open ad
     */
    private fun preloadAppOpen(
        context: Context,
        activityName: String,
        adPlace: String,
        adPlaceConstant: String,
        adId: String,
        currentIndex: Int,
        config: AdConfig
    ) {
        val listAdId = listOf(
            AdsAppOpenMultiPreload.AdIdModel().apply {
                this.adId = adId
                adName = "${adPlace}_app_open"
            }
        )
        
        AdsAppOpenMultiPreload.preloadMultipleAppOpenAds(
            context as? Activity ?: return,
            YNMAirBridge.AppData(activityName, adPlaceConstant),
            listAdId,
            adPlaceConstant,
            object : YNMAdsCallbacks(
                YNMAirBridge.AppData(activityName, adPlaceConstant),
                YNMAds.INTERSTITIAL
            ) {
                override fun onAdLoaded() {
                    super.onAdLoaded()
                    Log.d(TAG, "App Open ad preloaded successfully for $adPlace")
                    preloadedAds[adPlace] = Pair("app_open", adPlaceConstant)
                    val callback = preloadCallbacks.remove(adPlace)
                    callback?.invoke(WaterfallPreloadResult(true, "app_open", adPlace))
                }
                
                override fun onAdFailedToLoad(adError: AdsError?) {
                    super.onAdFailedToLoad(adError)
                    Log.w(TAG, "App Open ad failed for $adPlace: ${adError?.message}")
                    // Try next format
                    preloadWaterfall(context, activityName, config, currentIndex + 1)
                }
            }
        )
    }
    
    /**
     * Preload native ad
     */
    private fun preloadNative(
        context: Context,
        activityName: String,
        adPlace: String,
        adPlaceConstant: String,
        adId: String,
        currentIndex: Int,
        config: AdConfig
    ) {
        val listAdId = listOf(
            AdsNativeMultiPreload.AdIdModel().apply {
                this.adId = adId
                adName = "${adPlace}_native"
            }
        )
        
        AdsNativeMultiPreload.preloadMultipleNativeAds(
            context as? Activity ?: return,
            YNMAirBridge.AppData(activityName, adPlaceConstant),
            listAdId,
            adPlaceConstant,
            object : YNMAdsCallbacks() {
                override fun onNativeAdLoaded(nativeAd: NativeAd) {
                    super.onNativeAdLoaded(nativeAd)
                    Log.d(TAG, "Native ad preloaded successfully for $adPlace")
                    preloadedAds[adPlace] = Pair("full_native", adPlaceConstant)
                    val callback = preloadCallbacks.remove(adPlace)
                    callback?.invoke(WaterfallPreloadResult(true, "full_native", adPlace))
                }
                
                override fun onAdFailedToLoad(adError: AdsError?) {
                    super.onAdFailedToLoad(adError)
                    Log.w(TAG, "Native ad failed for $adPlace: ${adError?.message}")
                    // Try next format
                    preloadWaterfall(context, activityName, config, currentIndex + 1)
                }
            }
        )
    }
    
    /**
     * Show preloaded ad for a specific ad place
     * @param activity Activity context
     * @param activityName Name of activity for tracking
     * @param adPlace Ad place identifier
     * @param callback Callback when ad is shown or dismissed
     */
    fun showPreload(
        activity: Activity,
        activityName: String,
        adPlace: String,
        callback: ((Boolean) -> Unit)? = null
    ) {
        val preloaded = preloadedAds[adPlace]
        
        if (preloaded == null) {
            Log.w(TAG, "No preloaded ad found for $adPlace")
            callback?.invoke(false)
            return
        }
        
        val (format, adPlaceConstant) = preloaded
        
        Log.d(TAG, "Showing preloaded ad for $adPlace: format=$format")
        
        when (format) {
            "inter" -> {
                showPreloadedInterstitial(activity, activityName, adPlace, adPlaceConstant, callback)
            }
            "app_open" -> {
                showPreloadedAppOpen(activity, activityName, adPlace, adPlaceConstant, callback)
            }
            "full_native" -> {
                showPreloadedNativeFullScreen(activity, activityName, adPlace, adPlaceConstant, callback)
            }
            else -> {
                Log.w(TAG, "Unknown format: $format")
                callback?.invoke(false)
            }
        }
    }
    
    /**
     * Show preloaded interstitial ad
     */
    private fun showPreloadedInterstitial(
        activity: Activity,
        activityName: String,
        adPlace: String,
        adPlaceConstant: String,
        callback: ((Boolean) -> Unit)?
    ) {
        if (!AdsInterMultiPreload.isAdLoaded(adPlaceConstant)) {
            Log.w(TAG, "Interstitial ad not loaded for $adPlaceConstant")
            callback?.invoke(false)
            return
        }
        
        AdsInterMultiPreload.showPreloadedInterAdWithLoading(
            activity,
            adPlaceConstant,
            5000,
            object : YNMAdsCallbacks(
                YNMAirBridge.AppData(activityName, adPlaceConstant),
                YNMAds.INTERSTITIAL
            ) {
                override fun onNextAction(isShown: Boolean) {
                    super.onNextAction(isShown)
                    callback?.invoke(isShown)
                }
                
                override fun onInterstitialShow() {
                    super.onInterstitialShow()
                }
                
                override fun onAdClosed() {
                    super.onAdClosed()
                    AdsInterMultiPreload.destroyPreloadedAd(adPlaceConstant)
                }
                
                override fun onAdFailedToLoad(adError: AdsError?) {
                    super.onAdFailedToLoad(adError)
                    callback?.invoke(false)
                }
            }
        )
    }
    
    /**
     * Show preloaded app open ad
     */
    private fun showPreloadedAppOpen(
        activity: Activity,
        activityName: String,
        adPlace: String,
        adPlaceConstant: String,
        callback: ((Boolean) -> Unit)?
    ) {
        if (!AdsAppOpenMultiPreload.isAdLoaded(adPlaceConstant)) {
            Log.w(TAG, "App Open ad not loaded for $adPlaceConstant")
            callback?.invoke(false)
            return
        }
        
        AdsAppOpenMultiPreload.showPreloadedAppOpenAdWithLoading(
            activity,
            adPlaceConstant,
            3000, // timeout 3 seconds
            object : YNMAdsCallbacks(
                YNMAirBridge.AppData(activityName, adPlaceConstant),
                YNMAds.APP_OPEN
            ) {
                override fun onNextAction(isShown: Boolean) {
                    super.onNextAction(isShown)
                    callback?.invoke(isShown)
                }
                
                override fun onAdClosed() {
                    super.onAdClosed()
                    AdsAppOpenMultiPreload.destroyPreloadedAd(adPlaceConstant)
                }
                
                override fun onAdFailedToLoad(adError: AdsError?) {
                    super.onAdFailedToLoad(adError)
                    callback?.invoke(false)
                }
            }
        )
    }
    
    /**
     * Show preloaded native ad as full screen overlay
     */
    private fun showPreloadedNativeFullScreen(
        activity: Activity,
        activityName: String,
        adPlace: String,
        adPlaceConstant: String,
        callback: ((Boolean) -> Unit)?
    ) {
        if (!AdsNativeMultiPreload.isAdLoaded(adPlaceConstant)) {
            Log.w(TAG, "Native ad not loaded for $adPlaceConstant")
            callback?.invoke(false)
            return
        }
        
        try {
            // Inflate the full screen layout
            val inflater = android.view.LayoutInflater.from(activity)
            val fullScreenView = inflater.inflate(
                com.jrm.R.layout.layout_native_fullscreen_waterfall,
                null
            )
            
            // Get the YNMNativeAdView
            val nativeAdView = fullScreenView.findViewById<YNMNativeAdView>(com.jrm.R.id.native_onboarding_full)
            
            // Get the next button
            val btnNext = fullScreenView.findViewById<View>(
                com.jrm.R.id.btn_next
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
            AdsNativeMultiPreload.showPreloadedNativeAd(
                activity,
                nativeAdView,
                adPlaceConstant,
                com.jrm.R.layout.custom_full_screen_native_ads,
                com.jrm.R.layout.custom_full_screen_native_ads,
            )
            
            // Handle next button click
            btnNext.setOnClickListener {
                // Remove the full screen view
                rootView.removeView(fullScreenView)
                // Destroy the ad
                AdsNativeMultiPreload.destroyPreloadedAd(adPlaceConstant)
                // Clear from preloaded ads map
                preloadedAds.remove(adPlace)
                // Invoke callback
                callback?.invoke(true)
                Log.d(TAG, "Native full screen ad closed by user")
            }
            
            Log.d(TAG, "Native full screen ad shown for $adPlace")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing native full screen ad", e)
            callback?.invoke(false)
        }
    }
    
    /**
     * Show preloaded native ad in a view
     * @param activity Activity context
     * @param adView Native ad view to display the ad
     * @param adPlace Ad place identifier
     * @param layoutResId Layout resource ID for native ad
     */
    fun showPreloadedNative(
        activity: Activity,
        adView: View,
        adPlace: String,
        layoutResId: Int
    ): Boolean {
        val preloaded = preloadedAds[adPlace]
        
        if (preloaded == null) {
            Log.w(TAG, "No preloaded native ad found for $adPlace")
            return false
        }
        
        val (format, adPlaceConstant) = preloaded
        
        if (format != "full_native") {
            Log.w(TAG, "Preloaded ad is not native format: $format")
            return false
        }
        
        if (!AdsNativeMultiPreload.isAdLoaded(adPlaceConstant)) {
            Log.w(TAG, "Native ad not loaded for $adPlaceConstant")
            return false
        }
        
        // Cast to YNMNativeAdView if possible
        val nativeAdView = adView as? com.ads.nomyek_admob.ads_components.ads_native.YNMNativeAdView
            ?: return false
        
        AdsNativeMultiPreload.showPreloadedNativeAd(
            activity,
            nativeAdView,
            adPlaceConstant,
            layoutResId,
            layoutResId
        )
        
        return true
    }
    
    /**
     * Check if ad is preloaded for a specific ad place
     */
    fun isPreloaded(adPlace: String): Boolean {
        return preloadedAds.containsKey(adPlace)
    }
    
    /**
     * Get preloaded format for a specific ad place
     */
    fun getPreloadedFormat(adPlace: String): String? {
        return preloadedAds[adPlace]?.first
    }
    
    /**
     * Clear preloaded ad for a specific ad place
     */
    fun clearPreloaded(adPlace: String) {
        val preloaded = preloadedAds.remove(adPlace)
        if (preloaded != null) {
            val (_, adPlaceConstant) = preloaded
            // Destroy the preloaded ad
            AdsInterMultiPreload.destroyPreloadedAd(adPlaceConstant)
            AdsAppOpenMultiPreload.destroyPreloadedAd(adPlaceConstant)
            AdsNativeMultiPreload.destroyPreloadedAd(adPlaceConstant)
            Log.d(TAG, "Cleared preloaded ad for $adPlace")
        }
    }
    
    /**
     * Clear all preloaded ads
     */
    fun clearAll() {
        preloadedAds.keys.forEach { clearPreloaded(it) }
        preloadedAds.clear()
        adConfigCache.clear()
        preloadCallbacks.clear()
    }
}

