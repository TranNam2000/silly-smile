package com.jrm.onboarding.language

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ListView
import android.widget.TextView
import com.ads.nomyek_admob.admobs.AppOpenManager
import com.jrm.onboarding.onboarding.OnboardingActivity
import com.jrm.utils.SharedPref
import com.jrm.base.BaseActivity
import com.jrm.utils.remote_config.RemoteConfigManager
import com.jrm.R
import com.jrm.ads.WaterfallNativeAdManager
import com.jrm.base.BaseEventLogger
import com.jrm.base.tracking.TrackableButton
import com.jrm.databinding.ActivityLanguagesBinding
import com.jrm.onboarding.navigation.BaseNavigator
import com.jrm.utils.BaseConstants
import com.jrm.utils.BaseExtension
import com.jrm.utils.BaseUtils
import com.jrm.utils.purchase.IAPHelper

class LanguageActivity : BaseActivity<ActivityLanguagesBinding>() {
    private lateinit var languageData: List<LanguageModel>
    private lateinit var btnSave: TrackableButton
    private lateinit var languageAdapter: LanguageAdapter
    private lateinit var listView: ListView
    private var lastChosenItem: View? = null
    private var savedLangCode: String = ""
    private var firstOpen = true
    private var isLoaded202_2 = false;
    
    override fun getLayoutActivity(): Int {
        return R.layout.activity_languages
    }
    
    override fun initViews() {
        initDefine()
        initAction()
    }
    
    private fun initDefine() {
        languageData = LanguageModel.getAllLangData()
        listView = viewBinding.listview
        savedLangCode = if (!BaseUtils.isFinishObd()) "" else SharedPref.readString(BaseConstants.LANG_CODE_STORE, "")
        languageAdapter = LanguageAdapter(this@LanguageActivity, languageData, savedLangCode)
        btnSave = viewBinding.btnNext
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
    
    private fun goToNextActivity() {
        // Restart all activities in the stack
        if (!BaseUtils.isFinishObd()) {
            BaseExtension.showActivity(this, OnboardingActivity::class.java, null)
        } else {
            BaseNavigator.getInstance()?.navigateToHome(this)
        }
        finishAffinity()
    }
    
    private fun initAction() {
        viewBinding.backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        
        if (!BaseUtils.isFinishObd()) {
            viewBinding.backBtn.visibility = View.GONE
        }
        
        if (!IAPHelper.isPremium()) {
//            AdsNativeMultiPreload.showPreloadedNativeAd(
//                this,
//                viewBinding.nativeAd,
//                BaseConstants.NATIVE_LANGUAGE1,
//                AdsHelper.getLayoutForMetaNativeAd(BaseConstants.NATIVE_LANGUAGE1),
//                AdsHelper.getLayoutForMetaNativeAd(BaseConstants.NATIVE_LANGUAGE1),
//                null,
//                null
//            )

            WaterfallNativeAdManager.show(
                activity = this,
                adView = viewBinding.nativeAd,
                adPlace = BaseConstants.NATIVE_LANGUAGE1,
                waitForLoad = true // true: chờ nếu đang loading, false: fail ngay
            ) { success ->
                if (success) {
                }
            }

        } else {
            viewBinding.nativeAd.visibility = View.GONE
        }

        btnSave.visibility = if (savedLangCode == "") View.GONE else View.VISIBLE
        
        btnSave.setOnClickListener {
            goToNextActivity()
            SharedPref.saveString(BaseConstants.LANG_CODE_STORE, savedLangCode)
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
            SharedPref.saveString(BaseConstants.LANG_CODE_STORE, savedLangCode)
            
            // Log element click event for language selection
            BaseEventLogger.logElementClick(
                elementId = "language_item_$selectedLang",
                screenName = activityName,
                elementType = "listview_item"
            )

            // Hide loading popup
            val intent = Intent(this, Language2Activity::class.java)
            val bundle = Bundle()
            bundle.putInt("scrollPos", listView.firstVisiblePosition)
            bundle.putBoolean("isLoaded202_2", isLoaded202_2)
            intent.putExtras(bundle)
            startActivity(intent)
            overridePendingTransition(0, 0)
//            viewBinding.loadingPopupOverlay.visibility = View.VISIBLE
//            Handler(Looper.getMainLooper()).postDelayed({
//
//                viewBinding.loadingPopupOverlay.visibility = View.GONE
//            }, 500L)
        }
        
        // Auto-scroll to saved language position if coming from settings
        // (savedLangCode is not empty when coming from settings)
        if (BaseUtils.isFinishObd() && savedLangCode.isNotEmpty()) {
            scrollToSavedLanguage()
        }
    }

    override fun onResumeAfterInter() {
        super.onResumeAfterInter()
        if (!BaseUtils.isFinishObd()) preloadL2NativeAds();
    }

    private fun preloadL2NativeAds() {
        WaterfallNativeAdManager.preload(
            context = this,
            activityName = activityName,
            adPlace = BaseConstants.NATIVE_LANGUAGE2,
            configString = RemoteConfigManager.instance!!.nativeL2Ids,
            layoutAdmob = R.layout.custom_native_admob_large_language,
            layoutMax = R.layout.custom_native_admob_large_language_max
        ) { result ->
            if (result.success) {
                // Ad loaded - result.adType = "max_native" hoặc "admob_native"
                Log.d("SplashActivity", "L1 Native ad loaded successfully")
                Language2Activity.adPreloadIsLoading.postValue(false)
                Language2Activity.isLoadDoneSplash = true;
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
//            this@LanguageActivity,
//            YNMAirBridge.AppData(activityName, BaseConstants.NATIVE_LANGUAGE2),
//            listAdId,
//            BaseConstants.PRELOAD_NATIVE_202_1,
//            object : YNMAdsCallbacks() {
//                override fun onAdClicked() {
//                    super.onAdClicked()
//                    Language2Activity.clickNative = true;
//                }
//
//                override fun onNativeAdLoaded(nativeAd: NativeAd) {
//                    super.onNativeAdLoaded(nativeAd)
//                    Language2Activity.adPreloadIsLoading.postValue(false)
//                    Language2Activity.isLoadDoneSplash = true;
//                }
//            }
//        )
//        var preloaded = MaxNativePreload.getInstance().getAdStatus(BaseConstants.PRELOAD_NATIVE_202_1) == MaxNativePreload.AdStatus.LOADED || AdsNativeMultiPreload.getPreloadState(BaseConstants.PRELOAD_NATIVE_202_1) == AdsNativeMultiPreload.PreloadState.LOADED;
//        if (RemoteConfigManager.instance!!.getListAdIsOrder202().isNotEmpty() && !preloaded) {
//            if (RemoteConfigManager.instance!!.getListAdIsOrder202()[1].equals("Max")) {
//                MaxNativePreload.getInstance().preloadNative(
//                    this,
//                    RemoteConfigManager.instance!!.getLfo2MaxAdId(),
//                    BaseConstants.PRELOAD_NATIVE_202_2,
//                    R.layout.custom_native_admob_large,
//                    object : AppLovinCallback() {
//                        override fun onAdClicked() {
//                            super.onAdClicked()
//                            Language2Activity.clickNative = true;
//                        }
//
//                        override fun onAdFailedToLoad(i: MaxError?) {
//                            super.onAdFailedToLoad(i)
//                            isLoaded202_2 = true;
//                            // Notify Language2Activity that ads preload is complete (preloading = false)
//                            Language2Activity.adPreloadIsLoading.postValue(false)
//                        }
//
//                        override fun onAdLoaded() {
//                            super.onAdLoaded()
//                            isLoaded202_2 = true;
//                            // Notify Language2Activity that ads preload is complete (preloading = false)
//                            Language2Activity.adPreloadIsLoading.postValue(false)
//                        }
//                    })
//            }
//            else {
//                var listAdId: List<AdsNativeMultiPreload.AdIdModel> = listOf(
//                    AdsNativeMultiPreload.AdIdModel().apply {
//                        adId = RemoteConfigManager.instance!!.getLfo2HighAdId()
//                        adName = "native_language_2_high"
//                    }
//                )
//                if (RemoteConfigManager.instance!!.getListAdIsOrder202()[1].equals("All")) {
//                    listAdId = listOf(
//                        AdsNativeMultiPreload.AdIdModel().apply {
//                            adId = RemoteConfigManager.instance!!.getLfo2AdId()
//                            adName = "native_language_2"
//                        }
//                    )
//                }
//
//                AdsNativeMultiPreload.preloadMultipleNativeAds(
//                    this@LanguageActivity,
//                    YNMAirBridge.AppData(activityName, BaseConstants.NATIVE_LANGUAGE2),
//                    listAdId,
//                    BaseConstants.PRELOAD_NATIVE_202_2,
//                    object : YNMAdsCallbacks() {
//                        override fun onAdClicked() {
//                            super.onAdClicked()
//                            Language2Activity.clickNative = true;
//                        }
//
//                        override fun onNativeAdLoaded(nativeAd: NativeAd) {
//                            super.onNativeAdLoaded(nativeAd)
//                            isLoaded202_2 = true;
//                            // Notify Language2Activity that ads preload is complete (preloading = false)
//                            Language2Activity.adPreloadIsLoading.postValue(false)
//                        }
//
//                        override fun onAdFailedToLoad(adError: AdsError?) {
//                            super.onAdFailedToLoad(adError)
//                            isLoaded202_2 = true;
//                            // Notify Language2Activity that ads preload is complete (preloading = false)
//                            Language2Activity.adPreloadIsLoading.postValue(false)
//                        }
//                    }
//                )
//            }
//        }
    }


    private fun reShowNativeLanguageAds() {
        WaterfallNativeAdManager.preload(
            context = this,
            activityName = activityName,
            adPlace = BaseConstants.NATIVE_LANGUAGE1,
            configString = RemoteConfigManager.instance!!.nativeL1Ids,
            layoutAdmob = R.layout.custom_native_admob_large_language,
            layoutMax = R.layout.custom_native_admob_large_language_max
        ) { result ->
            if (result.success) {
                WaterfallNativeAdManager.show(
                    activity = this,
                    adView = viewBinding.nativeAd,
                    adPlace = BaseConstants.NATIVE_LANGUAGE1,
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
//                BaseConstants.NATIVE_LANGUAGE1,
//                RemoteConfigManager.instance?.nativeL1Ids ?: ""
//            ) ?: listOf()
//
//        AdsNativeMultiPreload.preloadMultipleNativeAds(
//            this@LanguageActivity,
//            YNMAirBridge.AppData(activityName, BaseConstants.NATIVE_LANGUAGE1),
//            listAdId,
//            BaseConstants.NATIVE_LANGUAGE1,
//            object : YNMAdsCallbacks() {
//                override fun onNativeAdLoaded(nativeAd: NativeAd) {
//                    super.onNativeAdLoaded(nativeAd)
//                    // Show the native ad in the native ad view if available
//                    viewBinding?.nativeAd?.let { adView ->
//                        AdsNativeMultiPreload.showPreloadedNativeAd(
//                            this@LanguageActivity,
//                            adView,
//                            BaseConstants.NATIVE_LANGUAGE1,
//                            AdsHelper.getLayoutForMetaNativeAd(BaseConstants.NATIVE_LANGUAGE1),
//                            AdsHelper.getLayoutForMetaNativeAd(BaseConstants.NATIVE_LANGUAGE1),
//                        )
//                    }
//                }
//            }
//        )
    }

    override fun onResume() {
        super.onResume()
        if (!AppOpenManager.getInstance().isInterstitialShowing()) {
            if (!firstOpen) reShowNativeLanguageAds();
            firstOpen = false
        }
    }
}
