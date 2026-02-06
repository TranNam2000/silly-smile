package com.jrm.service

import android.app.Activity
import android.view.View
import androidx.lifecycle.LifecycleOwner
import com.ads.nomyek_admob.ads_components.YNMAds
import com.ads.nomyek_admob.ads_components.ads_native.YNMNativeAdView
import com.jrm.utils.AdsHelper
import com.jrm.utils.Logger
import com.jrm.utils.remote_config.RemoteConfigManager

/**
 * WaterfallNativeAdHelper - Utility class to simplify WaterfallNativeAdManager usage
 *
 * Features:
 * - Build config strings from AdConfigModel
 * - Batch preload multiple placements
 * - Simplified preload & show APIs
 * - Lifecycle-aware helpers
 * - Debug utilities
 *
 * Usage:
 * ```
 * // Simple preload & show
 * WaterfallNativeAdHelper.preloadAndShow(
 *     activity = this,
 *     adView = binding.nativeAdView,
 *     placementId = "splash_ad_view",
 *     onSuccess = { /* ad shown */ },
 *     onFailure = { /* no ad */ }
 * )
 *
 * // Batch preload
 * WaterfallNativeAdHelper.preloadBatch(
 *     context = this,
 *     placements = listOf("onboarding_1", "onboarding_2", "onboarding_3")
 * )
 *
 * // Cleanup on destroy
 * WaterfallNativeAdHelper.cleanupActivity(adPlaces)
 * ```
 */
object WaterfallAdHelper {

    private const val TAG = "WaterfallAdHelper"

    // Ad type constants
    private object AdType {
        const val NATIVE_VIEW = "native_view"
        const val FULL_SCREEN = "full_screen_ad"
        const val NATIVE_FULL = "native_fsn"
        const val BANNER = "banner"
    }


    /**
     * ⭐ UNIVERSAL AD LOADER - Supports both Native and Interstitial ads
     * Auto-detects ad type from config and loads accordingly
     *
     * @param activity Activity context
     * @param placementName Placement ID from ad_config.json
     * @param adView (Optional) For native ads - if provided, will show ad
     * @param waitForLoad For native ads - whether to wait if still loading
     * @param onSuccess Success callback
     * @param onFailure Failure callback
     */
    /**
     * @param lifecycleOwner When banner is inside a Fragment, pass the Fragment so reload
     *   pauses when fragment is hidden and resumes when fragment is visible again.
     */
    @JvmStatic
    fun loadAd(
        activity: Activity,
        placementName: String,
        activityName: String,
        adView: View? = null,
        waitForLoad: Boolean = true,
        onSuccess: (() -> Unit)? = null,
        onFailure: (() -> Unit)? = null,
        isShow: Boolean = true,
        lifecycleOwner: LifecycleOwner? = null
    ) {
        if (AdsHelper.isDisableObdAd())
        YNMAds.getInstance().setInitCallback {
            Logger.d( "===== loadAd START =====")
            Logger.d( "placementId: $placementName")
            val config = RemoteConfigManager.instance?.adConfig
            if (config == null) {
                Logger.e( "AdConfig is null")
                onFailure?.invoke()
                return@setInitCallback
            }

            // Get placement and detect ad type
            val placement = config.adPlacements?.get(placementName)
            if (placement == null) {
                Logger.e( "Placement not found: $placementName")
                onFailure?.invoke()
                return@setInitCallback
            }

            val adType = placement.type
            Logger.d( "Detected ad type: $adType")

            // Check if enabled
            if (!placement.enable) {
                Logger.d( "Placement $placementName is disabled")
                onFailure?.invoke()
                return@setInitCallback
            }

            // Route to appropriate loader based on ad type
            adView?.visibility = View.VISIBLE
            when (adType) {
                AdType.NATIVE_VIEW -> {
                    Logger.d( "Loading as NATIVE ad")
                    NativeService.loadNativeAd(
                        activity,
                        placementName,
                        activityName = activityName,
                        adView as YNMNativeAdView?,
                        waitForLoad,
                        onSuccess,
                        onFailure,
                        lifecycleOwner = lifecycleOwner
                    )
                }

                AdType.FULL_SCREEN, AdType.NATIVE_FULL -> {
                    Logger.d( "Loading FULL_SCREEN ad for $placementName (isShow=$isShow)")
                    if (isShow) {
                        val onShowResult: (Boolean) -> Unit = { shown ->
                            if (shown) onSuccess?.invoke() else onFailure?.invoke()
                        }
                        if (FullScreenService.isPreloaded(placementName)) {
                            Logger.d( "Ad already preloaded, showing for $placementName")
                            FullScreenService.showPreload(
                                activity = activity,
                                activityName = activityName,
                                adPlace = placementName,
                                callback = onShowResult
                            )
                        } else {
                            FullScreenService.loadAdsFullScreen(
                                context = activity,
                                activityName = activityName,
                                adPlace = placementName,
                                callback = { result ->
                                    if (result.success) {
                                        FullScreenService.showPreload(
                                            activity = activity,
                                            activityName = activityName,
                                            adPlace = placementName,
                                            callback = onShowResult
                                        )
                                    } else {
                                        onFailure?.invoke()
                                    }
                                }
                            )
                        }
                    } else {
                        // isShow = false: chỉ preload, không hiển thị
                        if (FullScreenService.isPreloaded(placementName)) {
                            onSuccess?.invoke()
                        } else {
                            FullScreenService.loadAdsFullScreen(
                                context = activity,
                                activityName = activityName,
                                adPlace = placementName,
                                callback = { result ->
                                    if (result.success) onSuccess?.invoke() else onFailure?.invoke()
                                }
                            )
                        }
                    }
                }

                AdType.BANNER -> {
                    Logger.d( "Loading as BANNER ad")
                    adView?.visibility = View.GONE
                    BannerService.loadAndShowBanner(
                        activity = activity,
                        activityName = activityName,
                        placementName = placementName,
                        callback = { success ->
                            if (success) onSuccess?.invoke() else onFailure?.invoke()
                        },
                        lifecycleOwner = lifecycleOwner
                    )
                }

                else -> {
                    Logger.w( "Unknown ad type: $adType")
                    adView?.visibility = View.GONE
                    onFailure?.invoke()
                }
            }
        }
    }


}