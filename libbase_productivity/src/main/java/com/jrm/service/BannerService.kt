package com.jrm.service

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.ads.nomyek_admob.admobs.AppOpenManager
import com.ads.nomyek_admob.ads_components.YNMAds
import com.ads.nomyek_admob.ads_components.YNMAdsCallbacks
import com.ads.nomyek_admob.ads_components.ads_banner.YNMBannerAdView
import com.ads.nomyek_admob.ads_components.wrappers.AdsError
import com.ads.nomyek_admob.event.YNMAirBridge
import com.ads.nomyek_admob.event.YNMLogEventManager
import com.ads.nomyek_admob.utils.TypeAds
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnPaidEventListener
import com.jrm.BuildConfig
import com.jrm.model.UnitIdConfig
import com.jrm.service.BannerService.showPreloaded
import com.jrm.utils.Logger
import com.jrm.utils.AdsHelper
import com.jrm.utils.findViewByName
import com.jrm.utils.remote_config.RemoteConfigManager
import java.util.concurrent.ConcurrentHashMap

/**
 * Banner ads managed by [placeName] (ad_config placement ID).
 * Preload, show, load-and-show, and auto-refresh are all keyed by placeName.
 */
object BannerService {
    private const val TAG = "WaterfallBannerMgr"

    private fun getUnitId(config: UnitIdConfig): String {
        return if (BuildConfig.DEBUG && config.unitIdTest.isNotEmpty()) config.unitIdTest else config.unitId
    }

    private val config get() = RemoteConfigManager.instance?.adConfig
    private val loadingState = ConcurrentHashMap<String, Boolean>()
    private val preloadedBanners = ConcurrentHashMap<String, YNMBannerAdView>()

    private val handler = Handler(Looper.getMainLooper())

    /**
     * Preload a banner for [placeName] (config from ad_config). Cached by placeName.
     * Call [showPreloaded] later to show into a container.
     */
    fun preload(
        activity: Activity,
        placeName: String,
        screenName: String,
        callback: ((Boolean) -> Unit)? = null
    ) {
        if (AdsHelper.isDisableAllAd()) {
            callback?.invoke(false)
            return
        }
        val configs = Helper.buildUnitIdConfigList(config, placeName)
        val adIds = configs.map { getUnitId(it) }
        if (adIds.isEmpty()) {
            Logger.w("No ad config for placeName=$placeName")
            callback?.invoke(false)
            return
        }
        if (preloadedBanners.containsKey(placeName)) {
            Logger.d("[$placeName] Banner already preloaded")
            callback?.invoke(true)
            return
        }
        if (loadingState[placeName] == true) {
            Logger.d("[$placeName] Banner is already loading")
            callback?.invoke(false)
            return
        }
        loadingState[placeName] = true
        val adView = YNMBannerAdView(activity)
        loadBanner(activity, adView, adIds, screenName, placeName) { success ->
            loadingState.remove(placeName)
            if (success) {
                preloadedBanners[placeName] = adView
                Logger.d("[$placeName] Banner preload success")
            } else {
                Logger.d("[$placeName] Banner preload failed")
            }
            callback?.invoke(success)
        }
    }

    /**
     * Show a preloaded banner for [placeName] into [container].
     * @return true if a preloaded banner was found and shown.
     */
    fun showPreloaded(placeName: String, container: ViewGroup): Boolean {
        val adView = preloadedBanners[placeName] ?: return false
        (adView.parent as? ViewGroup)?.removeView(adView)
        container.removeAllViews()
        container.addView(adView)
        preloadedBanners.remove(placeName)
        return true
    }

    /**
     * Load and show a Standard Banner Ad for [placementName] (config from ad_config).
     * If [container] is null, only preloads (cache by placementName); use [showPreloaded] to show later.
     * If [container] is non-null, loads ad into that [YNMBannerAdView].
     */
    /**
     * Load and show banner. Use [lifecycleOwner] when banner is inside a Fragment so reload
     * pauses when fragment is hidden and resumes when fragment is visible again.
     */
    fun loadAndShowBanner(
        activity: Activity,
        activityName: String,
        placementName: String,
        callback: ((Boolean) -> Unit)? = null,
        lifecycleOwner: LifecycleOwner? = null
    ) {
        val container = if (activity.findViewByName<View>("bannerView") != null)
            activity.findViewByName<YNMBannerAdView>("bannerView")
        else null

        if (container == null) {
            preload(activity, placementName, activityName, callback)
            return
        }
        val configs = Helper.buildUnitIdConfigList(config, placementName)
        val adIds = configs.map { getUnitId(it) }
        Logger.d("loadAndShowBanner(placementName=$placementName) with ${adIds.size} IDs")
        if (adIds.isEmpty()) {
            Logger.w("No ad config for placementName=$placementName")
            container.visibility = View.GONE
            callback?.invoke(false)
            return
        }
        val timeReloadSeconds =
            config?.adPlacements?.get(placementName)?.nativeConfig?.reloadTime ?: 0L
        val intervalMs = timeReloadSeconds * 1000L
        val ownerToObserve = lifecycleOwner ?: (activity as LifecycleOwner)
        if (timeReloadSeconds > 0L) {
            val reloadKey = AdReloadScheduler.getReloadKey(placementName, ownerToObserve)
            val remainingMs = AdReloadScheduler.getRemainingReloadDelayMs(reloadKey, intervalMs)
            if (remainingMs > 0L) {
                Logger.d("[$placementName] Chưa hết interval, không request ad (đợi thêm ${remainingMs}ms)")
                scheduleReloadIfNeeded(
                    activity, placementName, activityName, timeReloadSeconds,
                    initialDelayMs = remainingMs,
                    onResult = callback,
                    lifecycleOwner = lifecycleOwner
                )
                callback?.invoke(true)
                return
            }
        }
        container.visibility = View.VISIBLE
        loadBanner(activity, container, adIds, activityName, placementName) { success ->
            if (activity.isDestroyed || activity.isFinishing) return@loadBanner
            if (!success) {
                container.visibility = View.GONE
            } else {
                Logger.d("Banner shown for placementName=$placementName")
                if (timeReloadSeconds > 0L) {
                    val owner = lifecycleOwner ?: (activity as LifecycleOwner)
                    if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
                        val reloadKey = AdReloadScheduler.getReloadKey(placementName, owner)
                        AdReloadScheduler.markReloadDone(reloadKey)
                        scheduleReloadIfNeeded(
                            activity, placementName, activityName, timeReloadSeconds, callback,
                            lifecycleOwner = lifecycleOwner
                        )
                    }
                }
            }
            callback?.invoke(success)
        }
    }

    /**
     * Schedules reload when [timeReloadSeconds] > 0 via [AdReloadScheduler].
     */
    private fun scheduleReloadIfNeeded(
        activity: Activity,
        placeName: String,
        activityName: String,
        timeReloadSeconds: Long,
        onResult: ((Boolean) -> Unit)?,
        initialDelayMs: Long? = null,
        lifecycleOwner: LifecycleOwner? = null
    ) {
        if (timeReloadSeconds <= 0) return
        val intervalMs = timeReloadSeconds * 1000L
        val ownerToObserve: LifecycleOwner = lifecycleOwner ?: (activity as LifecycleOwner)
        AdReloadScheduler.schedule(
            placeName = placeName,
            owner = ownerToObserve,
            activity = activity,
            intervalMs = intervalMs,
            initialDelayMs = initialDelayMs,
            placeLabel = placeName,
            onReload = { act, owner ->
                if (AppOpenManager.getInstance().isInterstitialShowing) return@schedule false
                loadAndShowBanner(act, activityName, placeName, onResult, lifecycleOwner = owner)
                true
            }
        )
    }

    /**
     * Stops the reload timer for [placeName]. Cancels all reload state for this placement
     * (all Activities/Fragments using this placeName).
     */
    fun cancelReload(placeName: String) {
        AdReloadScheduler.cancelReload(placeName)
    }

    /**
     * Load banner ads sequentially until one succeeds (waterfall).
     */
    private fun loadBanner(
        activity: Activity,
        adView: YNMBannerAdView,
        adIds: List<String>,
        screenName: String,
        placementName: String,
        callback: ((Boolean) -> Unit)?
    ) {
        if (AdsHelper.isDisableAllAd()) {
            callback?.invoke(false)
            return
        }
        if (adIds.isEmpty()) {
            callback?.invoke(false)
            return
        }
        loadRecursive(activity, adView, adIds, 0, screenName, placementName, callback)
    }

    /**
     * Load and show an Inline Adaptive Banner for [placeName] (config from ad_config).
     */
    fun loadAndShowInlineAdaptiveBanner(
        activity: Activity,
        container: ViewGroup,
        placeName: String,
        screenName: String,
        fixedHeightDp: Int = 150,
        callback: ((Boolean) -> Unit)? = null
    ) {
        val configs = Helper.buildUnitIdConfigList(config, placeName)
        val adIds = configs.map { getUnitId(it) }
        loadAndShowInlineAdaptiveBanner(activity, container, adIds, fixedHeightDp, callback)
    }

    /**
     * Load and show an Inline Adaptive Banner with explicit [adConfigs].
     */
    @JvmName("loadAndShowInlineAdaptiveBannerConfigs")
    fun loadAndShowInlineAdaptiveBanner(
        activity: Activity,
        container: ViewGroup,
        adConfigs: List<UnitIdConfig>,
        fixedHeightDp: Int = 150,
        callback: ((Boolean) -> Unit)? = null
    ) {
        val adIds = adConfigs.map { getUnitId(it) }
        loadAndShowInlineAdaptiveBanner(activity, container, adIds, fixedHeightDp, callback)
    }

    /**
     * Load and show an Inline Adaptive Banner with explicit [adIds].
     */
    fun loadAndShowInlineAdaptiveBanner(
        activity: Activity,
        container: ViewGroup,
        adIds: List<String>,
        fixedHeightDp: Int = 150,
        callback: ((Boolean) -> Unit)? = null
    ) {
        Logger.d("loadAndShowInlineAdaptiveBanner() called with IDs: $adIds")
        if (AdsHelper.isDisableAllAd()) {
            Logger.d("Ads disabled")
            container.visibility = View.GONE
            callback?.invoke(false)
            return
        }

        if (adIds.isEmpty()) {
            Logger.w("No ad IDs provided")
            container.visibility = View.GONE
            callback?.invoke(false)
            return
        }

        loadInlineRecursive(
            activity, container, adIds, 0, fixedHeightDp, callback
        )
    }

    /**
     * Check if a banner is preloaded for [placeName].
     */
    fun isPreloaded(placeName: String): Boolean {
        return preloadedBanners.containsKey(placeName)
    }

    /**
     * Destroy a preloaded banner for [placeName].
     */
    fun destroy(placeName: String) {
        preloadedBanners.remove(placeName)
    }


    private fun loadRecursive(
        activity: Activity,
        adView: YNMBannerAdView,
        adIds: List<String>,
        index: Int,
        screenName: String,
        placementName: String,
        callback: ((Boolean) -> Unit)?
    ) {
        // Base case: All ads failed
        if (index >= adIds.size) {
            callback?.invoke(false)
            return
        }

        val currentId = adIds[index]
        Logger.d("loadRecursive: Attempting $currentId (Index: $index/${adIds.size - 1})")
        adView.loadBanner(
            activity, currentId, object : YNMAdsCallbacks(
                YNMAirBridge.AppData(screenName, placementName), YNMAds.BANNER
            ) {
                override fun onAdLoaded() {
                    super.onAdLoaded()
                    Logger.d("✅ Banner loaded successfully: $currentId")
                    callback?.invoke(true)
                }

                override fun onAdFailedToLoad(adError: AdsError?) {
                    super.onAdFailedToLoad(adError)
                    Logger.w(
                        "❌ Banner failed: $currentId, error: ${adError?.message}. trying next..."
                    )

                    // Recursive step
                    loadRecursive(
                        activity, adView, adIds, index + 1, screenName, placementName, callback
                    )
                }
            })
    }
}

private fun loadInlineRecursive(
    activity: Activity,
    container: ViewGroup,
    adIds: List<String>,
    index: Int,
    fixedHeightDp: Int,
    callback: ((Boolean) -> Unit)?
) {
    if (index >= adIds.size) {
        Logger.d("All inline ads failed")
        container.visibility = View.GONE
        callback?.invoke(false)
        return
    }
    val currentId = adIds[index]
    Logger.d("Attempting load inline banner: $currentId")

    try {
        val adSize = getInlineAdaptiveBannerAdSize(activity, fixedHeightDp)
        val adView = AdView(activity).apply {
            adUnitId = currentId
            setAdSize(adSize)
        }

        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                super.onAdLoaded()
                if (activity.isDestroyed || activity.isFinishing) {
                    try {
                        adView.destroy()
                    } catch (e: Exception) {
                    }
                    return
                }
                container.removeAllViews()
                container.addView(adView)
                container.visibility = View.VISIBLE
                adView.onPaidEventListener = OnPaidEventListener { adValue: AdValue? ->
                    YNMLogEventManager.logPaidAdImpression(
                        activity, adValue, adView.adUnitId, adView.responseInfo, TypeAds.BANNER
                    )
                }
                callback?.invoke(true)
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                super.onAdFailedToLoad(error)
                try {
                    adView.destroy()
                } catch (e: Exception) {
                }
                if (activity.isDestroyed || activity.isFinishing) return
                loadInlineRecursive(
                    activity,
                    container,
                    adIds,
                    index + 1,
                    fixedHeightDp,
                    callback
                )
            }
        }

        adView.loadAd(AdRequest.Builder().build())
    } catch (e: Exception) {
        Logger.e("Error loading inline banner", e)
        loadInlineRecursive(activity, container, adIds, index + 1, fixedHeightDp, callback)
    }
}


private fun getInlineAdaptiveBannerAdSize(activity: Activity, fixedHeightDp: Int): AdSize {
    val display = activity.windowManager.defaultDisplay
    val outMetrics = DisplayMetrics()
    display.getMetrics(outMetrics)
    val density = outMetrics.density
    val adWidth = (outMetrics.widthPixels / density).toInt()
    return AdSize.getInlineAdaptiveBannerAdSize(adWidth, fixedHeightDp)
}


