package com.jrm.ads

import android.app.Activity
import android.util.DisplayMetrics
import com.jrm.utils.Logger
import android.view.View
import android.view.ViewGroup
import com.ads.nomyek_admob.ads_components.YNMAds
import com.ads.nomyek_admob.ads_components.YNMAdsCallbacks
import com.ads.nomyek_admob.ads_components.ads_banner.YNMBannerAdView
import com.ads.nomyek_admob.ads_components.wrappers.AdsError
import com.ads.nomyek_admob.event.YNMAirBridge
import com.ads.nomyek_admob.event.YNMLogEventManager
import com.ads.nomyek_admob.utils.TypeAds
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnPaidEventListener

object BannerAdHelper {

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
                Logger.d( "Starting to load inline adaptive banner: $adId")
                
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
                        Logger.d( "Inline adaptive banner loaded successfully")
                        
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
                        Logger.e( "Inline adaptive banner failed to load: ${adError.message} (code: ${adError.code})")
                        container.visibility = View.GONE
                        fallback.run()
                    }

                    override fun onAdClicked() {
                        super.onAdClicked()
                        Logger.d( "Inline adaptive banner clicked")
                    }

                    override fun onAdImpression() {
                        super.onAdImpression()
                        Logger.d( "Inline adaptive banner impression")
                    }

                    override fun onAdOpened() {
                        super.onAdOpened()
                        Logger.d( "Inline adaptive banner opened")
                    }

                    override fun onAdClosed() {
                        super.onAdClosed()
                        Logger.d( "Inline adaptive banner closed")
                    }
                }
                
                // Load the ad
                val adRequest = AdRequest.Builder().build()
                adView.loadAd(adRequest)
                
            } catch (e: Exception) {
                Logger.e( "Error loading inline adaptive banner", e)
                container.visibility = View.GONE
                fallback.run()
            }
        }
    }

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
    }
}
