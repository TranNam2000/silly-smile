package com.jrm.onboarding.language

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ListView
import android.widget.TextView
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.ads.nomyek_admob.admobs.AppOpenManager
import com.jrm.utils.BaseConstants
import com.jrm.base.BaseActivity
import com.jrm.onboarding.onboarding.OnboardingActivity
import com.jrm.utils.remote_config.RemoteConfigManager
import com.jrm.R
import com.jrm.ads.WaterfallNativeAdManager
import com.jrm.base.BaseEventLogger
import com.jrm.base.tracking.TrackableButton
import com.jrm.databinding.ActivityLanguagesBinding
import com.jrm.onboarding.navigation.BaseNavigator
import com.jrm.utils.AdsHelper
import com.jrm.utils.BaseExtension
import com.jrm.utils.SharedPref
import com.jrm.utils.BaseUtils
import com.jrm.utils.purchase.IAPHelper

class Language2Activity : BaseActivity<ActivityLanguagesBinding>() {
    
    private lateinit var languageData: List<LanguageModel>
    private lateinit var btnSave: TrackableButton

    private lateinit var languageAdapter: LanguageAdapter
    private lateinit var listView: ListView
    private var lastChosenItem: View? = null
    private var savedLangCode: String = ""
    private var position: Int = 0
    private var index: Long = 0
    private var firstOpen = true
    private var loadingHandler: Handler? = null
    private var isAdsShown = false
    private var adsObserver: Observer<Boolean>? = null
    private var scaleXAnimator: ObjectAnimator? = null
    private var scaleYAnimator: ObjectAnimator? = null
    private var isOb1HighLoaded: Boolean = false
    companion object {
        var clickNative = false
        // Shared LiveData for ads preload status communication between activities
        // true = still preloading, false = preload completed (success or failed)
        val adPreloadIsLoading = MutableLiveData<Boolean>(true)
        var isLoadDoneSplash = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(0, 0)
    }
    
    override fun getLayoutActivity(): Int {
        return R.layout.activity_languages
    }
    
    override fun initViews() {
        // Get ads loaded status from previous activity first
        val bundle = intent.extras

        // Reset LiveData for fresh observation (true = still preloading)
        initDefine()
        setupAdsObserver()
        initAction()
    }
    
    private fun initDefine() {
        languageData = LanguageModel.getAllLangData()
        listView = viewBinding.listview
        savedLangCode = SharedPref.readString(BaseConstants.LANG_CODE_STORE, "")
        languageAdapter = LanguageAdapter(this@Language2Activity, languageData, savedLangCode)
        btnSave = viewBinding.btnNext

        
        // Start shake animation to draw user attention
        startShakeAnimation()


        viewBinding.loadingAnim.visibility = View.VISIBLE
        btnSave.visibility = View.GONE
        val loadingDuration = if (Language2Activity.isLoadDoneSplash) 1000L else 2000L

        // Start loading animation timer (independent of ads)
        loadingHandler = Handler(Looper.getMainLooper())
        loadingHandler?.postDelayed({
            completeLoadingAnimation()
        }, loadingDuration)
        
        // If ads were already loaded from L1, show them immediately
        if (Language2Activity.isLoadDoneSplash && !IAPHelper.isPremium()) {
            showAdWhenReady()
        }
    }

    private fun completeLoadingAnimation() {
        viewBinding.loadingAnim.visibility = View.GONE
        btnSave.visibility = View.VISIBLE
    }
    
    /**
     * Find the position of the saved language in the list
     * @return Position of saved language, or 0 if not found
     */
    private fun findSavedLanguagePosition(): Int {
        if (savedLangCode.isEmpty()) return 0
        
        return languageData.indexOfFirst { it.langCode == savedLangCode }.let { index ->
            if (index >= 0) index else 0
        }
    }

    override fun onResumeAfterInter() {
        super.onResumeAfterInter()
    }
    
    /**
     * Scroll to the saved language position smoothly
     */
    private fun scrollToSavedLanguage() {
        val position = findSavedLanguagePosition()
        
        // Post to ensure ListView is fully laid out before scrolling
        listView.post {
            // Smooth scroll to position with offset to center it if possible
            listView.smoothScrollToPositionFromTop(position, listView.height / 3, 300)
        }
    }
    
    private fun setupAdsObserver() {
        // Only observe if ads were not already loaded from L1
        if (!Language2Activity.isLoadDoneSplash) {
            adsObserver = Observer { isStillPreloading ->
                if (!isStillPreloading && !isAdsShown) {
                    // Ads preload completed - show ads immediately
                    showAdWhenReady()
                }
            }
            adPreloadIsLoading.observe(this, adsObserver!!)
        }
    }
    
    private fun startShakeAnimation() {
        // Create a smooth, continuous gentle scale animation to draw attention
        scaleXAnimator = ObjectAnimator.ofFloat(
            btnSave,
            "scaleX",
            1.0f, 1.08f
        ).apply {
            duration = 800 // Duration for one complete cycle
            repeatCount = ValueAnimator.INFINITE // Loop infinitely
            repeatMode = ValueAnimator.REVERSE // Reverse direction on each cycle for smooth motion
            interpolator = AccelerateDecelerateInterpolator() // Smooth acceleration and deceleration
            startDelay = 300 // Slight delay after button appears
        }
        
        // Also animate scaleY to maintain aspect ratio
        scaleYAnimator = ObjectAnimator.ofFloat(
            btnSave,
            "scaleY",
            1.0f, 1.08f
        ).apply {
            duration = 800
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            startDelay = 300
        }
        
        scaleXAnimator?.start()
        scaleYAnimator?.start()
    }
    
    private fun stopShakeAnimation() {
        scaleXAnimator?.cancel()
        scaleYAnimator?.cancel()
        scaleXAnimator = null
        scaleYAnimator = null
        // Reset button scale to normal
        btnSave.scaleX = 1.0f
        btnSave.scaleY = 1.0f
    }
    
    private fun showAdWhenReady() {
        if (isAdsShown || IAPHelper.isPremium()) return
        isAdsShown = true
        showAd()
    }
    
    private fun goToNextActivity() {
        // Restart all activities in the stack
        if (!BaseUtils.isFinishObd()) {
            BaseExtension.showActivity(this, OnboardingActivity::class.java, null)
        } else {
            BaseNavigator.getInstance()?.navigateToHome(this)
        }
        finishAffinity()
    }

    private fun showAd() {
        WaterfallNativeAdManager.show(
            activity = this,
            adView = viewBinding.nativeAd,
            adPlace = BaseConstants.NATIVE_LANGUAGE2,
            waitForLoad = true // true: chờ nếu đang loading, false: fail ngay
        ) { success ->
            if (success) {
            }
        }
//        if (MaxNativePreload.getInstance().getAdStatus(BaseConstants.PRELOAD_NATIVE_202_1) == MaxNativePreload.AdStatus.LOADED) {
//            MaxNativePreload.getInstance().showNative(
//                BaseConstants.PRELOAD_NATIVE_202_1,
//                viewBinding.nativeAd,
//                null)
//            return;
//        }
//
//        if (MaxNativePreload.getInstance().getAdStatus(BaseConstants.PRELOAD_NATIVE_202_2) == MaxNativePreload.AdStatus.LOADED) {
//            MaxNativePreload.getInstance().showNative(
//                BaseConstants.PRELOAD_NATIVE_202_2,
//                viewBinding.nativeAd,
//                null)
//            return;
//        }
//
//        if (AdsNativeMultiPreload.getPreloadState(BaseConstants.PRELOAD_NATIVE_202_1) == AdsNativeMultiPreload.PreloadState.LOADED) {
//            AdsNativeMultiPreload.showPreloadedNativeAd(
//                this,
//                viewBinding.nativeAd,
//                BaseConstants.PRELOAD_NATIVE_202_1,
//                AdsHelper.getLayoutForMetaNativeAd(BaseConstants.PRELOAD_NATIVE_202_1),
//                AdsHelper.getLayoutForMetaNativeAd(BaseConstants.PRELOAD_NATIVE_202_1),
//                null,null
//            )
//            return;
//        }
//
//        if (AdsNativeMultiPreload.getPreloadState(BaseConstants.PRELOAD_NATIVE_202_2) == AdsNativeMultiPreload.PreloadState.LOADED) {
//            AdsNativeMultiPreload.showPreloadedNativeAd(
//                this,
//                viewBinding.nativeAd,
//                BaseConstants.PRELOAD_NATIVE_202_2,
//                AdsHelper.getLayoutForMetaNativeAd(BaseConstants.PRELOAD_NATIVE_202_2),
//                AdsHelper.getLayoutForMetaNativeAd(BaseConstants.PRELOAD_NATIVE_202_2),
//                null,null
//            )
//            return;
//        }
//
//        if (RemoteConfigManager.instance!!.getListAdIsOrder202()[2].equals("Max")) {
//            MaxNew.getInstance().loadNativeAdNew(
//                this,
//                viewBinding.nativeAd,
//                RemoteConfigManager.instance!!.getLfo2MaxAdId(),
//                R.layout.custom_native_admob_large_splash
//            )
//            return
//        }
//        if (BaseConstants.TEST_1_ONBOARDING) {
//            val listAdId: List<AdsNativeMultiPreload.AdIdModel> =
//                RemoteConfigManager.instance?.getListAdIdNativeFromRemote(
//                    BaseConstants.NATIVE_LANGUAGE2,
//                    RemoteConfigManager.instance?.nativeL2Ids ?: ""
//                ) ?: listOf()
//            AdsNativeMultiPreload.preloadMultipleNativeAds(
//                this@Language2Activity,
//                YNMAirBridge.AppData(activityName, BaseConstants.PRELOAD_NATIVE_202_3),
//                listAdId,
//                BaseConstants.PRELOAD_NATIVE_202_3,
//                object : YNMAdsCallbacks() {
//                    override fun onNativeAdLoaded(nativeAd: NativeAd) {
//                        super.onNativeAdLoaded(nativeAd)
//                        viewBinding?.nativeAd?.let { adView ->
//                            AdsNativeMultiPreload.showPreloadedNativeAd(
//                                this@Language2Activity,
//                                adView,
//                                BaseConstants.PRELOAD_NATIVE_202_3,
//                                AdsHelper.getLayoutForMetaNativeAd(BaseConstants.PRELOAD_NATIVE_202_3),
//                                AdsHelper.getLayoutForMetaNativeAd(BaseConstants.PRELOAD_NATIVE_202_3),
//                            )
//                        }
//                    }
//                    override fun onAdClicked() {
//                        super.onAdClicked()
//                        Language2Activity.clickNative = true;
//                    }
//                }
//            )
//        } else {
//            var listAdId: List<AdsNativeMultiPreload.AdIdModel> = listOf(
//                AdsNativeMultiPreload.AdIdModel().apply {
//                    adId = RemoteConfigManager.instance!!.getLfo2HighAdId()
//                    adName = "native_language_2_high"
//                }
//            )
//            if (RemoteConfigManager.instance!!.getListAdIsOrder202()[2].equals("All")) {
//                listAdId = listOf(
//                    AdsNativeMultiPreload.AdIdModel().apply {
//                        adId = RemoteConfigManager.instance!!.getLfo2AdId()
//                        adName = "native_language_2"
//                    }
//                )
//            }
//
//            AdsNativeMultiPreload.preloadMultipleNativeAds(
//                this@Language2Activity,
//                YNMAirBridge.AppData(activityName, BaseConstants.PRELOAD_NATIVE_202_3),
//                listAdId,
//                BaseConstants.PRELOAD_NATIVE_202_3,
//                object : YNMAdsCallbacks() {
//                    override fun onNativeAdLoaded(nativeAd: NativeAd) {
//                        super.onNativeAdLoaded(nativeAd)
//                        viewBinding?.nativeAd?.let { adView ->
//                            AdsNativeMultiPreload.showPreloadedNativeAd(
//                                this@Language2Activity,
//                                adView,
//                                BaseConstants.PRELOAD_NATIVE_202_3,
//                                AdsHelper.getLayoutForMetaNativeAd(BaseConstants.PRELOAD_NATIVE_202_3),
//                                AdsHelper.getLayoutForMetaNativeAd(BaseConstants.PRELOAD_NATIVE_202_3),
//                            )
//                        }
//                    }
//                    override fun onAdClicked() {
//                        super.onAdClicked()
//                        Language2Activity.clickNative = true;
//                    }
//                }
//            )
//        }
    }
    
    private fun initAction() {
        viewBinding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        if (!BaseUtils.isFinishObd()) {
            viewBinding.backBtn.visibility = View.GONE
        }
        
        // Hide native ad view if user has purchased premium
        if (IAPHelper.isPremium()) {
            viewBinding.nativeAd.visibility = View.GONE
        }
        
        btnSave.setOnClickListener {
            // Stop shake animation when user clicks the button
            stopShakeAnimation()
            
            // Show loading popup
            viewBinding.loadingPopupOverlay.visibility = View.VISIBLE
            
            // Preload onboarding ads immediately when showing popup
            if (!BaseUtils.isFinishObd() && !preloadAd) {
                preloadOnboarding1()
                preloadAd = true
            }
            
            // Save language and proceed after 1.5 seconds
            Handler(Looper.getMainLooper()).postDelayed({
                SharedPref.saveString(BaseConstants.LANG_CODE_STORE, savedLangCode)
                
                // Hide loading popup
                viewBinding.loadingPopupOverlay.visibility = View.GONE
                
                // Show interstitial and navigate
                AdsHelper.checkAndShowInterMissing(
                    this@Language2Activity,
                    activityName,
                    BaseConstants.INTER_SPLASH
                ) {
                    goToNextActivity()
                }
            }, 1500L)
        }
        
        listView.adapter = languageAdapter
        listView.setOnItemClickListener { parent, view, position, id ->
            lastChosenItem?.let {
                languageAdapter.setDeActiveButton(it)
            } ?: run {
                languageAdapter.setDeActiveButton(languageAdapter.savedLangItem)
            }
            
            val selectedLang = (view.findViewById<TextView>(R.id.lang_code)).text.toString()
            val selectedLangName = (view.findViewById<TextView>(R.id.lang_name)).text.toString()
            savedLangCode = selectedLang
            languageAdapter.setActiveButton(view)
            lastChosenItem = view
            languageAdapter.setSavedLang(selectedLang)
            
            // Log element click event for language selection
            BaseEventLogger.logElementClick(
                elementId = "language_item_$selectedLang",
                screenName = activityName,
                elementType = "listview_item"
            )
            
            // set texts:
            viewBinding.txtTitle.text = BaseExtension.getLocalizedText(this, selectedLang, R.string.language)
//            viewBinding.tvNext.text = BaseExtension.getLocalizedText(this, selectedLang, R.string.next)
//            viewBinding.txtLoading.text = BaseExtension.getLocalizedText(this, selectedLang, "applying_your_language")
        }
        
        // Check if there's a scroll position from previous activity (from bundle)
        val bundle = intent.extras
        val hasScrollPos = bundle?.containsKey("scrollPos") == true
        
        if (hasScrollPos) {
            // Restore scroll position from previous activity
            listView.setSelection(bundle!!.getInt("scrollPos", 0))
        } else {
            // Auto-scroll to saved language position
            scrollToSavedLanguage()
        }
    }

    private fun reShowNativeLanguageAds() {
        WaterfallNativeAdManager.preload(
            context = this,
            activityName = activityName,
            adPlace = BaseConstants.NATIVE_LANGUAGE2,
            configString = RemoteConfigManager.instance!!.nativeL2Ids,
            layoutAdmob = R.layout.custom_native_admob_large_language,
            layoutMax = R.layout.custom_native_admob_large_language_max
        ) { result ->
            if (result.success) {
                WaterfallNativeAdManager.show(
                    activity = this,
                    adView = viewBinding.nativeAd,
                    adPlace = BaseConstants.NATIVE_LANGUAGE2,
                    waitForLoad = true // true: chờ nếu đang loading, false: fail ngay
                ) { success ->
                    if (success) {
                    }
                }
            } else {
            }
        }
//        val listAdId: List<AdsNativeMultiPreload.AdIdModel> =
//            RemoteConfigManager.instance?.getListAdIdNativeFromRemote(
//                BaseConstants.NATIVE_LANGUAGE2,
//                RemoteConfigManager.instance?.nativeL2Ids ?: ""
//            ) ?: listOf()
//
//        AdsNativeMultiPreload.preloadMultipleNativeAds(
//            this@Language2Activity,
//            YNMAirBridge.AppData(activityName, BaseConstants.NATIVE_LANGUAGE2),
//            listAdId,
//            BaseConstants.NATIVE_LANGUAGE2,
//            object : YNMAdsCallbacks() {
//                override fun onNativeAdLoaded(nativeAd: NativeAd) {
//                    super.onNativeAdLoaded(nativeAd)
//                    // Show the native ad in the native ad view if available
//                    viewBinding?.nativeAd?.let { adView ->
//                        AdsNativeMultiPreload.showPreloadedNativeAd(
//                            this@Language2Activity,
//                            adView,
//                            BaseConstants.NATIVE_LANGUAGE2,
//                            AdsHelper.getLayoutForMetaNativeAd(BaseConstants.NATIVE_LANGUAGE2),
//                            AdsHelper.getLayoutForMetaNativeAd(BaseConstants.NATIVE_LANGUAGE2),
//                        )
//                    }
//                }
//
//                override fun onAdClicked() {
//                    super.onAdClicked()
//                    clickNative = true;
//                }
//            }
//        )
    }
    var preloadAd = false
    override fun onResume() {
        super.onResume()
        if (!AppOpenManager.getInstance().isInterstitialShowing()) {
            if (!firstOpen) {
                if (clickNative) {
                    goToNextActivity()
                } else {
                    reShowNativeLanguageAds()
                }
            };
            firstOpen = false
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Clean up handlers to prevent memory leaks
        loadingHandler?.removeCallbacksAndMessages(null)
        loadingHandler = null
        
        // Stop and clean up shake animation
        stopShakeAnimation()
        
        // Remove observer from companion object LiveData to prevent memory leaks
        adsObserver?.let { observer ->
            adPreloadIsLoading.removeObserver(observer)
        }
        adsObserver = null
    }
    
    private fun preloadOnboarding1() {
        if (!BaseUtils.isFinishObd()) {
            WaterfallNativeAdManager.preload(
                context = this,
                activityName = activityName,
                adPlace = BaseConstants.NATIVE_ONBOARD_1,
                configString = RemoteConfigManager.instance!!.nativeObd1Ids,
                layoutAdmob = R.layout.custom_native_admob_large,
                layoutMax = R.layout.custom_native_admob_large_max
            ) { result ->
                if (result.success) {
                    isOb1HighLoaded = true
                } else {
                }
            }
//            val listAdId: List<AdsNativeMultiPreload.AdIdModel> =
//                RemoteConfigManager.instance?.getListAdIdNativeFromRemote(
//                    BaseConstants.NATIVE_ONBOARD_1,
//                    RemoteConfigManager.instance?.nativeObd1Ids ?: ""
//                ) ?: listOf()
//
//            AdsNativeMultiPreload.preloadMultipleNativeAds(
//                this@Language2Activity,
//                YNMAirBridge.AppData(activityName, BaseConstants.NATIVE_ONBOARD_1),
//                listAdId,
//                BaseConstants.NATIVE_ONBOARD_1,
//                object : YNMAdsCallbacks() {
//                    override fun onNativeAdLoaded(nativeAd: NativeAd) {
//                        super.onNativeAdLoaded(nativeAd)
//                        isOb1HighLoaded = true
//                    }
//                    override fun onAdClicked() {
//                        super.onAdClicked()
//                        OnboardingFragmentNew.clickNative = true;
//                    }
//                }
//            )
        }
    }
}
