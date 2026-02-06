package com.jrm.onboarding.language

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.jrm.utils.Logger
import android.widget.ListView
import android.widget.TextView
import com.ads.nomyek_admob.admobs.AppOpenManager
import com.jrm.R
import com.jrm.service.WaterfallAdHelper
import com.jrm.base.BaseActivity
import com.jrm.base.BaseEventLogger
import com.jrm.base.tracking.TrackableButton
import com.jrm.databinding.ActivityLanguagesBinding
import com.jrm.onboarding.navigation.BaseNavigator
import com.jrm.onboarding.onboarding.OnboardingActivity
import com.jrm.utils.BaseConstants
import com.jrm.utils.BaseExtension
import com.jrm.utils.BaseUtils
import com.jrm.utils.SharedPref
import com.jrm.utils.purchase.IAPHelper
import com.jrm.utils.remote_config.RemoteConfigManager

class LanguageActivity : BaseActivity<ActivityLanguagesBinding>() {
    companion object {
        private const val TAG = "LanguageActivity"
    }
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
            if (RemoteConfigManager.instance?.adConfig?.screenObd?.language?.isTwoScreen == true) {
                BaseExtension.showActivity(this, Language2Activity::class.java, null)
            } else if(RemoteConfigManager.instance?.adConfig?.screenObd?.onboarding?.enable == true) {
                BaseExtension.showActivity(this, OnboardingActivity::class.java, null)
            }else{
                BaseNavigator.getInstance().navigateToHome(this)
            }
        } else {
            BaseNavigator.getInstance().navigateToHome(this)
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
//

            // Show L1 ad that was preloaded in SplashActivity
            WaterfallAdHelper.loadAd(
                activity = this,
                activityName =activityName,
                adView = viewBinding.nativeAd,
                placementName = "language_1_ad_view",
                waitForLoad = true,
                onSuccess = {
                    Logger.d( "✅ L1 native ad shown")
                },
                onFailure = {
                    Logger.w( "❌ L1 native ad not available")
                    viewBinding.nativeAd.visibility = View.GONE
                }
            )

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

        if (BaseUtils.isFinishObd() && savedLangCode.isNotEmpty()) {
            scrollToSavedLanguage()
        }
    }


    override fun onResumeAfterInter() {
        super.onResumeAfterInter()
        if (!BaseUtils.isFinishObd()) preloadL2NativeAds();
    }

    private fun preloadL2NativeAds() {
        WaterfallAdHelper.loadAd(
            activity = this,
            activityName = activityName,
            placementName = "language_2_ad_view",
            onSuccess = {
                Logger.d( "✅ L2 native ad preloaded")
                Language2Activity.adPreloadIsLoading.postValue(false)
                Language2Activity.isLoadDoneSplash = true
            },
            onFailure = {
                Language2Activity.adPreloadIsLoading.postValue(false)
                Language2Activity.isLoadDoneSplash = true
            }
        )

    }


    private fun reShowNativeLanguageAds() {
        // Preload and show L1 ad when coming back
        loadAds(
            placementId = "language_1_ad_view",
            adView = viewBinding.nativeAd,
            onSuccess = {
                Logger.d( "✅ L1 native ad re-shown")
            },
            onFailure = {
                Logger.w( "❌ L1 native ad failed to re-show")
                viewBinding.nativeAd.visibility = View.GONE
            }
        )
    }

    override fun onResume() {
        super.onResume()
        if (!AppOpenManager.getInstance().isInterstitialShowing) {
            if (!firstOpen) reShowNativeLanguageAds();
            firstOpen = false
        }
    }
}
