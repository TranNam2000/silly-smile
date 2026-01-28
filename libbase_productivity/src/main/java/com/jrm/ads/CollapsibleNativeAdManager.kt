package com.jrm.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import com.ads.nomyek_admob.ads_components.YNMAdsCallbacks
import com.ads.nomyek_admob.ads_components.ads_native.YNMNativeAdView
import com.ads.nomyek_admob.ads_components.wrappers.AdsError
import com.ads.nomyek_admob.event.YNMAirBridge
import com.ads.nomyek_admob.max.AppLovinCallback
import com.ads.nomyek_admob.max.MaxNativePreload
import com.ads.nomyek_admob.utils.AdsNativeMultiPreload
import com.applovin.mediation.MaxError
import com.google.android.gms.ads.nativead.NativeAd
import com.jrm.R
import com.jrm.utils.AdsHelper
import com.jrm.utils.purchase.IAPHelper

/**
 * Manager class for collapsible bottom native ad layout
 * Handles preloading, loading, showing, hiding and animation of native ads
 *
 * Usage:
 * 1. Preload ads in previous activity: CollapsibleNativeAdManager.preloadAds(context, "MAX_NATIVE:id1,NATIVE:id2", "placement", "screen")
 * 2. Show preloaded ads in target activity: manager.showPreloadedAd("placement")
 */
class CollapsibleNativeAdManager(
    private var activity: Activity?,
    private var rootLayout: ViewGroup?
) {

    /**
     * Enum for close button position
     */
    enum class CloseButtonPosition {
        LEFT, RIGHT
    }

    private var collapsibleLayout: View? = null
    private var nativeAdView: YNMNativeAdView? = null
    private var btnCloseRight: ImageView? = null
    private var btnCloseLeft: ImageView? = null
    private var background: FrameLayout? = null

    private var isExpanded = false
    private var isAdLoaded = false
    private var closeButtonPosition: CloseButtonPosition = CloseButtonPosition.RIGHT

    private var onAdLoadedCallback: (() -> Unit)? = null
    private var onAdFailedCallback: (() -> Unit)? = null
    private var onCollapseCallback: (() -> Unit)? = null

    companion object {
        private const val TAG = "CollapsibleNativeAd"
        private val preloadedAdsCache = mutableMapOf<String, PreloadedAdData>()
        private val preloadCallbacks = mutableMapOf<String, MutableList<(Boolean) -> Unit>>()

        /**
         * Data class to hold preloaded ad information
         */
        private data class PreloadedAdData(
            val maxNativeId: String?,
            val nativeAdIds: List<AdsNativeMultiPreload.AdIdModel>,
            val adPlace: String,
            val screenName: String,
            var isPreloaded: Boolean = false,
            var isMaxNative: Boolean = false,
            var isLoading: Boolean = false,
            var hasFailed: Boolean = false
        )

        /**
         * Parsed ad data from string format
         */
        private data class ParsedAdData(
            val maxNativeId: String?,
            val nativeAdIds: List<AdsNativeMultiPreload.AdIdModel>
        )

        /**
         * Parse ad ID string format "MAX_NATIVE:id,NATIVE:id,NATIVE:id"
         * @param adIdString String format of ad IDs
         * @return ParsedAdData with separated MAX native and regular native IDs
         */
        private fun parseAdIdString(adIdString: String): ParsedAdData {
            var maxNativeId: String? = null
            val nativeAdIds = mutableListOf<AdsNativeMultiPreload.AdIdModel>()

            adIdString.split(",").forEach { item ->
                val parts = item.trim().split(":")
                if (parts.size == 2) {
                    val type = parts[0].trim()
                    val id = parts[1].trim()

                    when (type) {
                        "MAX_NATIVE" -> {
                            // Only take the first MAX_NATIVE ID
                            if (maxNativeId == null) {
                                maxNativeId = id
                            }
                        }
                        "NATIVE" -> {
                            nativeAdIds.add(AdsNativeMultiPreload.AdIdModel().apply {
                                this.adId = id
                                this.adName = "native_collapsible"
                            })
                        }
                    }
                }
            }

            return ParsedAdData(maxNativeId, nativeAdIds)
        }

        /**
         * Preload ads in advance from another activity
         * @param context Context for loading ads
         * @param adIdString String format "MAX_NATIVE:id,NATIVE:id,NATIVE:id"
         * @param adPlace Ad placement identifier (used as cache key)
         * @param screenName Screen name for tracking
         * @param layoutId Layout resource ID for MAX native ad
         */
        @JvmStatic
        fun preloadAds(
            context: Context,
            adIdString: String,
            adPlace: String,
            screenName: String,
            layoutId: Int
        ) {
            Log.d(TAG, "[$adPlace] preloadAds() called - adIds: $adIdString")

            // Check if already preloaded or loading
            val existingData = preloadedAdsCache[adPlace]
            if (existingData != null) {
                if (existingData.isPreloaded) {
                    Log.d(TAG, "[$adPlace] Ad already preloaded, skipping")
                    return
                }
                if (existingData.isLoading) {
                    Log.d(TAG, "[$adPlace] Ad is already loading, skipping")
                    return
                }
            }

            // Check if premium or ads disabled
            if (IAPHelper.isPremium() || AdsHelper.isDisableAllAd()) {
                Log.d(TAG, "[$adPlace] Premium or ads disabled, skipping preload")
                notifyPreloadCallbacks(adPlace, false)
                return
            }

            val parsedData = parseAdIdString(adIdString)

            // Check if we have any ads to preload
            if (parsedData.maxNativeId == null && parsedData.nativeAdIds.isEmpty()) {
                Log.e(TAG, "[$adPlace] No valid ad IDs found in: $adIdString")
                notifyPreloadCallbacks(adPlace, false)
                return
            }

            Log.d(TAG, "[$adPlace] Starting preload - MAX: ${parsedData.maxNativeId}, NATIVE count: ${parsedData.nativeAdIds.size}")

            // Store preload data
            val preloadData = PreloadedAdData(
                maxNativeId = parsedData.maxNativeId,
                nativeAdIds = parsedData.nativeAdIds,
                adPlace = adPlace,
                screenName = screenName,
                isPreloaded = false,
                isMaxNative = parsedData.maxNativeId != null,
                isLoading = true,
                hasFailed = false
            )
            preloadedAdsCache[adPlace] = preloadData

            // Start preloading
            com.ads.nomyek_admob.ads_components.YNMAds.getInstance().setInitCallback {
                // Check for MAX native first
                if (parsedData.maxNativeId != null && parsedData.maxNativeId.isNotEmpty()) {
                    Log.d(TAG, "[$adPlace] Preloading MAX Native ad: ${parsedData.maxNativeId}")
                    // Preload MAX native ad using MaxNativePreload
                    MaxNativePreload.getInstance().preloadNative(
                        context,
                        parsedData.maxNativeId,
                        adPlace,
                        layoutId,
                        object : AppLovinCallback() {
                            override fun onAdLoaded() {
                                super.onAdLoaded()
                                Log.d(TAG, "[$adPlace] ✅ MAX Native ad preloaded successfully")
                                preloadData.isPreloaded = true
                                preloadData.isMaxNative = true
                                preloadData.isLoading = false
                                preloadData.hasFailed = false
                                notifyPreloadCallbacks(adPlace, true)
                            }

                            override fun onAdFailedToLoad(error: MaxError?) {
                                super.onAdFailedToLoad(error)
                                Log.e(TAG, "[$adPlace] ❌ MAX Native ad failed: ${error?.message}")
                                // If MAX native fails and we have regular native IDs, try to preload them
                                if (parsedData.nativeAdIds.isNotEmpty()) {
                                    Log.d(TAG, "[$adPlace] Falling back to regular Native ads")
                                    preloadRegularNativeAds(context, parsedData, preloadData, adPlace, screenName)
                                } else {
                                    preloadData.isPreloaded = false
                                    preloadData.isLoading = false
                                    preloadData.hasFailed = true
                                    notifyPreloadCallbacks(adPlace, false)
                                }
                            }
                        }
                    )
                } else if (parsedData.nativeAdIds.isNotEmpty()) {
                    Log.d(TAG, "[$adPlace] Preloading regular Native ads (count: ${parsedData.nativeAdIds.size})")
                    // Preload regular native ads only
                    preloadRegularNativeAds(context, parsedData, preloadData, adPlace, screenName)
                }
            }
        }

        /**
         * Preload regular native ads
         */
        private fun preloadRegularNativeAds(
            context: Context,
            parsedData: ParsedAdData,
            preloadData: PreloadedAdData,
            adPlace: String,
            screenName: String
        ) {
            AdsNativeMultiPreload.preloadMultipleNativeAds(
                context,
                YNMAirBridge.AppData(screenName, adPlace),
                parsedData.nativeAdIds,
                adPlace,
                object : YNMAdsCallbacks() {
                    override fun onNativeAdLoaded(nativeAd: NativeAd) {
                        super.onNativeAdLoaded(nativeAd)
                        Log.d(TAG, "[$adPlace] ✅ Regular Native ad preloaded successfully")
                        preloadData.isPreloaded = true
                        preloadData.isMaxNative = false
                        preloadData.isLoading = false
                        preloadData.hasFailed = false
                        notifyPreloadCallbacks(adPlace, true)
                    }

                    override fun onAdFailedToLoad(adError: AdsError?) {
                        super.onAdFailedToLoad(adError)
                        Log.e(TAG, "[$adPlace] ❌ Regular Native ad failed: ${adError?.message}")
                        preloadData.isPreloaded = false
                        preloadData.isLoading = false
                        preloadData.hasFailed = true
                        notifyPreloadCallbacks(adPlace, false)
                    }
                }
            )
        }

        /**
         * Wait for preload to complete and get result
         * @param adPlace Ad placement identifier
         * @param callback Callback with result (true if loaded, false if failed)
         */
        @JvmStatic
        fun waitForPreload(adPlace: String, callback: (Boolean) -> Unit) {
            val preloadData = preloadedAdsCache[adPlace]

            if (preloadData == null) {
                Log.d(TAG, "[$adPlace] waitForPreload: No preload data found")
                callback(false)
                return
            }

            if (preloadData.isPreloaded) {
                Log.d(TAG, "[$adPlace] waitForPreload: Already preloaded, returning immediately")
                callback(true)
                return
            }

            if (preloadData.hasFailed) {
                Log.d(TAG, "[$adPlace] waitForPreload: Preload failed, returning immediately")
                callback(false)
                return
            }

            if (preloadData.isLoading) {
                Log.d(TAG, "[$adPlace] waitForPreload: Still loading, adding callback to wait list")
                val callbacks = preloadCallbacks.getOrPut(adPlace) { mutableListOf() }
                callbacks.add(callback)
            } else {
                Log.d(TAG, "[$adPlace] waitForPreload: Not loading and not loaded")
                callback(false)
            }
        }

        /**
         * Notify all waiting callbacks
         */
        private fun notifyPreloadCallbacks(adPlace: String, success: Boolean) {
            val callbacks = preloadCallbacks.remove(adPlace)
            if (callbacks != null && callbacks.isNotEmpty()) {
                Log.d(TAG, "[$adPlace] Notifying ${callbacks.size} waiting callbacks with result: $success")
                callbacks.forEach { callback ->
                    callback(success)
                }
            }
        }

        /**
         * Check if ads are preloaded for a specific placement
         * @param adPlace Ad placement identifier
         * @return True if ads are preloaded
         */
        @JvmStatic
        fun isPreloaded(adPlace: String): Boolean {
            return preloadedAdsCache[adPlace]?.isPreloaded == true
        }

        /**
         * Check if ads are currently loading for a specific placement
         * @param adPlace Ad placement identifier
         * @return True if ads are loading
         */
        @JvmStatic
        fun isLoading(adPlace: String): Boolean {
            return preloadedAdsCache[adPlace]?.isLoading == true
        }

        /**
         * Check if preload has failed for a specific placement
         * @param adPlace Ad placement identifier
         * @return True if preload failed
         */
        @JvmStatic
        fun hasFailed(adPlace: String): Boolean {
            return preloadedAdsCache[adPlace]?.hasFailed == true
        }

        /**
         * Clear preloaded ads for a specific placement
         * @param adPlace Ad placement identifier
         */
        @JvmStatic
        fun clearPreloadedAds(adPlace: String) {
            preloadedAdsCache.remove(adPlace)
        }

        /**
         * Clear all preloaded ads
         */
        @JvmStatic
        fun clearAllPreloadedAds() {
            preloadedAdsCache.clear()
        }
    }

    init {
        initializeLayout()
    }

    /**
     * Initialize the collapsible layout
     */
    private fun initializeLayout() {
        activity?.let { act ->
            rootLayout?.let { root ->
                // Inflate the collapsible layout
                val inflater = LayoutInflater.from(act)
                collapsibleLayout = inflater.inflate(R.layout.layout_native_collapsible, root, false)

                // Find views
                nativeAdView = collapsibleLayout?.findViewById(R.id.native_onboarding)
                btnCloseRight = collapsibleLayout?.findViewById(R.id.btn_close)
                btnCloseLeft = collapsibleLayout?.findViewById(R.id.btn_close)
                background = collapsibleLayout?.findViewById(R.id.background)

                // Set up close button click listeners
                btnCloseRight?.setOnClickListener { toggleCollapse() }
                btnCloseLeft?.setOnClickListener { toggleCollapse() }

                // Apply initial close button position
                updateCloseButtonVisibility()

                // Initially hide the layout until ad is loaded
                collapsibleLayout?.visibility = View.GONE

                // Set layout params to position at bottom
                val layoutParams = when (root) {
                    is FrameLayout -> {
                        FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            gravity = Gravity.BOTTOM
                        }
                    }
                    is androidx.constraintlayout.widget.ConstraintLayout -> {
                        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.MATCH_PARENT,
                            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            bottomToBottom = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                            startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                            endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                        }
                    }
                    is android.widget.RelativeLayout -> {
                        android.widget.RelativeLayout.LayoutParams(
                            android.widget.RelativeLayout.LayoutParams.MATCH_PARENT,
                            android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            addRule(android.widget.RelativeLayout.ALIGN_PARENT_BOTTOM)
                        }
                    }
                    else -> {
                        // Default layout params
                        ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    }
                }

                // Add to root layout with bottom positioning
                root.addView(collapsibleLayout, layoutParams)
            }
        }
    }

    /**
     * Show preloaded ad from cache
     * @param adPlace Ad placement identifier (cache key)
     */
    fun showPreloadedAd(adPlace: String) {
        Log.d(TAG, "[$adPlace] showPreloadedAd() called")

        // Check if premium or ads disabled
        if (IAPHelper.isPremium() || AdsHelper.isDisableAllAd()) {
            Log.d(TAG, "[$adPlace] Premium or ads disabled, not showing")
            collapsibleLayout?.visibility = View.GONE
            clearPreloadedAds(adPlace)
            onAdFailedCallback?.invoke()
            return
        }

        // Check if activity is valid
        val act = activity
        if (act == null || act.isFinishing || act.isDestroyed) {
            Log.e(TAG, "[$adPlace] Activity is null or finishing/destroyed")
            collapsibleLayout?.visibility = View.GONE
            clearPreloadedAds(adPlace)
            onAdFailedCallback?.invoke()
            return
        }

        // Check if interstitial is showing
        if (com.ads.nomyek_admob.admobs.AppOpenManager.getInstance().isInterstitialShowing) {
            Log.d(TAG, "[$adPlace] Interstitial is showing, not showing collapsible ad")
            collapsibleLayout?.visibility = View.GONE
            onAdFailedCallback?.invoke()
            return
        }

        // Get preloaded ad data
        val preloadData = preloadedAdsCache[adPlace]
        if (preloadData == null) {
            Log.e(TAG, "[$adPlace] No preload data found")
            collapsibleLayout?.visibility = View.GONE
            onAdFailedCallback?.invoke()
            return
        }

        if (!preloadData.isPreloaded) {
            Log.e(TAG, "[$adPlace] Ad not preloaded yet (isLoading: ${preloadData.isLoading}, hasFailed: ${preloadData.hasFailed})")
            collapsibleLayout?.visibility = View.GONE
            onAdFailedCallback?.invoke()
            return
        }

        Log.d(TAG, "[$adPlace] Showing preloaded ad (isMaxNative: ${preloadData.isMaxNative})")

        nativeAdView?.let { adView ->
            // Check if it's MAX native ad
            if (preloadData.isMaxNative && preloadData.maxNativeId != null) {
                Log.d(TAG, "[$adPlace] Showing MAX Native ad")
                val currentActivity = activity
                if (currentActivity == null || currentActivity.isFinishing || currentActivity.isDestroyed) {
                    Log.e(TAG, "[$adPlace] Activity invalid when showing MAX native")
                    clearPreloadedAds(adPlace)
                    onAdFailedCallback?.invoke()
                    return
                }

                isAdLoaded = true
                background?.visibility = View.GONE
                adView.visibility = View.VISIBLE

                // Show the preloaded MAX native ad
                MaxNativePreload.getInstance().showNative(
                    adPlace,
                    adView,
                    null
                )

                showWithAnimation()
                Log.d(TAG, "[$adPlace] ✅ MAX Native ad shown successfully")

                // Clear preloaded ad after showing
                clearPreloadedAds(adPlace)
                Log.d(TAG, "[$adPlace] Cleared preloaded ad from cache")

                onAdLoadedCallback?.invoke()
            } else {
                Log.d(TAG, "[$adPlace] Showing regular Native ad")
                // Show regular native ad
                showRegularNativeAd(act, preloadData, adView)

                // Clear preloaded ad after showing
                clearPreloadedAds(adPlace)
                Log.d(TAG, "[$adPlace] Cleared preloaded ad from cache")
            }
        }
    }

    /**
     * Show regular native ad from preloaded data
     */
    private fun showRegularNativeAd(
        act: Activity,
        preloadData: PreloadedAdData,
        adView: YNMNativeAdView
    ) {
        val currentActivity = activity
        if (currentActivity == null || currentActivity.isFinishing || currentActivity.isDestroyed) {
            Log.e(TAG, "[${preloadData.adPlace}] Activity invalid when showing regular native")
            collapsibleLayout?.visibility = View.GONE
            onAdFailedCallback?.invoke()
            return
        }

        isAdLoaded = true
        background?.visibility = View.GONE
        adView.visibility = View.VISIBLE

        // Show the preloaded native ad using AdsNativeMultiPreload.showPreloadedNativeAd()
        AdsNativeMultiPreload.showPreloadedNativeAd(
            currentActivity,
            adView,
            preloadData.adPlace,
            R.layout.custom_native_admob_collap,
            R.layout.custom_native_admob_collap
        )

        showWithAnimation()
        Log.d(TAG, "[${preloadData.adPlace}] ✅ Regular Native ad shown successfully")
        onAdLoadedCallback?.invoke()
    }

    /**
     * Load native ad with ad ID string format "MAX_NATIVE:id,NATIVE:id,NATIVE:id"
     * @param adIdString String format of ad IDs
     * @param adPlace Ad placement identifier
     * @param screenName Screen name for tracking
     */
    /**
     * Alias for loadNativeAd - Load native ad with config string
     * @param adConfigString String format of ad IDs "MAX_NATIVE:id,NATIVE:id"
     * @param adPlace Ad placement identifier
     * @param screenName Screen name for tracking
     */
    fun loadNativeAdWithConfig(
        adConfigString: String,
        adPlace: String,
        screenName: String
    ) {
        loadNativeAd(adConfigString, adPlace, screenName)
    }

    fun loadNativeAd(
        adIdString: String,
        adPlace: String,
        screenName: String
    ) {
        val parsedData = parseAdIdString(adIdString)

        // Check if premium or ads disabled
        if (IAPHelper.isPremium() || AdsHelper.isDisableAllAd()) {
            collapsibleLayout?.visibility = View.GONE
            onAdFailedCallback?.invoke()
            return
        }

        // Check if activity is valid
        val act = activity
        if (act == null || act.isFinishing || act.isDestroyed) {
            collapsibleLayout?.visibility = View.GONE
            onAdFailedCallback?.invoke()
            return
        }

        // Check if interstitial is showing
        if (com.ads.nomyek_admob.admobs.AppOpenManager.getInstance().isInterstitialShowing) {
            collapsibleLayout?.visibility = View.GONE
            onAdFailedCallback?.invoke()
            return
        }

        nativeAdView?.let { adView ->
            // Check if MAX native ad exists
            if (parsedData.maxNativeId != null && parsedData.maxNativeId.isNotEmpty()) {
                // Load and show MAX native ad using MaxNativePreload
                MaxNativePreload.getInstance().preloadNative(
                    act,
                    parsedData.maxNativeId,
                    adPlace,
                    R.layout.custom_native_admob_collap_max,
                    object : AppLovinCallback() {
                        override fun onAdLoaded() {
                            super.onAdLoaded()
                            val currentActivity = activity
                            if (currentActivity == null || currentActivity.isFinishing || currentActivity.isDestroyed) {
                                collapsibleLayout?.visibility = View.GONE
                                onAdFailedCallback?.invoke()
                                return
                            }

                            isAdLoaded = true
                            background?.visibility = View.GONE
                            adView.visibility = View.VISIBLE

                            // Show MAX native ad
                            MaxNativePreload.getInstance().showNative(
                                adPlace,
                                adView,
                                null
                            )

                            showWithAnimation()
                            onAdLoadedCallback?.invoke()
                        }

                        override fun onAdFailedToLoad(error: MaxError?) {
                            super.onAdFailedToLoad(error)
                            // Fallback to regular native ads if available
                            if (parsedData.nativeAdIds.isNotEmpty()) {
                                loadRegularNativeAds(act, parsedData.nativeAdIds, adPlace, screenName, adView)
                            } else {
                                collapsibleLayout?.visibility = View.GONE
                                onAdFailedCallback?.invoke()
                            }
                        }
                    }
                )
            } else if (parsedData.nativeAdIds.isNotEmpty()) {
                // Load regular native ads
                loadRegularNativeAds(act, parsedData.nativeAdIds, adPlace, screenName, adView)
            } else {
                collapsibleLayout?.visibility = View.GONE
                onAdFailedCallback?.invoke()
            }
        }
    }

    /**
     * Load native ad with list of ad IDs (for backward compatibility)
     * @param listAdId List of ad ID models (NATIVE only, not MAX_NATIVE)
     * @param adPlace Ad placement identifier
     * @param screenName Screen name for tracking
     */
    fun loadNativeAdWithList(
        listAdId: List<AdsNativeMultiPreload.AdIdModel>,
        adPlace: String,
        screenName: String
    ) {
        // Check if premium or ads disabled
        if (IAPHelper.isPremium() || AdsHelper.isDisableAllAd()) {
            collapsibleLayout?.visibility = View.GONE
            onAdFailedCallback?.invoke()
            return
        }

        // Check if activity is valid
        val act = activity
        if (act == null || act.isFinishing || act.isDestroyed) {
            collapsibleLayout?.visibility = View.GONE
            onAdFailedCallback?.invoke()
            return
        }

        // Check if interstitial is showing
        if (com.ads.nomyek_admob.admobs.AppOpenManager.getInstance().isInterstitialShowing) {
            collapsibleLayout?.visibility = View.GONE
            onAdFailedCallback?.invoke()
            return
        }

        nativeAdView?.let { adView ->
            // Load regular native ads only (this method doesn't support MAX_NATIVE)
            loadRegularNativeAds(act, listAdId, adPlace, screenName, adView)
        }
    }

    /**
     * Load regular native ads using AdsNativeMultiPreload
     */
    private fun loadRegularNativeAds(
        act: Activity,
        listAdId: List<AdsNativeMultiPreload.AdIdModel>,
        adPlace: String,
        screenName: String,
        adView: YNMNativeAdView
    ) {
        com.ads.nomyek_admob.ads_components.YNMAds.getInstance().setInitCallback {
            // Preload multiple native ads
            AdsNativeMultiPreload.preloadMultipleNativeAds(
                act,
                YNMAirBridge.AppData(screenName, adPlace),
                listAdId,
                adPlace,
                object : YNMAdsCallbacks() {
                    override fun onNativeAdLoaded(nativeAd: NativeAd) {
                        super.onNativeAdLoaded(nativeAd)

                        val currentActivity = activity
                        if (currentActivity == null || currentActivity.isFinishing || currentActivity.isDestroyed) {
                            collapsibleLayout?.visibility = View.GONE
                            onAdFailedCallback?.invoke()
                            return
                        }

                        isAdLoaded = true
                        background?.visibility = View.GONE
                        adView.visibility = View.VISIBLE

                        // Show the preloaded native ad
                        AdsNativeMultiPreload.showPreloadedNativeAd(
                            currentActivity,
                            adView,
                            adPlace,
                            R.layout.custom_native_admob_collap,
                            R.layout.custom_native_admob_collap
                        )

                        showWithAnimation()
                        onAdLoadedCallback?.invoke()
                    }

                    override fun onAdFailedToLoad(adError: AdsError?) {
                        super.onAdFailedToLoad(adError)
                        collapsibleLayout?.visibility = View.GONE
                        onAdFailedCallback?.invoke()
                    }
                }
            )
        }
    }

    /**
     * Show the layout without animation
     */
    private fun showWithAnimation() {
        if (collapsibleLayout?.visibility == View.VISIBLE) {
            return
        }

        collapsibleLayout?.let { layout ->
            layout.visibility = View.VISIBLE
            layout.clearAnimation() // Clear any existing animation
            isExpanded = true
        }
    }

    /**
     * Toggle collapse/expand state
     */
    fun toggleCollapse() {
        if (isExpanded) {
            collapse()
        } else {
            expand()
        }
    }

    /**
     * Collapse the ad layout
     */
    fun collapse() {
        if (!isExpanded || !isAdLoaded) {
            return
        }

        collapsibleLayout?.let { layout ->
            layout.clearAnimation() // Clear any existing animation
            layout.visibility = View.GONE
            isExpanded = false

            // Trigger callback when collapsed
            onCollapseCallback?.invoke()
        }
    }

    /**
     * Expand the ad layout
     */
    fun expand() {
        if (isExpanded || !isAdLoaded) {
            return
        }

        collapsibleLayout?.let { layout ->
            layout.clearAnimation() // Clear any existing animation
            layout.visibility = View.VISIBLE
            isExpanded = true
        }
    }

    /**
     * Hide the ad layout immediately without animation
     */
    fun hide() {
        collapsibleLayout?.let { layout ->
            layout.clearAnimation() // Clear any existing animation
            layout.visibility = View.GONE
        }
        isExpanded = false
    }

    /**
     * Show the ad layout immediately without animation
     */
    fun show() {
        if (isAdLoaded) {
            collapsibleLayout?.let { layout ->
                layout.clearAnimation() // Clear any existing animation
                layout.visibility = View.VISIBLE
            }
            isExpanded = true
        }
    }

    /**
     * Check if the ad is currently expanded
     */
    fun isExpanded(): Boolean = isExpanded

    /**
     * Check if ad is loaded
     */
    fun isAdLoaded(): Boolean = isAdLoaded

    /**
     * Set callback for when ad is loaded
     */
    fun setOnAdLoadedCallback(callback: (() -> Unit)?) {
        onAdLoadedCallback = callback
    }

    /**
     * Set callback for when ad fails to load
     */
    fun setOnAdFailedCallback(callback: (() -> Unit)?) {
        onAdFailedCallback = callback
    }

    /**
     * Set callback for when ad is collapsed by user
     */
    fun setOnCollapseCallback(callback: () -> Unit) {
        onCollapseCallback = callback
    }

    /**
     * Set close button position (LEFT or RIGHT)
     * @param position CloseButtonPosition.LEFT or CloseButtonPosition.RIGHT
     */
    fun setCloseButtonPosition(position: CloseButtonPosition) {
        closeButtonPosition = position
        updateCloseButtonVisibility()
    }

    /**
     * Get current close button position
     */
    fun getCloseButtonPosition(): CloseButtonPosition = closeButtonPosition

    /**
     * Update close button visibility based on position setting
     */
    private fun updateCloseButtonVisibility() {
        when (closeButtonPosition) {
            CloseButtonPosition.LEFT -> {
                btnCloseLeft?.visibility = View.VISIBLE
                btnCloseRight?.visibility = View.GONE
            }
            CloseButtonPosition.RIGHT -> {
                btnCloseLeft?.visibility = View.GONE
                btnCloseRight?.visibility = View.VISIBLE
            }
        }
    }

    /**
     * Clean up resources
     */
    fun destroy() {
        collapsibleLayout?.let { layout ->
            rootLayout?.removeView(layout)
        }
        activity = null
        rootLayout = null
        collapsibleLayout = null
        nativeAdView = null
        btnCloseRight = null
        btnCloseLeft = null
        onAdLoadedCallback = null
        onAdFailedCallback = null
        onCollapseCallback = null
    }
}

