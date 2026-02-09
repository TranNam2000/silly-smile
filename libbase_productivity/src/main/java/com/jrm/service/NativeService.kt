package com.jrm.service

import android.app.Activity
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.jrm.utils.Logger
import com.ads.nomyek_admob.admobs.AppOpenManager
import com.ads.nomyek_admob.ads_components.YNMAdsCallbacks
import com.ads.nomyek_admob.ads_components.ads_native.YNMNativeAdView
import com.ads.nomyek_admob.ads_components.wrappers.AdsError
import com.ads.nomyek_admob.event.YNMAirBridge
import com.ads.nomyek_admob.utils.AdsNativeMultiPreload
import com.google.android.gms.ads.nativead.NativeAd
import com.jrm.R
import com.jrm.service.Helper.buildUnitIdConfigList
import com.jrm.service.Helper.convertToAdIdModelList
import com.jrm.service.NativeService.loadNativeAd
import com.jrm.model.AdConfigModel
import com.jrm.utils.remote_config.RemoteConfigManager

object NativeService {
    const val TAG = "NativeService"
    private val config = RemoteConfigManager.instance?.adConfig

    /**
     * Load (and optionally show) native ad for the given placement.
     *
     * When [timeReload] > 0 and [adView] is not null, a reload timer is started and automatically
     * cancelled when the [activity] is destroyed (no need to call [cancelReload] manually).
     *
     * @param adView If non-null, ad will be shown into this view when loaded; if null, only preload.
     * @param timeReload If > 0 (seconds), schedule reload after ad is shown. Use 0 to disable.
     * @param waitForLoad Only when [adView] != null: if true, wait for ad to load then show; if false and ad not loaded yet, hide view and [onFailure] immediately (no wait).
     */
    /**
     * @param lifecycleOwner When native ad is inside a Fragment, pass the Fragment so reload
     *   pauses when fragment is hidden and resumes when fragment is visible again.
     */
    fun loadNativeAd(
        activity: Activity,
        placeName: String,
        activityName: String,
        adView: YNMNativeAdView?,
        waitForLoad: Boolean = true,
        onSuccess: (() -> Unit)? = null,
        onFailure: (() -> Unit)? = null,
        lifecycleOwner: LifecycleOwner? = null
    ) {
        val listAdId = convertToAdIdModelList(buildUnitIdConfigList(config, placeName))
        if (listAdId.isEmpty()) {
            onFailure?.invoke()
            adView?.visibility = View.GONE
            return
        }

        if (adView == null) {
            preloadOnly(activity, placeName, activityName, listAdId, onSuccess, onFailure)
            return
        }

        val (layoutAdmob, layoutMax) = getLayoutResources(config, placeName)
        val timeReload = config?.adPlacements?.get(placeName)?.nativeConfig?.reloadTime ?: 0L
        val intervalMs = timeReload * 1000L
        val ownerToObserve = lifecycleOwner ?: (activity as LifecycleOwner)
        if (timeReload > 0L) {
            val reloadKey = AdReloadScheduler.getReloadKey(placeName, ownerToObserve)
            val remainingMs = AdReloadScheduler.getRemainingReloadDelayMs(reloadKey, intervalMs)
            if (remainingMs > 0L) {
                Logger.d( "[$placeName] Chưa hết interval, không request ad (đợi thêm ${remainingMs}ms)")
                scheduleReloadIfNeeded(
                    activity, placeName, activityName, adView, timeReload, waitForLoad,
                    onSuccess, onFailure, lifecycleOwner = lifecycleOwner, initialDelayMs = remainingMs
                )
                onSuccess?.invoke()
                return
            }
        }
        if (AdsNativeMultiPreload.isAdLoaded(placeName)) {
            showAdInView(
                activity,
                activityName,
                adView,
                placeName,
                layoutAdmob,
                layoutMax,
                timeReload,
                waitForLoad,
                onSuccess,
                onFailure,
                lifecycleOwner
            )
            return
        }
        preloadAndShow(
            activity,
            placeName,
            activityName,
            listAdId,
            adView,
            layoutAdmob,
            layoutMax,
            timeReload,
            waitForLoad,
            onSuccess,
            onFailure,
            lifecycleOwner
        )
    }

    private fun showAdInView(
        activity: Activity,
        activityName: String,
        adView: YNMNativeAdView,
        placeName: String,
        layoutAdmob: Int,
        layoutMax: Int,
        timeReload: Long,
        waitForLoad: Boolean,
        onSuccess: (() -> Unit)?,
        onFailure: (() -> Unit)?,
        lifecycleOwner: LifecycleOwner? = null
    ) {
        val activityLabel = "${activity.javaClass.simpleName}($activityName)"
        Logger.d( "[$placeName] Show native ad tại Activity: $activityLabel")
        AdsNativeMultiPreload.showPreloadedNativeAd(
            activity,
            adView,
            placeName,
            layoutAdmob,
            layoutMax
        )
        adView.visibility = View.VISIBLE
        onSuccess?.invoke()
        if (timeReload > 0L) {
            val owner = lifecycleOwner ?: (activity as LifecycleOwner)
            if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
                val reloadKey = AdReloadScheduler.getReloadKey(placeName, owner)
                AdReloadScheduler.markReloadDone(reloadKey)
                scheduleReloadIfNeeded(
                    activity,
                    placeName,
                    activityName,
                    adView,
                    timeReload,
                    waitForLoad,
                    onSuccess,
                    onFailure,
                    lifecycleOwner = lifecycleOwner
                )
            }
        }
    }

    private fun preloadOnly(
        activity: Activity,
        placeName: String,
        activityName: String,
        listAdId: List<AdsNativeMultiPreload.AdIdModel>,
        onSuccess: (() -> Unit)?,
        onFailure: (() -> Unit)?
    ) {
        AdsNativeMultiPreload.preloadMultipleNativeAds(
            activity,
            YNMAirBridge.AppData(activityName, placeName),
            listAdId,
            placeName = placeName,
            object : YNMAdsCallbacks() {
                override fun onNativeAdLoaded(nativeAd: NativeAd) {
                    super.onNativeAdLoaded(nativeAd)
                    onSuccess?.invoke()
                }

                override fun onAdFailedToLoad(adError: AdsError?) {
                    super.onAdFailedToLoad(adError)
                    onFailure?.invoke()
                }
            }
        )
    }

    private fun preloadAndShow(
        activity: Activity,
        placeName: String,
        activityName: String,
        listAdId: List<AdsNativeMultiPreload.AdIdModel>,
        adView: YNMNativeAdView,
        layoutAdmob: Int,
        layoutMax: Int,
        timeReload: Long,
        waitForLoad: Boolean,
        onSuccess: (() -> Unit)?,
        onFailure: (() -> Unit)?,
        lifecycleOwner: LifecycleOwner? = null
    ) {
        if (waitForLoad){
            adView.visibility = View.VISIBLE
        }
        AdsNativeMultiPreload.preloadMultipleNativeAds(
            activity,
            YNMAirBridge.AppData(activityName, placeName),
            listAdId,
            placeName = placeName,
            object : YNMAdsCallbacks() {
                override fun onNativeAdLoaded(nativeAd: NativeAd) {
                    super.onNativeAdLoaded(nativeAd)
                    showAdInView(
                        activity,
                        activityName,
                        adView,
                        placeName,
                        layoutAdmob,
                        layoutMax,
                        timeReload,
                        waitForLoad,
                        onSuccess,
                        onFailure,
                        lifecycleOwner
                    )
                }

                override fun onAdFailedToLoad(adError: AdsError?) {
                    super.onAdFailedToLoad(adError)
                    adView.visibility = View.GONE
                    onFailure?.invoke()
                }
            }
        )
    }

    /**
     * Schedules reload when [timeReload] > 0 via [AdReloadScheduler].
     */
    private fun scheduleReloadIfNeeded(
        activity: Activity,
        placeName: String,
        activityName: String,
        adView: YNMNativeAdView,
        timeReload: Long,
        waitForLoad: Boolean,
        onSuccess: (() -> Unit)?,
        onFailure: (() -> Unit)?,
        lifecycleOwner: LifecycleOwner? = null,
        initialDelayMs: Long? = null
    ) {
        if (timeReload <= 0) return
        val intervalMs = timeReload * 1000L
        val ownerToObserve: LifecycleOwner = lifecycleOwner ?: (activity as LifecycleOwner)
        AdReloadScheduler.schedule(
            placeName = placeName,
            owner = ownerToObserve,
            activity = activity,
            intervalMs = intervalMs,
            initialDelayMs = initialDelayMs,
            placeLabel = placeName,
            onReload = { act, owner ->
                if (FullScreenService.isFullScreenAdShowing) return@schedule false
                Logger.d("[$placeName] Reload native ad tại Activity: ${act.javaClass.simpleName}($activityName)")
                AdsNativeMultiPreload.destroyPreloadedAd(placeName)
                loadNativeAd(act, placeName, activityName, adView, waitForLoad, onSuccess, onFailure, owner)
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
     * Get layout resource IDs for a placement
     */
    fun getLayoutResources(
        config: AdConfigModel?,
        placementId: String
    ): Pair<Int, Int> {
        val placement = config?.adPlacements?.get(placementId)

        val layoutAdmob = placement?.nativeConfig?.layoutRes?.let {
            getLayoutResourceId(it)
        } ?: R.layout.custom_native_admob_large

        val layoutMax = placement?.nativeConfig?.metaLayoutRes?.let {
            getLayoutResourceId(it)
        } ?: R.layout.custom_native_admob_large_max

        return Pair(layoutAdmob, layoutMax)
    }

    /**
     * Convert layout resource name to resource ID
     */
    private fun getLayoutResourceId(layoutName: String): Int {
        return when (layoutName) {
            "native_admob_large" -> R.layout.custom_native_admob_large
            "native_admob_large_2" -> R.layout.custom_native_admob_large_language
            "native_admob_medium" -> R.layout.custom_native_admob_large
            "native_admob_medium_2" -> R.layout.custom_native_admob_large
            "native_admob_banner" -> R.layout.custom_native_admob_large
            "native_admob_banner_large" -> R.layout.custom_native_admob_large
            "custom_native_fsn_layout" -> R.layout.custom_native_admob_large
            else -> {
                Logger.w("Unknown layout: $layoutName, using default")
                R.layout.custom_native_admob_large
            }
        }
    }
}