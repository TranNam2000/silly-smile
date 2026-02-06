package com.jrm.base

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.ads.nomyek_admob.admobs.AppOpenManager
import com.ads.nomyek_admob.ads_components.YNMAds
import com.ads.nomyek_admob.ads_components.YNMAdsCallbacks
import com.ads.nomyek_admob.ads_components.ads_native.YNMNativeAdView
import com.ads.nomyek_admob.event.YNMAirBridge
import com.ads.nomyek_admob.utils.AdsNativeMultiPreload
import com.google.android.gms.ads.nativead.NativeAd
import com.jrm.R
import com.jrm.utils.remote_config.RemoteConfigManager
import com.jrm.utils.AdsHelper
import com.jrm.utils.BaseConstants
import com.jrm.utils.purchase.IAPHelper

/**
 * Enhanced BaseObdFragment that combines functionality from BaseObdFragment and BaseFragmentScreen
 * Supports both ViewBinding and DataBinding
 * Includes native ads, screen tracking, and analytics
 */
abstract class BaseFragment<B : ViewBinding>(val bindingFactory: (LayoutInflater) -> B) :
    Fragment() {

    private var _binding: B? = null
    public val binding: B? get() = _binding
    
    // Screen tracking variables
    private var startTime: Long = 0
    
    // Native ads refresh variables
    private var refreshNativeTime: Long = 0
    private var refreshNativeHandler: Handler? = null
    private var refreshNativeRunnable: Runnable? = null
    private var isInForeground: Boolean = true

    // Abstract methods for subclasses
    abstract fun initView()
    abstract fun addEvent()
    
    // Optional methods with default implementations
    open fun loadAds() {}
    open fun getScreenName(): String? = null
    protected val fragmentName: String
        get() = this::class.java.simpleName
    open fun initViews() {
        initView()
    } // Alias for compatibility

    open fun initActions() {
        addEvent()
    } // Alias for compatibility

    open var isClick: Boolean = true

    open fun setViewRootClick() {}

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        _binding = bindingFactory(layoutInflater)
        
        // If binding is DataBinding, set lifecycle owner
        if (_binding is ViewDataBinding) {
            (_binding as ViewDataBinding).lifecycleOwner = viewLifecycleOwner
        }
        
        return _binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.setOnTouchListener(View.OnTouchListener { v, event ->
            setViewRootClick()
            v.clearFocus()
            isClick
        })
        // Set default refresh time from RemoteConfig
        setRefreshNativeTime(RemoteConfigManager.instance?.timeReloadNative ?: 0)

        // Initialize views and actions
        initViews()
        initActions()
        loadAds()
    }

    override fun onResume() {
        super.onResume()
        isInForeground = true
        startTime = System.currentTimeMillis()
        if (!isHidden) {
            logScreenViewEvent()
        }
    }
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)

        if (!hidden) {
            // Fragment is now visible
            isInForeground = true
            startTime = System.currentTimeMillis()
            logScreenViewEvent()
        } else {
            // Fragment is now hidden
            isInForeground = false
        }
    }
    private fun logScreenViewEvent() {
        // Log screen view event using BaseEventLogger
        fragmentName.let { screenName ->
            BaseEventLogger.logScreenView(screenName)
            BaseEventLogger.logScreenViewResume(screenName)
        }
    }

    override fun onPause() {
        super.onPause()
        isInForeground = false
        
        // Screen view complete is now handled automatically in BaseEventLogger.logScreenView()
        // No need to manually log complete events here
    }


    override fun onDestroyView() {
        super.onDestroyView()
        
        // Stop native refresh timer
        stopNativeRefreshTimer()
        
        // Unbind DataBinding if applicable
        if (_binding is ViewDataBinding) {
            (_binding as ViewDataBinding).unbind()
        }
        
        _binding = null
    }

    /**
     * Enhanced event logging using BaseEventLogger
     */
    open fun logEvent(eventName: String) {
        BaseEventLogger.logEvent(eventName)
    }
    
    open fun logEvent(eventName: String, custom: Map<String, Any?>?) {
        BaseEventLogger.logEvent(eventName, custom)
    }
    
    open fun logEvent(
        eventName: String,
        action: String,
        label: String,
        value: Number? = null,
        custom: Map<String, Any?>? = null
    ) {
        BaseEventLogger.logEvent(eventName, action, label, value, custom)
    }

    /**
     * Native ads functionality
     */
    fun setRefreshNativeTime(timeInSeconds: Long) {
        this.refreshNativeTime = timeInSeconds
    }

    fun showRefreshNative(listAdId: List<AdsNativeMultiPreload.AdIdModel>, adPlace: String) {
        // Load native initially
        loadNative(listAdId, adPlace)
        
        // Start refresh timer if refreshNativeTime > 0
        if (refreshNativeTime > 0) {
            startNativeRefreshTimer(listAdId, adPlace)
        }
    }

    private fun startNativeRefreshTimer(listAdId: List<AdsNativeMultiPreload.AdIdModel>, adPlace: String) {
        // Cancel existing timer if any
        stopNativeRefreshTimer()
        
        refreshNativeHandler = Handler(Looper.getMainLooper())
        refreshNativeRunnable = object : Runnable {
            override fun run() {
                // Only refresh if fragment is in foreground
                if (isInForeground) {
                    loadNative(listAdId, adPlace)
                }
                
                // Schedule next refresh
                refreshNativeHandler?.postDelayed(this, (refreshNativeTime * 1000).toLong())
            }
        }
        
        // Start the timer
        refreshNativeHandler?.postDelayed(refreshNativeRunnable!!, (refreshNativeTime * 1000).toLong())
    }

    private fun stopNativeRefreshTimer() {
        refreshNativeRunnable?.let { runnable ->
            refreshNativeHandler?.removeCallbacks(runnable)
        }
        refreshNativeRunnable = null
        refreshNativeHandler = null
    }

    fun loadNative(listAdId: List<AdsNativeMultiPreload.AdIdModel>, adPlace: String) {
        if (IAPHelper.isPremium()) {
            findViewByName<YNMNativeAdView>("nativeAd")?.let { adView ->
                adView.visibility = View.GONE
            }
            return
        }
        if (activity == null || AdsHelper.isDisableAllAd()) return
        YNMAds.getInstance().setInitCallback {
            activity?.let {
                var adView: YNMNativeAdView? = findViewByName<YNMNativeAdView>("nativeAd")

                if (adView != null) {
                    AdsHelper.checkAndShowNativeMissing(it, R.layout.custom_native_admob_medium, listOf(
                        BaseConstants.INTER_SPLASH, BaseConstants.NATIVE_SPLASH,
                        BaseConstants.NATIVE_LANGUAGE2, BaseConstants.NATIVE_ONBOARD_1, BaseConstants.NATIVE_ONBOARD_2, BaseConstants.NATIVE_ONBOARD_3, BaseConstants.NATIVE_ONBOARD_4, BaseConstants.NATIVE_ONBOARD_5), adView,
                        {
                        }
                    ) {
                        AdsNativeMultiPreload.preloadMultipleNativeAds(
                            it,
                            YNMAirBridge.AppData(fragmentName, adPlace),
                            listAdId,
                            adPlace,
                            object : YNMAdsCallbacks() {
                                override fun onNativeAdLoaded(nativeAd: NativeAd) {
                                    super.onNativeAdLoaded(nativeAd)
                                    // Show the native ad in the native ad view if available
                                    findViewByName<YNMNativeAdView>("nativeAd")?.let { adView ->
                                        AdsNativeMultiPreload.showPreloadedNativeAd(
                                            it,
                                            adView,
                                            adPlace,
                                            R.layout.custom_native_admob_medium,
                                            R.layout.custom_native_admob_medium
                                        )
                                    }
                                }

                                override fun onAdClicked() {
                                    super.onAdClicked()
                                    // Log ad click event
                                    logEvent(
                                        "native_ad_clicked",
                                        "click",
                                        adPlace,
                                        1,
                                        mapOf(
                                            "screen_name" to fragmentName,
                                            "ad_place" to adPlace
                                        )
                                    )
                                }
                            }
                        )
                    }
                }

            }
        }
    }

    /**
     * Safe way to find view by name across different modules
     * Returns null if view doesn't exist instead of crashing
     */
    private inline fun <reified T : View> findViewByName(viewName: String): T? {
        return try {
            val resourceId = resources.getIdentifier(viewName, "id", requireContext().packageName)
            if (resourceId != 0) {
                view?.findViewById<T>(resourceId)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Convenience methods for DataBinding support
     */
    @Suppress("UNCHECKED_CAST")
    protected fun <DB : ViewDataBinding> getDataBinding(): DB? {
        return _binding as? DB
    }

}