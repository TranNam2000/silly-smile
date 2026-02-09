package com.jrm.service

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import com.ads.nomyek_admob.ads_components.YNMAds
import com.ads.nomyek_admob.ads_components.YNMAdsCallbacks
import com.ads.nomyek_admob.ads_components.ads_native.YNMNativeAdView
import com.ads.nomyek_admob.ads_components.wrappers.AdsError
import com.ads.nomyek_admob.event.YNMAirBridge
import com.ads.nomyek_admob.utils.AdsAppOpenMultiPreload
import com.ads.nomyek_admob.utils.AdsInterMultiPreload
import com.ads.nomyek_admob.utils.AdsNativeMultiPreload
import com.google.android.gms.ads.nativead.NativeAd
import com.jrm.BuildConfig
import com.jrm.model.UnitIdConfig
import com.jrm.service.Helper.buildUnitIdConfigList
import com.jrm.utils.AdsHelper
import com.jrm.utils.Logger
import com.jrm.utils.remote_config.RemoteConfigManager


/**
 * Model classes for ad configuration
 */
data class AdConfig(
    val adPlace: String,
    val ads: List<UnitIdConfig>
)

data class AdItem(
    val format: String,
    val adId: String
)

/**
 * Waterfall ad preload result
 */
data class FullScreenPreloadResult(
    val success: Boolean,
    val format: String? = null,
    val adPlace: String? = null,
    val error: String? = null
)


object FullScreenService {
    private const val TAG = "FullScreenService"

    // Cache for loaded ad configs
    private val adConfigCache = mutableMapOf<String, AdConfig>()

    // Track preloaded ads by adPlace
    // Key: adPlace, Value: Pair(format, adPlace constant for preload system)
    private val preloadedAds = mutableMapOf<String, Pair<String, String>>()

    // Callbacks for preload completion
    private val preloadCallbacks = mutableMapOf<String, (FullScreenPreloadResult) -> Unit>()

    // Track if full screen ad is currently showing
    @Volatile
    var isFullScreenAdShowing: Boolean = false
        private set

    private fun getUnitId(config: UnitIdConfig): String {
        return if (BuildConfig.DEBUG && config.unitIdTest.isNotEmpty()) {
            config.unitIdTest
        } else {
            config.unitId
        }
    }

    /**
     * Parse simple string format: "INTER:ad_id1,OPEN:ad_id2,FULL_NATIVE:ad_id3"
     * @param configString String in format "FORMAT:ad_id,FORMAT:ad_id,..."
     * @param adPlace Ad place identifier
     * @return AdConfig or null if parsing fails
     */

    /**
     * Preload waterfall ads for a specific ad place
     * Tries each format in sequence until one succeeds
     * @param context Activity context
     * @param activityName Name of activity for tracking
     * @param adPlace Ad place identifier (e.g., "full_screen_splash")
     * @param configString Config string in format "INTER:ad_id,OPEN:ad_id,FULL_NATIVE:ad_id"
     * @param callback Optional callback when preload completes
     */
    fun loadAdsFullScreen(
        context: Context,
        activityName: String,
        adPlace: String,
        callback: ((FullScreenPreloadResult) -> Unit)? = null
    ) {
        // Check if ads are disabled
        if (AdsHelper.isDisableAllAd() || AdsHelper.isDisableObdAd()) {
            Logger.d("Ads disabled, skipping preload for $adPlace")
            callback?.invoke(FullScreenPreloadResult(false, null, adPlace, "Ads disabled"))
            return
        }

        // Check if config has placements
        val adConfigModel = RemoteConfigManager.instance?.adConfig
        if ((adConfigModel?.adPlacements?.size ?: 0) == 0) {
            Logger.e("Config has no placements for $adPlace")
            callback?.invoke(FullScreenPreloadResult(false, null, adPlace, "Empty config"))
            return
        }

        // Build unit id list from ad config (same as NativeService)
        val unitIdConfigList = buildUnitIdConfigList(adConfigModel, adPlace)

        if (unitIdConfigList.isEmpty()) {
            Logger.e("Failed to parse config for place: $adPlace")
            callback?.invoke(FullScreenPreloadResult(false, null, adPlace, "Parse failed"))
            return
        }

        val adConfig = AdConfig(adPlace = adPlace, ads = unitIdConfigList)

        // Cache the config
        adConfigCache[adPlace] = adConfig

        // Store callback
        if (callback != null) {
            preloadCallbacks[adPlace] = callback
        }

        // Check if already preloaded
        if (preloadedAds.containsKey(adPlace)) {
            val (format, _) = preloadedAds[adPlace]!!
            Logger.d("Ad already preloaded for $adPlace: $format")
            callback?.invoke(FullScreenPreloadResult(true, format, adPlace))
            return
        }

        // Start waterfall preload
        preloadWaterfall(context, activityName, adConfig, 0)
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
            Logger.w("All ad formats failed for ${config.adPlace}")
            val callback = preloadCallbacks.remove(config.adPlace)
            callback?.invoke(
                FullScreenPreloadResult(
                    false,
                    null,
                    config.adPlace,
                    "All formats failed"
                )
            )
            return
        }

        val adItem = config.ads[index]
        val adPlaceConstant = "${config.adPlace}"
        val unitId = getUnitId(adItem)

        Logger.d("Preloading format: ${adItem.format} ($unitId) for ${config.adPlace}")

        when (adItem.format.lowercase()) {
            "inter", "interstitial" -> {
                preloadInterstitial(
                    context,
                    activityName,
                    config.adPlace,
                    adPlaceConstant,
                    unitId,
                    index,
                    config
                )
            }

            "app_open", "appopen", "open" -> {
                preloadAppOpen(
                    context,
                    activityName,
                    config.adPlace,
                    adPlaceConstant,
                    unitId,
                    index,
                    config
                )
            }

            "full_native", "native", "native_full" -> {
                preloadNative(
                    context,
                    activityName,
                    config.adPlace,
                    adPlaceConstant,
                    unitId,
                    index,
                    config
                )
            }

            else -> {
                Logger.w("Unknown ad format: ${adItem.format}, skipping")
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

                    Logger.d("Interstitial ad preloaded successfully for $adPlace")
                    preloadedAds[adPlace] = Pair("inter", adPlaceConstant)
                    val callback = preloadCallbacks.remove(adPlace)
                    callback?.invoke(FullScreenPreloadResult(true, "inter", adPlace))
                }

                override fun onAdFailedToLoad(adError: AdsError?) {
                    super.onAdFailedToLoad(adError)
                    Logger.w("Interstitial ad failed for $adPlace: ${adError?.message}")
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
                    Logger.d("App Open ad preloaded successfully for $adPlace")
                    preloadedAds[adPlace] = Pair("app_open", adPlaceConstant)
                    val callback = preloadCallbacks.remove(adPlace)
                    callback?.invoke(FullScreenPreloadResult(true, "app_open", adPlace))
                }

                override fun onAdFailedToLoad(adError: AdsError?) {
                    super.onAdFailedToLoad(adError)
                    Logger.w("App Open ad failed for $adPlace: ${adError?.message}")
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

        Logger.d("🔵 [FULL_SCREEN] Preloading native ad - adPlace=$adPlace, adPlaceConstant=$adPlaceConstant, adId=$adId")
        AdsNativeMultiPreload.preloadMultipleNativeAds(
            context as? Activity ?: return,
            YNMAirBridge.AppData(activityName, adPlaceConstant),
            listAdId,
            adPlaceConstant,
            object : YNMAdsCallbacks() {
                override fun onNativeAdLoaded(nativeAd: NativeAd) {
                    super.onNativeAdLoaded(nativeAd)
                    Logger.d("🔵 [FULL_SCREEN] Native ad preloaded successfully - adPlace=$adPlace, adPlaceConstant=$adPlaceConstant")
                    val isLoadedCheck = AdsNativeMultiPreload.isAdLoaded(adPlaceConstant)
                    Logger.d("🔵 [FULL_SCREEN] Verification after preload: isAdLoaded($adPlaceConstant) = $isLoadedCheck")
                    preloadedAds[adPlace] = Pair("full_native", adPlaceConstant)
                    val callback = preloadCallbacks.remove(adPlace)

                    callback?.invoke(FullScreenPreloadResult(true, "full_native", adPlace))
                }

                override fun onAdFailedToLoad(adError: AdsError?) {
                    super.onAdFailedToLoad(adError)
                    Logger.w("Native ad failed for $adPlace: ${adError?.message}")
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
     * @param callback Callback when ad is shown or dismissed (true = shown and closed, false = failed to show)
     */
    fun showPreload(
        activity: Activity,
        activityName: String,
        adPlace: String,
        callback: ((Boolean) -> Unit)? = null
    ) {
        if (activity.isFinishing || activity.isDestroyed) {
            Logger.w("Activity finishing/destroyed, skip show for $adPlace")
            callback?.invoke(false)
            return
        }

        val preloaded = preloadedAds[adPlace]

        if (preloaded == null) {
            Logger.w("No preloaded ad found for $adPlace")
            callback?.invoke(false)
            return
        }

        val (format, adPlaceConstant) = preloaded

        Logger.d("Showing preloaded ad for $adPlace: format=$format")


        // Mark as showing AFTER all checks passed and callback is wrapped
        isFullScreenAdShowing = true

        when (format) {
            "inter" -> {
                showPreloadedInterstitial(
                    activity,
                    activityName,
                    adPlace,
                    adPlaceConstant, callback
                )
            }

            "app_open" -> {
                showPreloadedAppOpen(activity, activityName, adPlace, adPlaceConstant, callback)
            }

            "full_native" -> {
                showPreloadedNativeFullScreen(
                    activity,
                    activityName,
                    adPlace,
                    adPlaceConstant,
                    callback
                )
            }

            else -> {
                Logger.w("Unknown format: $format")
                isFullScreenAdShowing = false
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
            loadAdsFullScreen(activity, activityName, adPlace) { result ->
                if (result.success) {
                    postShowInterstitialIfActivityAlive(
                        activity,
                        activityName,
                        adPlaceConstant,
                        callback
                    )
                } else {
                    Handler(Looper.getMainLooper()).post { callback?.invoke(false) }
                }
            }
            return
        }

        postShowInterstitialIfActivityAlive(
            activity,
            activityName,
            adPlaceConstant,
            callback
        )
    }

    /**
     * Show interstitial on next frame; skip if activity is finishing/destroyed to avoid WindowLeaked.
     * Callback is invoked only after ad is closed (onAdClosed) or on failure — ensures "show first, then switch screen".
     */
    private fun postShowInterstitialIfActivityAlive(
        activity: Activity,
        activityName: String,
        adPlaceConstant: String,
        callback: ((Boolean) -> Unit)?
    ) {

        val timeoutFullScreenMs = (RemoteConfigManager.instance?.timeOutFullScreenAd ?: 5L) * 1000L
        AdsInterMultiPreload.showPreloadedInterAdWithLoading(
            context = activity,
            placeName = adPlaceConstant,
            timeOut = timeoutFullScreenMs,
            callback = object : YNMAdsCallbacks(
                YNMAirBridge.AppData(activityName, adPlaceConstant),
                YNMAds.INTERSTITIAL
            ) {
                override fun onNextAction(isShown: Boolean) {
                    super.onNextAction(isShown)
                    callback?.invoke(isShown)
                }

                override fun onInterstitialShow() {
                    super.onInterstitialShow()
                    // Ad is showing, callback will be called in onNextAction or onAdClosed
                }

                override fun onAdClosed() {
                    super.onAdClosed()
                    AdsInterMultiPreload.destroyPreloadedAd(adPlaceConstant)
                    // Ensure callback is called if not already called
                    // Note: onNextAction should have been called, but ensure state is reset
                    isFullScreenAdShowing = false
                    callback?.invoke(true)
                }

                override fun onAdFailedToLoad(adError: AdsError?) {
                    super.onAdFailedToLoad(adError)
                    isFullScreenAdShowing = false
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
            Logger.w("App Open ad not loaded for $adPlaceConstant")
            callback?.invoke(false)
            return
        }

        val timeoutFullScreenMs = (RemoteConfigManager.instance?.timeOutFullScreenAd ?: 5L) * 1000L
        AdsAppOpenMultiPreload.showPreloadedAppOpenAdWithLoading(
            context = activity,
            placeName = adPlaceConstant,
            timeOut = timeoutFullScreenMs,
            callback = object : YNMAdsCallbacks(
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
                    // Ensure state is reset if callback wasn't called
                    isFullScreenAdShowing = false
                    callback?.invoke(true)
                }

                override fun onAdFailedToLoad(adError: AdsError?) {
                    super.onAdFailedToLoad(adError)
                    callback?.invoke(false)
                    isFullScreenAdShowing = false
                }
            }
        )
    }

    /**
     * Show preloaded native ad as full screen overlay.
     * Uses native_config.delay_time (seconds) from ad config for native full; shows after delay.
     */
    private fun showPreloadedNativeFullScreen(
        activity: Activity,
        activityName: String,
        adPlace: String,
        adPlaceConstant: String,
        callback: ((Boolean) -> Unit)?
    ) {
        Logger.d("🔵 [FULL_SCREEN] Checking native ad for adPlace=$adPlace, adPlaceConstant=$adPlaceConstant")
        val isLoaded = AdsNativeMultiPreload.isAdLoaded(adPlaceConstant)
        Logger.d("🔵 [FULL_SCREEN] isAdLoaded($adPlaceConstant) = $isLoaded")

        if (!isLoaded) {
            Logger.w("Native ad not loaded for $adPlaceConstant (adPlace=$adPlace)")
            // Try to check with adPlace as fallback
            val isLoadedWithAdPlace = AdsNativeMultiPreload.isAdLoaded(adPlace)
            Logger.d("🔵 [FULL_SCREEN] Fallback check isAdLoaded($adPlace) = $isLoadedWithAdPlace")
            if (isLoadedWithAdPlace) {
                Logger.d("🔵 [FULL_SCREEN] Using adPlace as key instead of adPlaceConstant")
                doShowPreloadedNativeFullScreen(activity, adPlace, adPlace, callback)
                return
            }
            callback?.invoke(false)
            return
        }
        doShowPreloadedNativeFullScreen(activity, adPlace, adPlaceConstant, callback)
    }

    private fun doShowPreloadedNativeFullScreen(
        activity: Activity,
        adPlace: String,
        adPlaceConstant: String,
        callback: ((Boolean) -> Unit)?
    ) {
        if (!AdsNativeMultiPreload.isAdLoaded(adPlaceConstant)) {
            Logger.w("Native ad not loaded for $adPlaceConstant")
            callback?.invoke(false)
            return
        }

        try {
            val rootView =
                activity.window.decorView.findViewById<android.view.ViewGroup>(android.R.id.content)
            if (rootView == null) {
                Logger.e("Root view not found")
                callback?.invoke(false)
                return
            }

            // Check if native_onboarding_full view already exists in activity
            // Search in decorView to find existing views (views added programmatically)
            val decorView = activity.window.decorView
            val existingNativeAdView =
                decorView.findViewById<YNMNativeAdView>(com.jrm.R.id.native_onboarding_full)
            val existingContainer =
                decorView.findViewById<View>(com.jrm.R.id.fullscreen_native_container)
            val existingBtnNext = decorView.findViewById<View>(com.jrm.R.id.btn_next)

            Logger.d("Checking for existing views - nativeAdView: ${existingNativeAdView != null}, container: ${existingContainer != null}, btnNext: ${existingBtnNext != null}")

            val nativeAdView: YNMNativeAdView
            val container: View
            val btnNext: View
            val fullScreenView: View

            if (existingNativeAdView != null && existingContainer != null && existingBtnNext != null) {
                // Use existing views from activity
                Logger.d("Using existing native_onboarding_full view in activity for $adPlace")
                nativeAdView = existingNativeAdView
                container = existingContainer
                btnNext = existingBtnNext

                // Clear previous click listeners to avoid multiple callbacks
                btnNext.setOnClickListener(null)

                // Show the container
                container.visibility = View.VISIBLE
                Logger.d("Container visibility set to VISIBLE for $adPlace")

                // Set click listener
                btnNext.setOnClickListener {
                    // Hide container if using existing view
                    container.visibility = View.GONE
                    // Destroy the ad
                    AdsNativeMultiPreload.destroyPreloadedAd(adPlaceConstant)
                    // Clear from preloaded ads map
                    preloadedAds.remove(adPlace)
                    isFullScreenAdShowing = false
                    // Invoke callback
                    callback?.invoke(true)
                    Logger.d("Native full screen ad closed by user")
                }
            } else {
                // Inflate new layout
                Logger.d("Inflating new layout for native full screen ad for $adPlace")
                val inflater = android.view.LayoutInflater.from(activity)
                fullScreenView = inflater.inflate(
                    com.jrm.R.layout.layout_native_fullscreen_waterfall,
                    null
                )

                // Get the YNMNativeAdView
                nativeAdView =
                    fullScreenView.findViewById(com.jrm.R.id.native_onboarding_full)
                if (nativeAdView == null) {
                    Logger.e("Native ad view not found in layout")
                    callback?.invoke(false)
                    return
                }

                // Get the next button
                btnNext = fullScreenView.findViewById<View>(com.jrm.R.id.btn_next)
                if (btnNext == null) {
                    Logger.e("Next button not found in layout")
                    callback?.invoke(false)
                    return
                }

                // Get the container
                container =
                    fullScreenView.findViewById<View>(com.jrm.R.id.fullscreen_native_container)
                if (container == null) {
                    Logger.e("Container not found in layout")
                    callback?.invoke(false)
                    return
                }

                // Add to root view
                rootView.addView(fullScreenView)

                // Show the container
                container.visibility = View.VISIBLE

                // Set click listener
                btnNext.setOnClickListener {
                    Logger.d("Native full screen ad close button clicked")
                    // Remove the full screen view
                    rootView.removeView(fullScreenView)
                    // Destroy the ad
                    AdsNativeMultiPreload.destroyPreloadedAd(adPlaceConstant)
                    // Clear from preloaded ads map
                    preloadedAds.remove(adPlace)
                    isFullScreenAdShowing = false
                    // Invoke callback
                    callback?.invoke(true)
                    Logger.d("Native full screen ad closed by user")
                }
            }


            // Show the native ad using YNMNativeAdView's built-in method
            // YNMNativeAdView will automatically load and display the ad
            Logger.d("Calling AdsNativeMultiPreload.showPreloadedNativeAd for $adPlace")
            AdsNativeMultiPreload.showPreloadedNativeAd(
                activity,
                nativeAdView,
                adPlaceConstant,
                com.jrm.R.layout.custom_full_screen_native_ads,
                com.jrm.R.layout.custom_full_screen_native_ads,
                callback = {
                    if (!it) {
                        isFullScreenAdShowing = false
                    }
                }
            )

            Logger.d("Native full screen ad shown successfully for $adPlace")
        } catch (e: Exception) {
            Logger.e("Error showing native full screen ad", e)
            callback?.invoke(false)
        }
    }


    /**
     * Check if ad is preloaded for a specific ad place
     */
    fun isPreloaded(adPlace: String): Boolean {
        val hasEntry = preloadedAds.containsKey(adPlace)
        if (!hasEntry) {
            return false
        }

        // Verify that the ad is actually loaded in the SDK
        val preloaded = preloadedAds[adPlace]
        if (preloaded == null) {
            return false
        }

        val (format, adPlaceConstant) = preloaded
        Logger.d("🔵 [FULL_SCREEN] isPreloaded($adPlace) - format=$format, adPlaceConstant=$adPlaceConstant")

        // Check if ad is actually loaded in the SDK
        val isActuallyLoaded = when (format) {
            "inter" -> AdsInterMultiPreload.isAdLoaded(adPlaceConstant)
            "app_open" -> AdsAppOpenMultiPreload.isAdLoaded(adPlaceConstant)
            "full_native" -> AdsNativeMultiPreload.isAdLoaded(adPlaceConstant)
            else -> false
        }

        Logger.d("🔵 [FULL_SCREEN] isPreloaded($adPlace) - SDK check result: $isActuallyLoaded")

        // If not actually loaded, remove from cache to prevent false positives
        if (!isActuallyLoaded) {
            Logger.w("🔵 [FULL_SCREEN] Ad marked as preloaded but not actually loaded, removing from cache")
            preloadedAds.remove(adPlace)
            return false
        }

        return true
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
            Logger.d("Cleared preloaded ad for $adPlace")
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
        isFullScreenAdShowing = false
    }


}