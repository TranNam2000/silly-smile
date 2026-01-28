package com.jrm.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.ads.nomyek_admob.ads_components.YNMAdsCallbacks
import com.ads.nomyek_admob.ads_components.wrappers.AdsError
import com.ads.nomyek_admob.event.YNMAirBridge
import com.ads.nomyek_admob.max.AppLovinCallback
import com.ads.nomyek_admob.max.MaxNativePreload
import com.ads.nomyek_admob.utils.AdsNativeMultiPreload
import com.applovin.mediation.MaxError
import com.google.android.gms.ads.nativead.NativeAd
import com.jrm.utils.AdsHelper
import com.jrm.utils.purchase.IAPHelper
import java.lang.ref.WeakReference

/**
 * Waterfall Native Ad Manager
 * Handles loading native ads in waterfall fashion supporting both MAX Native and Admob Native
 * 
 * Format: "MAX_NATIVE:31211d25a876bf39,NATIVE:ca-app-pub-xxx/xxx,NATIVE:ca-app-pub-yyy/yyy"
 * 
 * Usage:
 * 1. Preload: WaterfallNativeAdManager.preload(context, activityName, adPlace, configString, callback)
 * 2. Check: WaterfallNativeAdManager.isPreloaded(adPlace)
 * 3. Show: WaterfallNativeAdManager.show(activity, adView, adPlace, layoutId, waitForLoad)
 * 4. Cancel: WaterfallNativeAdManager.cancelCallbacks(adPlace) - in onDestroy()
 * 5. Destroy: WaterfallNativeAdManager.destroy(adPlace) - in onDestroy()
 * 
 * Activity Lifecycle Best Practices:
 * ```
 * class MyActivity : Activity() {
 *     override fun onCreate() {
 *         // Preload in previous activity (e.g., SplashActivity)
 *         WaterfallNativeAdManager.preload(this, "MyActivity", adPlace, config) { result ->
 *             // Handle result
 *         }
 *     }
 *     
 *     override fun onResume() {
 *         // Show ad when activity is visible
 *         WaterfallNativeAdManager.show(this, adView, adPlace, waitForLoad = true) { success ->
 *             if (success) adView.visibility = View.VISIBLE
 *         }
 *     }
 *     
 *     override fun onDestroy() {
 *         super.onDestroy()
 *         // Cancel pending callbacks to avoid memory leaks
 *         WaterfallNativeAdManager.cancelCallbacks(adPlace)
 *         // Destroy ad and clear cache
 *         WaterfallNativeAdManager.destroy(adPlace)
 *     }
 * }
 * ```
 * 
 * Safety Features:
 * - Uses WeakReference for activity/view to prevent memory leaks
 * - Checks activity.isFinishing/isDestroyed before showing ads
 * - Callbacks can be cancelled when activity is destroyed
 * - Automatic cleanup of callbacks after notification
 */
object WaterfallNativeAdManager {
    
    private const val TAG = "WaterfallNativeAdMgr"
    
    // Cache for preloaded ads
    private val preloadedAdsCache = mutableMapOf<String, PreloadedAdData>()
    
    // Callbacks waiting for preload completion
    private val preloadCallbacks = mutableMapOf<String, MutableList<(WaterfallNativeResult) -> Unit>>()
    
    /**
     * Result data class
     */
    data class WaterfallNativeResult(
        val success: Boolean,
        val adType: String, // "max_native" or "admob_native"
        val adPlace: String,
        val nativeAd: NativeAd? = null // Only for admob native
    )
    
    /**
     * Data class to hold preloaded ad information
     */
    private data class PreloadedAdData(
        val adPlace: String,
        val screenName: String,
        val adItems: List<AdItem>,
        val layoutAdmob: Int,
        val layoutMax: Int,
        var currentIndex: Int = 0,
        var isPreloaded: Boolean = false,
        var isLoading: Boolean = false,
        var loadedAdType: String? = null, // "max_native" or "admob_native"
        var loadedNativeAd: NativeAd? = null // For admob native
    )
    
    /**
     * Ad item in waterfall
     */
    private data class AdItem(
        val type: String, // "MAX_NATIVE" or "NATIVE"
        val adId: String
    )
    
    /**
     * Parse config string format "MAX_NATIVE:id1,NATIVE:id2,NATIVE:id3"
     */
    private fun parseConfigString(configString: String): List<AdItem> {
        if (configString.isBlank()) return emptyList()
        
        return configString.split(",").mapNotNull { item ->
            val parts = item.trim().split(":")
            if (parts.size == 2) {
                val type = parts[0].trim()
                val adId = parts[1].trim()
                if ((type == "MAX_NATIVE" || type == "NATIVE") && adId.isNotEmpty()) {
                    AdItem(type, adId)
                } else null
            } else null
        }
    }
    
    /**
     * Preload native ads in waterfall fashion
     * @param context Context
     * @param activityName Activity name for tracking
     * @param adPlace Ad placement identifier
     * @param configString Config string format "MAX_NATIVE:id1,NATIVE:id2,NATIVE:id3"
     * @param layoutAdmob Layout resource ID for Admob native
     * @param layoutMax Layout resource ID for MAX native
     * @param callback Callback with result
     */
    @JvmStatic
    fun preload(
        context: Context,
        activityName: String,
        adPlace: String,
        configString: String,
        layoutAdmob: Int,
        layoutMax: Int,
        callback: ((WaterfallNativeResult) -> Unit)? = null
    ) {
        Log.d(TAG, "[$adPlace] preload() called - config: $configString")
        
        // Check if premium or ads disabled
        if (IAPHelper.isPremium() || AdsHelper.isDisableAllAd()) {
            Log.d(TAG, "[$adPlace] Premium or ads disabled, skipping preload")
            callback?.invoke(WaterfallNativeResult(false, "", adPlace))
            return
        }
        
        // Check if already preloaded or loading
        val existingData = preloadedAdsCache[adPlace]
        if (existingData != null) {
            if (existingData.isPreloaded) {
                Log.d(TAG, "[$adPlace] Already preloaded with type: ${existingData.loadedAdType}")
                callback?.invoke(
                    WaterfallNativeResult(
                        true,
                        existingData.loadedAdType ?: "",
                        adPlace,
                        existingData.loadedNativeAd
                    )
                )
                return
            }
            if (existingData.isLoading) {
                Log.d(TAG, "[$adPlace] Already loading, adding callback to queue")
                callback?.let { addPreloadCallback(adPlace, it) }
                return
            }
        }
        
        // Parse config
        val adItems = parseConfigString(configString)
        if (adItems.isEmpty()) {
            Log.e(TAG, "[$adPlace] No valid ad items found in config: $configString")
            callback?.invoke(WaterfallNativeResult(false, "", adPlace))
            return
        }
        
        Log.d(TAG, "[$adPlace] Starting waterfall with ${adItems.size} items")
        
        // Store preload data
        val preloadData = PreloadedAdData(
            adPlace = adPlace,
            screenName = activityName,
            adItems = adItems,
            layoutAdmob = layoutAdmob,
            layoutMax = layoutMax,
            currentIndex = 0,
            isPreloaded = false,
            isLoading = true
        )
        preloadedAdsCache[adPlace] = preloadData
        
        // Add callback
        callback?.let { addPreloadCallback(adPlace, it) }
        
        // Start waterfall
        com.ads.nomyek_admob.ads_components.YNMAds.getInstance().setInitCallback {
            preloadNextInWaterfall(context, activityName, preloadData)
        }
    }
    
    /**
     * Preload next ad in waterfall
     */
    private fun preloadNextInWaterfall(
        context: Context,
        activityName: String,
        preloadData: PreloadedAdData
    ) {
        val adPlace = preloadData.adPlace
        
        // Check if we've exhausted all items
        if (preloadData.currentIndex >= preloadData.adItems.size) {
            Log.e(TAG, "[$adPlace] All waterfall items failed")
            preloadData.isLoading = false
            preloadData.isPreloaded = false
            notifyPreloadCallbacks(adPlace, WaterfallNativeResult(false, "", adPlace))
            return
        }
        
        val currentItem = preloadData.adItems[preloadData.currentIndex]
        Log.d(TAG, "[$adPlace] Trying item ${preloadData.currentIndex + 1}/${preloadData.adItems.size} - Type: ${currentItem.type}, ID: ${currentItem.adId}")
        
        when (currentItem.type) {
            "MAX_NATIVE" -> preloadMaxNative(context, activityName, preloadData, currentItem)
            "NATIVE" -> preloadAdmobNative(context, activityName, preloadData, currentItem)
            else -> {
                Log.e(TAG, "[$adPlace] Unknown ad type: ${currentItem.type}")
                preloadData.currentIndex++
                preloadNextInWaterfall(context, activityName, preloadData)
            }
        }
    }
    
    /**
     * Preload MAX Native ad
     */
    private fun preloadMaxNative(
        context: Context,
        activityName: String,
        preloadData: PreloadedAdData,
        adItem: AdItem
    ) {
        val adPlace = preloadData.adPlace
        Log.d(TAG, "[$adPlace] Preloading MAX Native: ${adItem.adId}")
        
        MaxNativePreload.getInstance().preloadNative(
            context,
            adItem.adId,
            adPlace,
            preloadData.layoutMax,
            object : AppLovinCallback() {
                override fun onAdLoaded() {
                    super.onAdLoaded()
                    Log.d(TAG, "[$adPlace] ✅ MAX Native loaded successfully")
                    preloadData.isPreloaded = true
                    preloadData.isLoading = false
                    preloadData.loadedAdType = "max_native"
                    notifyPreloadCallbacks(
                        adPlace,
                        WaterfallNativeResult(true, "max_native", adPlace)
                    )
                }
                
                override fun onAdFailedToLoad(error: MaxError?) {
                    super.onAdFailedToLoad(error)
                    Log.w(TAG, "[$adPlace] ❌ MAX Native failed: ${error?.message}, trying next")
                    preloadData.currentIndex++
                    preloadNextInWaterfall(context, activityName, preloadData)
                }
            }
        )
    }
    
    /**
     * Preload Admob Native ad
     */
    private fun preloadAdmobNative(
        context: Context,
        activityName: String,
        preloadData: PreloadedAdData,
        adItem: AdItem
    ) {
        val adPlace = preloadData.adPlace
        Log.d(TAG, "[$adPlace] Preloading Admob Native: ${adItem.adId}")
        
        val listAdId = listOf(
            AdsNativeMultiPreload.AdIdModel().apply {
                this.adId = adItem.adId
                adName = "${adPlace}_native_${preloadData.currentIndex}"
            }
        )
        
        AdsNativeMultiPreload.preloadMultipleNativeAds(
            context as? Activity ?: run {
                Log.e(TAG, "[$adPlace] Context is not an Activity, skipping")
                preloadData.currentIndex++
                preloadNextInWaterfall(context, activityName, preloadData)
                return
            },
            YNMAirBridge.AppData(activityName, adPlace),
            listAdId,
            adPlace,
            object : YNMAdsCallbacks() {
                override fun onNativeAdLoaded(nativeAd: NativeAd) {
                    super.onNativeAdLoaded(nativeAd)
                    Log.d(TAG, "[$adPlace] ✅ Admob Native loaded successfully")
                    preloadData.isPreloaded = true
                    preloadData.isLoading = false
                    preloadData.loadedAdType = "admob_native"
                    preloadData.loadedNativeAd = nativeAd
                    notifyPreloadCallbacks(
                        adPlace,
                        WaterfallNativeResult(true, "admob_native", adPlace, nativeAd)
                    )
                }
                
                override fun onAdFailedToLoad(adError: AdsError?) {
                    super.onAdFailedToLoad(adError)
                    Log.w(TAG, "[$adPlace] ❌ Admob Native failed: ${adError?.message}, trying next")
                    preloadData.currentIndex++
                    preloadNextInWaterfall(context, activityName, preloadData)
                }
            }
        )
    }
    
    /**
     * Check if ad is preloaded
     */
    @JvmStatic
    fun isPreloaded(adPlace: String): Boolean {
        val data = preloadedAdsCache[adPlace]
        return data?.isPreloaded == true
    }
    
    /**
     * Check if ad is currently loading
     */
    @JvmStatic
    fun isLoading(adPlace: String): Boolean {
        val data = preloadedAdsCache[adPlace]
        return data?.isLoading == true
    }
    
    /**
     * Get load status
     * @return "preloaded", "loading", "failed", or "not_started"
     */
    @JvmStatic
    fun getLoadStatus(adPlace: String): String {
        val data = preloadedAdsCache[adPlace] ?: return "not_started"
        return when {
            data.isPreloaded -> "preloaded"
            data.isLoading -> "loading"
            else -> "failed"
        }
    }
    
    /**
     * Get loaded ad type
     */
    @JvmStatic
    fun getLoadedAdType(adPlace: String): String? {
        return preloadedAdsCache[adPlace]?.loadedAdType
    }
    
    /**
     * Show preloaded native ad
     * @param activity Activity context
     * @param adView Native ad view container
     * @param adPlace Ad placement identifier
     * @param waitForLoad If true, will wait for preload to complete before showing. If false, will fail immediately if not loaded
     * @param callback Callback when ad is shown or failed
     */
    @JvmStatic
    fun show(
        activity: Activity,
        adView: com.ads.nomyek_admob.ads_components.ads_native.YNMNativeAdView,
        adPlace: String,
        waitForLoad: Boolean = true,
        callback: ((Boolean) -> Unit)? = null
    ) {
        Log.d(TAG, "[$adPlace] show() called")
        
        val preloadData = preloadedAdsCache[adPlace]
        
        // Case 1: No preload data at all
        if (preloadData == null) {
            Log.e(TAG, "[$adPlace] No preload data found")
            callback?.invoke(false)
            return
        }
        
        // Case 2: Ad is already loaded
        if (preloadData.isPreloaded) {
            Log.d(TAG, "[$adPlace] Ad already loaded, showing now")
            showLoadedAd(activity, adView, adPlace, preloadData, callback)
            return
        }
        
        // Case 3: Ad is still loading
        if (preloadData.isLoading) {
            if (waitForLoad) {
                Log.d(TAG, "[$adPlace] Ad is loading, waiting for completion...")
                
                // Use WeakReference to avoid memory leak
                val weakActivity = WeakReference(activity)
                val weakAdView = WeakReference(adView)
                
                // Add callback to be notified when load completes
                addPreloadCallback(adPlace) { result ->
                    // Get activity from weak reference
                    val act = weakActivity.get()
                    val view = weakAdView.get()
                    
                    // Check if activity and view are still valid
                    if (act == null || act.isFinishing || act.isDestroyed) {
                        Log.w(TAG, "[$adPlace] Activity is null/finishing/destroyed, skipping show")
                        callback?.invoke(false)
                        return@addPreloadCallback
                    }
                    
                    if (view == null) {
                        Log.w(TAG, "[$adPlace] AdView is null, skipping show")
                        callback?.invoke(false)
                        return@addPreloadCallback
                    }
                    
                if (result.success) {
                    Log.d(TAG, "[$adPlace] Load completed, showing ad")
                    showLoadedAd(act, view, adPlace, preloadData, callback)
                } else {
                    Log.e(TAG, "[$adPlace] Load failed")
                    callback?.invoke(false)
                }
                }
            } else {
                Log.w(TAG, "[$adPlace] Ad is still loading and waitForLoad=false")
                callback?.invoke(false)
            }
            return
        }
        
        // Case 4: Preload failed or not started
        Log.e(TAG, "[$adPlace] Ad not loaded and not loading")
        callback?.invoke(false)
    }
    
    /**
     * Internal method to show already loaded ad
     */
    private fun showLoadedAd(
        activity: Activity,
        adView: com.ads.nomyek_admob.ads_components.ads_native.YNMNativeAdView,
        adPlace: String,
        preloadData: PreloadedAdData,
        callback: ((Boolean) -> Unit)?
    ) {
        when (preloadData.loadedAdType) {
            "max_native" -> {
                Log.d(TAG, "[$adPlace] Showing MAX Native ad with layout: ${preloadData.layoutMax}")
                MaxNativePreload.getInstance().showNative(
                    adPlace,
                    adView,
                    null
                )
                callback?.invoke(true)
            }
            "admob_native" -> {
                Log.d(TAG, "[$adPlace] Showing Admob Native ad with layout: ${preloadData.layoutAdmob}")
                AdsNativeMultiPreload.showPreloadedNativeAd(
                    activity,
                    adView,
                    adPlace,
                    preloadData.layoutAdmob,
                    preloadData.layoutAdmob
                )
                callback?.invoke(true)
            }
            else -> {
                Log.e(TAG, "[$adPlace] Unknown ad type: ${preloadData.loadedAdType}")
                callback?.invoke(false)
            }
        }
    }
    
    /**
     * Cancel all pending callbacks for an ad place
     * Useful when activity is destroyed and you don't want callbacks to fire
     */
    @JvmStatic
    fun cancelCallbacks(adPlace: String) {
        Log.d(TAG, "[$adPlace] cancelCallbacks() called")
        preloadCallbacks.remove(adPlace)
    }
    
    /**
     * Destroy preloaded ad and clear cache
     * This also cancels all pending callbacks
     */
    @JvmStatic
    fun destroy(adPlace: String) {
        Log.d(TAG, "[$adPlace] destroy() called")
        
        val preloadData = preloadedAdsCache[adPlace]
        if (preloadData != null) {
            when (preloadData.loadedAdType) {
                "max_native" -> {
                    // MAX native cleanup is handled internally by MaxNativePreload
                    Log.d(TAG, "[$adPlace] Destroying MAX Native ad")
                }
                "admob_native" -> {
                    Log.d(TAG, "[$adPlace] Destroying Admob Native ad")
                    AdsNativeMultiPreload.destroyPreloadedAd(adPlace)
                }
            }
        }
        
        preloadedAdsCache.remove(adPlace)
        preloadCallbacks.remove(adPlace)
    }
    
    /**
     * Clear all cached ads
     */
    @JvmStatic
    fun clearAll() {
        Log.d(TAG, "clearAll() called")
        preloadedAdsCache.keys.toList().forEach { adPlace ->
            destroy(adPlace)
        }
    }
    
    // Helper methods for callback management
    
    private fun addPreloadCallback(adPlace: String, callback: (WaterfallNativeResult) -> Unit) {
        val callbacks = preloadCallbacks.getOrPut(adPlace) { mutableListOf() }
        callbacks.add(callback)
    }
    
    private fun notifyPreloadCallbacks(adPlace: String, result: WaterfallNativeResult) {
        val callbacks = preloadCallbacks.remove(adPlace) ?: return
        callbacks.forEach { it.invoke(result) }
    }
}

