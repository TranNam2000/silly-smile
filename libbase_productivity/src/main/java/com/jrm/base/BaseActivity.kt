package com.jrm.base

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.lifecycleScope
import com.ads.nomyek_admob.admobs.AppOpenManager
import com.ads.nomyek_admob.ads_components.YNMAds
import com.ads.nomyek_admob.ads_components.YNMAdsCallbacks
import com.ads.nomyek_admob.ads_components.ads_banner.YNMBannerAdView
import com.ads.nomyek_admob.ads_components.ads_native.YNMNativeAdView
import com.ads.nomyek_admob.ads_components.wrappers.AdsError
import com.ads.nomyek_admob.event.YNMAirBridge
import com.ads.nomyek_admob.utils.AdsNativeMultiPreload
import com.google.android.gms.ads.nativead.NativeAd
import com.jrm.BuildConfig
import com.jrm.R
import com.jrm.ads.CollapsibleNativeAdManager
import com.jrm.utils.BaseConstants
import com.jrm.utils.remote_config.RemoteConfigManager
import com.jrm.utils.LocaleHelper
import com.jrm.utils.SharedPref
import com.jrm.utils.AdsHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

public abstract class BaseActivity<VB : ViewDataBinding> : AppCompatActivity() {
    protected lateinit var viewBinding: VB
    public var activityName: String = this::class.java.simpleName
    private var listBannerId: List<String> = listOf()
    private var refreshBannerTime: Int = 0
    private var refreshNativeBannerTime: Long = 0
    private var refreshNativeTime: Long = 0
    private var refreshHandler: Handler? = null
    private var refreshRunnable: Runnable? = null
    private var refreshNativeHandler: Handler? = null
    private var refreshNativeRunnable: Runnable? = null
    private var refreshGeneralNativeHandler: Handler? = null
    private var refreshGeneralNativeRunnable: Runnable? = null
    public var isInForeground: Boolean = true
    private var typeBanner: String = "normal"
    private var interstitialCheckJob: Job? = null

    // Track current native ad configuration for refresh
    private var currentNativeAdConfig: NativeAdConfig? = null

    private data class NativeAdConfig(
        val listAdId: List<AdsNativeMultiPreload.AdIdModel>,
        val adPlace: String,
        val nativeAdViewId: String,
        val layoutResId: Int
    )

    // Flag to disable screen tracking for container activities (activities that only host fragments)
    protected var enableScreenTracking: Boolean = true

    // Collapsible native ad manager
    private var collapsibleNativeAdManager: CollapsibleNativeAdManager? = null

    // Flag to enable collapsible native ad (default is false)
    protected var enableCollapsibleNativeAd: Boolean = false

    // Flag to disable auto-load collapsible native in onResume (for custom management)
    protected var disableAutoLoadCollapsibleNative: Boolean = false

    // Coroutine job for checking interstitial status
    private var checkInterstitialJob: Job? = null

    fun setListBannerId(bannerIds: List<String>) {
        this.listBannerId = bannerIds
    }

    fun setRefreshBannerTime(timeInSeconds: Int) {
        this.refreshBannerTime = timeInSeconds
    }

    fun setRefreshNativeBannerTime(timeInSeconds: Long) {
        this.refreshNativeBannerTime = timeInSeconds
    }

    fun setRefreshNativeTime(timeInSeconds: Long) {
        this.refreshNativeTime = timeInSeconds
    }

    fun setBannerType(type: String) {
        this.typeBanner = type;
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        initWindow(window)
        fullScreenCall(window)
        super.onCreate(savedInstanceState)

        // Initialize the view binding
        viewBinding = DataBindingUtil.setContentView(this, getLayoutActivity())
        setContentView(viewBinding.root)
        setRefreshNativeBannerTime(RemoteConfigManager.instance!!.timeReloadNativeBanner)
        setRefreshNativeTime(RemoteConfigManager.instance?.timeReloadNative ?: 0)
        // Initialize collapsible native ad if enabled
        if (enableCollapsibleNativeAd) {
            initCollapsibleNativeAd()
        }
        initViews()

        // Add padding to the top to avoid status bar hiding content
        findViewById<View>(android.R.id.content)?.let { contentView ->
            ViewCompat.setOnApplyWindowInsetsListener(contentView) { v, insets ->
                val topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
                val paddingTop = topInset.coerceAtLeast(50) // Ensure padding top is at least 50 or the status bar height
                v.setPadding(0, paddingTop, 0, 0)
                v.setBackgroundColor(resources.getColor(R.color.white))
                insets
            }
        }

        // Log screen view event using BaseEventLogger (only if screen tracking is enabled)
        if (enableScreenTracking) {
            activityName?.let { screenName ->
                BaseEventLogger.logScreenView(screenName)
            }
        }
        if (YNMAds.isGoHome &&!AdsHelper.isInterBlocking() && firstLoadInter) {
            firstLoadInter = false
            AdsHelper.preloadInterClick(this, activityName)
        }
    }

    protected abstract fun getLayoutActivity(): Int

    protected abstract fun initViews()

    fun loadCollapsibleBanner(id:String, gravity: String? = "bottom") {
        findViewByName<View>("bannerView")?.let {
            if (!AdsHelper.isDisableAllAd()) {
                YNMAds.getInstance().setInitCallback {
                    YNMAds.getInstance().loadCollapsibleBanner(this, id, gravity, YNMAdsCallbacks(YNMAirBridge.AppData(activityName, "banner"), YNMAds.BANNER))
                }
            } else {
                it.visibility = View.GONE
            }        }
    }

    fun loadCollapsibleBanner(id:String) {
        findViewByName<View>("bannerView")?.let {
            if (!AdsHelper.isDisableAllAd()) {
                YNMAds.getInstance().setInitCallback {
                    YNMAds.getInstance().loadCollapsibleBanner(this, id, "bottom",
                        YNMAdsCallbacks(YNMAirBridge.AppData(activityName, "banner"), YNMAds.BANNER)
                    )
                }
            } else {
                it.visibility = View.GONE
            }
        }
    }

    fun loadBanner(id:String) {
        findViewByName<View>("bannerView")?.let {
            if (!AdsHelper.isDisableAllAd()) {
                YNMAds.getInstance().setInitCallback {
                    findViewByName<YNMBannerAdView>("bannerView")?.loadBanner(this, id,object : YNMAdsCallbacks(YNMAirBridge.AppData(activityName, "banner_home"), YNMAds.BANNER) {
                        override fun onAdLoaded() {
                            super.onAdLoaded()
                        }

                        override fun onAdFailedToLoad(adError: AdsError?) {
                            super.onAdFailedToLoad(adError)
                        }
                    })
                }
            } else {
                it.visibility = View.GONE
            }
        }
    }

    fun loadMultiIdBanner() {
        findViewByName<View>("bannerView")?.let {
            if (!AdsHelper.isDisableAllAd() && listBannerId.isNotEmpty()) {
                YNMAds.getInstance().setInitCallback {
                    loadBannerWithFallback(0)
                }
            } else {
                it.visibility = View.GONE
            }
        }
    }

    private fun loadBannerWithFallback(index: Int) {
        if (index >= listBannerId.size) {
            // All banner IDs failed, hide banner view
            findViewByName<View>("bannerView")?.visibility = View.GONE
            return
        }

        val bannerId = listBannerId[index]
        if (typeBanner.compareTo("normal") == 0) {
            findViewByName<YNMBannerAdView>("bannerView")?.loadBanner(
                this,
                bannerId,
                object : YNMAdsCallbacks(YNMAirBridge.AppData(activityName, "banner"), YNMAds.BANNER) {
                    override fun onAdLoaded() {
                        super.onAdLoaded()
                        // Banner loaded successfully, stop trying other IDs
                    }

                    override fun onAdFailedToLoad(adError: AdsError?) {
                        super.onAdFailedToLoad(adError)
                        // Current banner failed, try next ID
                        loadBannerWithFallback(index + 1)
                    }
                }
            )
        } else {
            YNMAds.getInstance().loadCollapsibleBanner(this, bannerId, "bottom", object : YNMAdsCallbacks(YNMAirBridge.AppData(activityName, "banner"), YNMAds.BANNER) {
                override fun onAdLoaded() {
                    super.onAdLoaded()
                    // Banner loaded successfully, stop trying other IDs
                }

                override fun onAdFailedToLoad(adError: AdsError?) {
                    super.onAdFailedToLoad(adError)
                    // Current banner failed, try next ID
                    loadBannerWithFallback(index + 1)
                }
            })
        }

    }

    fun showRefreshMultiIdBanner() {
        // Load banner initially
        loadMultiIdBanner()

        // Start refresh timer if refreshBannerTime > 0
        if (refreshBannerTime > 0) {
            startBannerRefreshTimer()
        }
    }

    private fun startBannerRefreshTimer() {
        // Cancel existing timer if any
        stopBannerRefreshTimer()

        refreshHandler = Handler(Looper.getMainLooper())
        refreshRunnable = object : Runnable {
            override fun run() {
                // Only refresh if app is in foreground
                if (isInForeground) {
                    loadMultiIdBanner()
                }

                // Schedule next refresh
                refreshHandler?.postDelayed(this, (refreshBannerTime * 1000).toLong())
            }
        }

        // Start the timer
        refreshHandler?.postDelayed(refreshRunnable!!, (refreshBannerTime * 1000).toLong())
    }

    private fun stopBannerRefreshTimer() {
        refreshRunnable?.let { runnable ->
            refreshHandler?.removeCallbacks(runnable)
        }
        refreshRunnable = null
        refreshHandler = null
    }

    private fun startNativeBannerRefreshTimer() {
        // Cancel existing timer if any
        stopNativeBannerRefreshTimer()

        refreshNativeHandler = Handler(Looper.getMainLooper())
        refreshNativeRunnable = object : Runnable {
            override fun run() {
                // Only refresh if app is in foreground
                loadNativeBanner()
                // Schedule next refresh
                refreshNativeHandler?.postDelayed(this, (refreshNativeBannerTime * 1000).toLong())
            }
        }

        // Start the timer
        refreshNativeHandler?.postDelayed(refreshNativeRunnable!!, (refreshNativeBannerTime * 1000).toLong())
    }

    private fun stopNativeBannerRefreshTimer() {
        refreshNativeRunnable?.let { runnable ->
            refreshNativeHandler?.removeCallbacks(runnable)
        }
        refreshNativeRunnable = null
        refreshNativeHandler = null
    }

    fun showRefreshNativeBanner() {
        // Load native banner initially
        loadNativeBanner()

        // Start refresh timer if refreshNativeBannerTime > 0
        if (refreshNativeBannerTime > 0) {
            startNativeBannerRefreshTimer()
        }
    }

    /**
     * General native ads methods with customizable parameters
     */
    fun showRefreshNative(
        listAdId: List<AdsNativeMultiPreload.AdIdModel>,
        adPlace: String,
        nativeAdViewId: String = "nativeAd",
        layoutResId: Int = com.jrm.R.layout.custom_native_admob_medium
    ) {
        // Save configuration for refresh
        currentNativeAdConfig = NativeAdConfig(listAdId, adPlace, nativeAdViewId, layoutResId)

        // Load native ad initially
        loadNative(listAdId, adPlace, nativeAdViewId, layoutResId)

        // Start refresh timer if refreshNativeTime > 0
        if (refreshNativeTime > 0) {
            startGeneralNativeRefreshTimer()
        }
    }

    fun loadNative(
        listAdId: List<AdsNativeMultiPreload.AdIdModel>,
        adPlace: String,
        nativeAdViewId: String = "nativeAd",
        layoutResId: Int = com.jrm.R.layout.custom_native_admob_medium
    ) {
        if (!isInForeground || AppOpenManager.getInstance().isInterstitialShowing || isDestroyed || isFinishing) {
            return
        }

        if (AdsHelper.isDisableAllAd()) {
            findViewByName<YNMNativeAdView>(nativeAdViewId)?.visibility = View.GONE
            return
        }

        YNMAds.getInstance().setInitCallback {
            ////////
            YNMAds.getInstance().setInitCallback {
                var adView: YNMNativeAdView? = findViewByName<YNMNativeAdView>(nativeAdViewId)
                if (adView != null) {
                    AdsHelper.checkAndShowNativeMissing(this, layoutResId, listOf(BaseConstants.NATIVE_SPLASH,
                        BaseConstants.NATIVE_LANGUAGE2, BaseConstants.NATIVE_ONBOARD_1, BaseConstants.NATIVE_ONBOARD_2, BaseConstants.NATIVE_ONBOARD_3, BaseConstants.NATIVE_ONBOARD_4, BaseConstants.NATIVE_ONBOARD_5), adView,
                        {
                        }
                    ) {
                        AdsNativeMultiPreload.preloadMultipleNativeAds(
                            this,
                            YNMAirBridge.AppData(activityName, adPlace),
                            listAdId,
                            adPlace,
                            object : YNMAdsCallbacks() {
                                override fun onNativeAdLoaded(nativeAd: NativeAd) {
                                    super.onNativeAdLoaded(nativeAd)
                                    // Show the native ad in the native ad view if available
                                    findViewByName<YNMNativeAdView>(nativeAdViewId)?.let { adView ->
                                        AdsNativeMultiPreload.showPreloadedNativeAd(
                                            this@BaseActivity,
                                            adView,
                                            adPlace,
                                            layoutResId,
                                            layoutResId
                                        )
                                    }
                                }

                                override fun onAdClicked() {
                                    super.onAdClicked()
                                    // Log ad click event
                                    BaseEventLogger.logEvent(
                                        "native_ad_clicked",
                                        "click",
                                        adPlace,
                                        1,
                                        mapOf(
                                            "screen_name" to activityName,
                                            "ad_place" to adPlace
                                        )
                                    )
                                }
                            }
                        )
                    }
                }
            }
            ////////

        }
    }

    private fun startGeneralNativeRefreshTimer() {
        // Cancel existing timer if any
        stopGeneralNativeRefreshTimer()

        refreshGeneralNativeHandler = Handler(Looper.getMainLooper())
        refreshGeneralNativeRunnable = object : Runnable {
            override fun run() {
                // Only refresh if app is in foreground and config exists
                if (isInForeground) {
                    currentNativeAdConfig?.let { config ->
                        loadNative(config.listAdId, config.adPlace, config.nativeAdViewId, config.layoutResId)
                    }
                }
                // Schedule next refresh
                refreshGeneralNativeHandler?.postDelayed(this, (refreshNativeTime * 1000).toLong())
            }
        }

        // Start the timer
        refreshGeneralNativeHandler?.postDelayed(refreshGeneralNativeRunnable!!, (refreshNativeTime * 1000).toLong())
    }

    private fun stopGeneralNativeRefreshTimer() {
        refreshGeneralNativeRunnable?.let { runnable ->
            refreshGeneralNativeHandler?.removeCallbacks(runnable)
        }
        refreshGeneralNativeRunnable = null
        refreshGeneralNativeHandler = null
    }

    companion object {
        fun initWindow(window: Window) {
            val background = ColorDrawable(Color.parseColor("#FFFFFF"))
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = window.context.resources.getColor(R.color.black)
            window.setBackgroundDrawable(background)
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

            window.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }

        fun fullScreenCall(window: Window) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        }
        var firstLoadInter: Boolean = true
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(
            LocaleHelper.setLocale(
                newBase,
                SharedPref.readString(BaseConstants.LANG_CODE_STORE, "en")
            )
        )
    }

    var showedCollapNative = false

    override fun onResume() {
        super.onResume()
        isInForeground = true
        if (AppOpenManager.getInstance().isInterstitialShowing) {
            // Wait until interstitial is closed before logging screen view
            waitForInterstitialToClose()
        } else {
            BaseEventLogger.logScreenViewResume(activityName)
            onResumeAfterInter()
        }


        // Cancel any existing check job
        checkInterstitialJob?.cancel()

        // Start checking for interstitial and load collapsible native ad when ready

        checkInterstitialJob = lifecycleScope.launch {
            // Wait 1 second before first check
            delay(1000)

            // Loop until interstitial is not showing
            while (AppOpenManager.getInstance().isInterstitialShowing) {
                delay(1000) // Wait 1 second before next check
            }

        }

    }

    protected open fun onResumeAfterInter() {

    }

    /**
     * Wait for interstitial ad to close before logging screen view
     * Will cancel if user exits the app (onPause/onDestroy)
     */
    private fun waitForInterstitialToClose() {
        // Cancel any existing check
        stopInterstitialCheck()

        interstitialCheckJob = lifecycleScope.launch {
            // Check every 1 second until interstitial is closed or user exits
            Log.d(activityName, "waitForInterstitialToClose: ")
            while (isActive && isInForeground) {
                // Wait 1 second before checking
                delay(500)

                Log.d(activityName, "waitForInterstitialToClose run ")
                if (!AppOpenManager.getInstance().isInterstitialShowing) {
                    // Interstitial closed, log screen view
                    BaseEventLogger.logScreenViewResume(activityName)
                    onResumeAfterInter()
                    break
                }
            }
            Log.d(activityName, "waitForInterstitialToClose end ")
        }
    }

    /**
     * Stop checking for interstitial state
     */
    private fun stopInterstitialCheck() {
        interstitialCheckJob?.cancel()
        interstitialCheckJob = null
    }

    override fun onPause() {
        super.onPause()
        isInForeground = false
        // Cancel interstitial check when user exits
        stopInterstitialCheck()
        // Cancel the check job when activity is paused
        checkInterstitialJob?.cancel()
        checkInterstitialJob = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopBannerRefreshTimer()
        stopNativeBannerRefreshTimer()
        stopGeneralNativeRefreshTimer()
        stopInterstitialCheck()
        destroyCollapsibleNativeAd()

        // Cancel the check job when activity is destroyed
        checkInterstitialJob?.cancel()
        checkInterstitialJob = null

    }

    fun hideKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            // Find the currently focused view in the activity
            val currentFocus = this.currentFocus

            // Check if the focused view is an EditText
            if (currentFocus is EditText) {
                val outRect = Rect()
                currentFocus.getGlobalVisibleRect(outRect)

                // If the touch event is outside the bounds of the focused EditText...
                if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    // ...clear its focus and hide the keyboard.
                    currentFocus.clearFocus()
                    hideKeyboard(currentFocus)
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    private fun hideKeyboard(view: View) {
        // Get the InputMethodManager from the context
        val imm = this.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    /**
     * Safe way to find view by name across different modules
     * Returns null if view doesn't exist instead of crashing
     */
    private inline fun <reified T : View> findViewByName(viewName: String): T? {
        return try {
            val resourceId = resources.getIdentifier(viewName, "id", packageName)
            if (resourceId != 0) {
                findViewById<T>(resourceId)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    fun loadNativeBanner() {
        if (!isInForeground || AppOpenManager.getInstance().isInterstitialShowing || isDestroyed || isFinishing) {
            return
        }
        if (AdsHelper.isDisableAllAd()) {
            findViewByName<YNMNativeAdView>("native_banner")?.visibility = View.GONE
            return
        }
        if (enableCollapsibleNativeAd && collapsibleNativeAdManager != null) {
            if (collapsibleNativeAdManager!!.isExpanded()) {
                return
            }
        }
        YNMAds.getInstance().setInitCallback {
            val listAdId: List<AdsNativeMultiPreload.AdIdModel> = RemoteConfigManager.instance!!.getListAdIdNativeFromRemote(BaseConstants.NATIVE_BANNER, RemoteConfigManager.instance!!.homeNabanerIds)
            AdsNativeMultiPreload.preloadMultipleNativeAds(
                this,
                YNMAirBridge.AppData(activityName, BaseConstants.NATIVE_BANNER),
                listAdId,
                BaseConstants.NATIVE_BANNER,
                object : YNMAdsCallbacks() {
                    override fun onNativeAdLoaded(nativeAd: NativeAd) {
                        super.onNativeAdLoaded(nativeAd)
                        // Show the native ad in the native ad view if available
                        findViewByName<YNMNativeAdView>("native_banner")?.let { adView ->
                            AdsNativeMultiPreload.showPreloadedNativeAd(
                                this@BaseActivity,
                                adView,
                                BaseConstants.NATIVE_BANNER,
                                R.layout.custom_native_admob_banner,
                                R.layout.custom_native_admob_banner
                            )
                        }
                    }
                }
            )
        }
    }

    fun detachKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        val currentFocusedView: View? = getCurrentFocus()

        if (currentFocusedView != null) {
            imm.hideSoftInputFromWindow(currentFocusedView.getWindowToken(), 0)
        }
    }

    /**
     * Initialize collapsible native ad manager
     * This is called automatically if enableCollapsibleNativeAd is set to true
     */
    private fun initCollapsibleNativeAd() {
        try {
            val rootLayout = findViewById<View>(android.R.id.content) as? android.view.ViewGroup
            if (rootLayout != null) {
                collapsibleNativeAdManager = com.jrm.ads.CollapsibleNativeAdManager(this, rootLayout)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Load collapsible native ad
     * Call this method from child activities after setting enableCollapsibleNativeAd = true
     *
     * @param listAdId List of ad ID models
     * @param adPlace Ad placement identifier
     * @param screenName Screen name for tracking (defaults to activityName)
     * @param onCollapse Callback when user collapses the ad
     */    protected fun loadCollapsibleNativeAd(
        listAdId: List<AdsNativeMultiPreload.AdIdModel>,
        adPlace: String,
        screenName: String = activityName,
        onCollapse: (() -> Unit)? = null
    ) {
        if (!enableCollapsibleNativeAd) {
            return
        }

        (collapsibleNativeAdManager as? com.jrm.ads.CollapsibleNativeAdManager)?.let { manager ->
            // Set collapse callback if provided
            onCollapse?.let { callback ->
                manager.setOnCollapseCallback(callback)
            }

            // Load ad
            manager.loadNativeAdWithList(listAdId, adPlace, screenName)
        }
    }

    /**
     * Show preloaded collapsible native ad
     * Call this method to show ads that were preloaded in previous activities
     *
     * @param adPlace Ad placement identifier (same as used in preloadAds)
     * @param onCollapse Callback when user collapses the ad
     * @param onAdFailed Callback when ad fails to show (optional)
     */
    protected fun showCollapsibleNativeAdWithCallback(
        adPlace: String,
        onCollapse: (() -> Unit)? = null,
        onAdFailed: (() -> Unit)? = null
    ) {
        if (!enableCollapsibleNativeAd) {
            onAdFailed?.invoke()
            return
        }

        (collapsibleNativeAdManager as? com.jrm.ads.CollapsibleNativeAdManager)?.let { manager ->
            // Set collapse callback
            onCollapse?.let { callback ->
                manager.setOnCollapseCallback(callback)
            }

            // Set ad loaded callback
            manager.setOnAdLoadedCallback {
                Log.d(activityName, "Preloaded collapsible native ad shown successfully")
            }

            // Set ad failed callback
            manager.setOnAdFailedCallback {
                Log.e(activityName, "Failed to show preloaded collapsible native ad")
                onAdFailed?.invoke()
            }

            // Show preloaded ad
            manager.showPreloadedAd(adPlace)
        } ?: run {
            // Manager is null
            onAdFailed?.invoke()
        }
    }

    /**
     * Wait for preloaded ad and show when ready
     * This method will:
     * - Show immediately if ad is already preloaded
     * - Wait for preload to complete if still loading
     * - Call onAdFailed if preload fails or no preload exists
     *
     * @param adPlace Ad placement identifier (same as used in preloadAds)
     * @param onCollapse Callback when user collapses the ad
     * @param onAdFailed Callback when ad fails to show or preload fails
     */
    protected fun waitAndShowCollapsibleNativeAd(
        adPlace: String,
        onCollapse: (() -> Unit)? = null,
        onAdFailed: (() -> Unit)? = null
    ) {
        if (!enableCollapsibleNativeAd) {
            onAdFailed?.invoke()
            return
        }

        // Wait for preload to complete
        com.jrm.ads.CollapsibleNativeAdManager.waitForPreload(adPlace) { success ->
            if (success) {
                // Preload succeeded, show the ad
                Log.d(activityName, "Preload completed successfully, showing ad")
                showCollapsibleNativeAdWithCallback(adPlace, onCollapse, onAdFailed)
            } else {
                // Preload failed or doesn't exist
                Log.e(activityName, "Preload failed or doesn't exist")
                onAdFailed?.invoke()
            }
        }
    }

    /**
     * Show preloaded collapsible native ad with smart handling
     * This method will:
     * 1. If ad is already preloaded -> show immediately
     * 2. If ad is loading -> wait for it to complete, then show
     * 3. If preload failed or doesn't exist -> call onAdFailed
     *
     * @param adPlace Ad placement identifier (same as used in preloadAds)
     * @param adConfigString Config string for preload if not already loading (format: "MAX_NATIVE:id,NATIVE:id")
     * @param onCollapse Callback when user collapses the ad (e.g., start waterfall ads)
     * @param onAdFailed Callback when ad fails to show or preload fails
     */
    protected fun showPreloadedCollapsibleNativeAdSmart(
        adPlace: String,
        adConfigString: String = "",
        onCollapse: (() -> Unit)? = null,
        onAdFailed: (() -> Unit)? = null
    ) {
        if (!enableCollapsibleNativeAd) {
            onAdFailed?.invoke()
            return
        }

        // Check preload status
        val isAlreadyPreloaded = CollapsibleNativeAdManager.isPreloaded(adPlace)
        val isCurrentlyLoading = CollapsibleNativeAdManager.isLoading(adPlace)

        when {
            // Case 1: Ad is already preloaded - show immediately
            isAlreadyPreloaded -> {
                Log.d(activityName, "[$adPlace] Ad already preloaded, showing immediately")
                showCollapsibleNativeAdWithCallback(
                    adPlace = adPlace,
                    onCollapse = onCollapse,
                    onAdFailed = onAdFailed
                )
            }

            // Case 2: Ad is loading - wait for it to complete
            isCurrentlyLoading -> {
                Log.d(activityName, "[$adPlace] Ad is loading, waiting for completion...")
                waitAndShowCollapsibleNativeAd(
                    adPlace = adPlace,
                    onCollapse = onCollapse,
                    onAdFailed = {
                        // Preload failed after waiting - try load from scratch if config provided
                        Log.d(activityName, "[$adPlace] Preload failed after waiting")
                        if (adConfigString.isNotEmpty()) {
                            Log.d(activityName, "[$adPlace] Loading from scratch with config")
                            loadCollapsibleNativeAdFromConfig(adPlace, adConfigString, onCollapse)
                        } else {
                            onAdFailed?.invoke()
                        }
                    }
                )
            }

            // Case 3: No preload or preload failed - try load from scratch if config provided
            else -> {
                Log.d(activityName, "[$adPlace] No preload or preload failed")
                if (adConfigString.isNotEmpty()) {
                    Log.d(activityName, "[$adPlace] Starting preload with config")
                    // Start preload
                    CollapsibleNativeAdManager.preloadAds(
                        this,
                        adConfigString,
                        adPlace,
                        activityName,
                        com.jrm.R.layout.custom_native_admob_collap
                    )
                    // Wait for it to complete
                    waitAndShowCollapsibleNativeAd(
                        adPlace = adPlace,
                        onCollapse = onCollapse,
                        onAdFailed = onAdFailed
                    )
                } else {
                    onAdFailed?.invoke()
                }
            }
        }
    }

    // Track retry attempts to prevent infinite loops
    private val collapsibleNativeAdRetryCount = mutableMapOf<String, Int>()
    private val MAX_COLLAPSIBLE_NATIVE_AD_RETRY = 2

    /**
     * Load collapsible native ad from config string
     * @param adPlace Ad placement identifier
     * @param adConfigString Config string format "MAX_NATIVE:id,NATIVE:id"
     * @param onCollapse Callback when user collapses the ad
     */
    private fun loadCollapsibleNativeAdFromConfig(
        adPlace: String,
        adConfigString: String,
        onCollapse: (() -> Unit)? = null
    ) {
        // Check retry count to prevent infinite loop
        val retryCount = collapsibleNativeAdRetryCount.getOrDefault(adPlace, 0)
        if (retryCount >= MAX_COLLAPSIBLE_NATIVE_AD_RETRY) {
            Log.e(activityName, "[$adPlace] Max retry attempts reached ($MAX_COLLAPSIBLE_NATIVE_AD_RETRY), stopping to prevent infinite loop")
            collapsibleNativeAdRetryCount.remove(adPlace)
            return
        }
        
        // Increment retry count
        collapsibleNativeAdRetryCount[adPlace] = retryCount + 1
        Log.d(activityName, "[$adPlace] Loading from config, retry attempt: ${retryCount + 1}")
        
        (collapsibleNativeAdManager as? CollapsibleNativeAdManager)?.let { manager ->
            // Clear previous callbacks to prevent stale references
            manager.setOnAdFailedCallback(null)
            manager.setOnAdLoadedCallback(null)
            
            // Set collapse callback
            onCollapse?.let { callback ->
                manager.setOnCollapseCallback(callback)
            }
            
            // Set success callback to reset retry count
            manager.setOnAdLoadedCallback {
                Log.d(activityName, "[$adPlace] Ad loaded successfully, resetting retry count")
                collapsibleNativeAdRetryCount.remove(adPlace)
            }

            // Load ad with config string
            manager.loadNativeAdWithConfig(adConfigString, adPlace, activityName)
        }
    }


    protected fun setCollapsibleNativeCloseButtonPosition(position: CollapsibleNativeAdManager.CloseButtonPosition) {
        (collapsibleNativeAdManager as? CollapsibleNativeAdManager)?.setCloseButtonPosition(position)
    }

    /**
     * Toggle collapse/expand state of the collapsible native ad
     */
    protected fun toggleCollapsibleNativeAd() {
        (collapsibleNativeAdManager as? CollapsibleNativeAdManager)?.toggleCollapse()
    }

    /**
     * Hide the collapsible native ad
     */
    protected fun hideCollapsibleNativeAd() {
        (collapsibleNativeAdManager as? CollapsibleNativeAdManager)?.hide()
    }

    /**
     * Show the collapsible native ad
     */
    protected fun showCollapsibleNativeAd() {
        (collapsibleNativeAdManager as? CollapsibleNativeAdManager)?.show()
    }

    /**
     * Check if collapsible native ad is expanded
     */
    protected fun isCollapsibleNativeAdExpanded(): Boolean {
        return (collapsibleNativeAdManager as? CollapsibleNativeAdManager)?.isExpanded() ?: false
    }

    /**
     * Destroy collapsible native ad manager
     */
    private fun destroyCollapsibleNativeAd() {
        (collapsibleNativeAdManager as? CollapsibleNativeAdManager)?.destroy()
        collapsibleNativeAdManager = null
    }

}