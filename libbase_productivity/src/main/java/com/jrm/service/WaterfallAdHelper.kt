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
        Logger.d("🔵 [$activityName] loadAd() called - placement: $placementName, isShow: $isShow, adView: ${if (adView != null) "provided" else "null"}")
        
        val isDisableObd = AdsHelper.isDisableObdAd()
        val isDisableAll = AdsHelper.isDisableAllAd()
        Logger.d("🔵 [$activityName] Ads status - isDisableObdAd: $isDisableObd, isDisableAllAd: $isDisableAll")
        
        if (isDisableObd){
            Logger.w("🔴 [$activityName] Ads disabled (isDisableObdAd), returning early")
            return
        }
        
        YNMAds.getInstance().setInitCallback {
            Logger.d("🟢 [$activityName] ===== loadAd START =====")
            Logger.d("🟢 [$activityName] placementId: $placementName, activityName: $activityName")
            
            val config = RemoteConfigManager.instance?.adConfig
            if (config == null) {
                Logger.e("🔴 [$activityName] AdConfig is null")
                onFailure?.invoke()
                return@setInitCallback
            }
            
            Logger.d("🟢 [SPLASH] AdConfig loaded, placements count: ${config.adPlacements?.size ?: 0}")

            // Get placement and detect ad type
            val placement = config.adPlacements?.get(placementName)
            if (placement == null) {
                Logger.e("🔴 [$activityName] Placement not found: $placementName")
                Logger.d("🟡 [$activityName] Available placements: ${config.adPlacements?.keys?.joinToString()}")
                onFailure?.invoke()
                return@setInitCallback
            }

            val adType = placement.type
            val placementEnable = placement.enable
            Logger.d("🟢 [$activityName] Placement found - type: $adType, enable: $placementEnable")
            Logger.d("🟢 [$activityName] Placement config: ${placement.toString()}")

            // Check if enabled
            if (!placementEnable) {
                Logger.w("🔴 [$activityName] Placement $placementName is disabled in config")
                onFailure?.invoke()
                return@setInitCallback
            }

            // Route to appropriate loader based on ad type
            Logger.d("🟢 [$activityName] Routing to ad loader - adType: $adType")
            adView?.visibility = View.VISIBLE
            when (adType) {
                AdType.NATIVE_VIEW -> {
                    Logger.d("🟡 [$activityName] Loading as NATIVE_VIEW ad")
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
                    Logger.d("🟡 [$activityName] Loading FULL_SCREEN ad for $placementName (isShow=$isShow)")
                    val isPreloaded = FullScreenService.isPreloaded(placementName)
                    Logger.d("🟡 [$activityName] Interstitial preloaded status: $isPreloaded")
                    
                    if (isShow) {
                        val onShowResult: (Boolean) -> Unit = { shown ->
                            Logger.d("🟡 [$activityName] Interstitial show result: $shown")
                            if (shown) {
                                Logger.d("✅ [$activityName] Interstitial shown successfully")
                                onSuccess?.invoke()
                            } else {
                                Logger.w("❌ [$activityName] Interstitial show failed")
                                onFailure?.invoke()
                            }
                        }
                        if (isPreloaded) {
                            Logger.d("🟡 [$activityName] Interstitial already preloaded, showing for $placementName")
                            FullScreenService.showPreload(
                                activity = activity,
                                activityName = activityName,
                                adPlace = placementName,
                                callback = { shown ->
                                    if (!shown) {
                                        // If show failed, try to load and show again
                                        Logger.w("🟡 [SPLASH] Preloaded ad show failed, loading fresh ad...")
                                        FullScreenService.loadAdsFullScreen(
                                            context = activity,
                                            activityName = activityName,
                                            adPlace = placementName,
                                            callback = { result ->
                                                Logger.d("🟡 [SPLASH] Fresh load result: success=${result.success}, format=${result.format}")
                                                if (result.success) {
                                                    FullScreenService.showPreload(
                                                        activity = activity,
                                                        activityName = activityName,
                                                        adPlace = placementName,
                                                        callback = onShowResult
                                                    )
                                                } else {
                                                    onShowResult(false)
                                                }
                                            }
                                        )
                                    } else {
                                        onShowResult(true)
                                    }
                                }
                            )
                        } else {
                            Logger.d("🟡 [$activityName] Interstitial not preloaded, loading now...")
                            FullScreenService.loadAdsFullScreen(
                                context = activity,
                                activityName = activityName,
                                adPlace = placementName,
                                callback = { result ->
                                    Logger.d("🟡 [$activityName] Interstitial load result: success=${result.success}, format=${result.format}, error=${result.error}")
                                    if (result.success) {
                                        Logger.d("🟡 [SPLASH] Interstitial loaded, now showing...")
                                        FullScreenService.showPreload(
                                            activity = activity,
                                            activityName = activityName,
                                            adPlace = placementName,
                                            callback = onShowResult
                                        )
                                    } else {
                                        Logger.w("❌ [$activityName] Interstitial load failed: ${result.error}")
                                        onFailure?.invoke()
                                    }
                                }
                            )
                        }
                    } else {
                        // isShow = false: chỉ preload, không hiển thị
                        Logger.d("🟡 [$activityName] Preloading interstitial (isShow=false)")
                        if (isPreloaded) {
                            Logger.d("✅ [$activityName] Interstitial already preloaded")
                            onSuccess?.invoke()
                        } else {
                            FullScreenService.loadAdsFullScreen(
                                context = activity,
                                activityName = activityName,
                                adPlace = placementName,
                                callback = { result ->
                                    Logger.d("🟡 [SPLASH] Interstitial preload result: success=${result.success}")
                                    if (result.success) onSuccess?.invoke() else onFailure?.invoke()
                                }
                            )
                        }
                    }
                }

                AdType.BANNER -> {
                    Logger.d("🟡 [$activityName] Loading as BANNER ad")
                    adView?.visibility = View.GONE
                    BannerService.loadAndShowBanner(
                        activity = activity,
                        activityName = activityName,
                        placementName = placementName,
                        callback = { success ->
                            Logger.d("🟡 [SPLASH] Banner callback - success: $success")
                            if (success) {
                                Logger.d("✅ [SPLASH] Banner loaded and shown successfully")
                                onSuccess?.invoke()
                            } else {
                                Logger.w("❌ [SPLASH] Banner failed to load/show")
                                onFailure?.invoke()
                            }
                        },
                        lifecycleOwner = lifecycleOwner
                    )
                }

                else -> {
                    Logger.w("🔴 [SPLASH] Unknown ad type: $adType")
                    adView?.visibility = View.GONE
                    onFailure?.invoke()
                }
            }
        }
    }


}