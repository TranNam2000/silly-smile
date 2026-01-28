package com.jrm.onboarding.onboarding

import android.util.Log
import android.view.View
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import androidx.viewpager2.widget.ViewPager2
import com.ads.nomyek_admob.ads_components.YNMAds
import com.ads.nomyek_admob.ads_components.YNMAdsCallbacks
import com.ads.nomyek_admob.ads_components.wrappers.AdsError
import com.ads.nomyek_admob.event.YNMAirBridge
import com.ads.nomyek_admob.event.YNMAirBridgeDefaultEvent
import com.ads.nomyek_admob.utils.AdsInterMultiPreload
import com.jrm.utils.remote_config.RemoteConfigManager
import com.jrm.utils.BaseConstants
import com.jrm.base.ViewPagerAddFragmentsAdapter
import com.jrm.base.BaseActivity
import com.jrm.R
import com.jrm.ads.WaterfallNativeAdManager
import com.jrm.databinding.ActivityOnboardingScreenBinding
import com.jrm.onboarding.navigation.BaseNavigator
import com.jrm.utils.BaseUtils

class OnboardingActivity : BaseActivity<ActivityOnboardingScreenBinding>() {
    var preload0b4 : Boolean = false
    var preloadOb5 : Boolean = false
    var preloadOb6 : Boolean = false
    var preloadOb3 : Boolean = false
    var preloadOb2 : Boolean = false
    // MutableLiveData to observe loadingOb3 state changes
    val loadingOb3LiveData = MutableLiveData<Boolean>().apply { value = true }
    override fun getLayoutActivity(): Int {
        return R.layout.activity_onboarding_screen
    }

    override fun initViews() {
        initViewPager()
    }

    public fun isFiveObd() : Boolean {
        return RemoteConfigManager.instance!!.numberScreenObd.toInt() == 5;
    }

    fun preloadOnboarding2() {
        preloadOb2 = true

        WaterfallNativeAdManager.preload(
            context = this,
            activityName = activityName,
            adPlace = BaseConstants.NATIVE_ONBOARD_2,
            configString = RemoteConfigManager.instance!!.nativeObd2Ids,
            layoutAdmob = R.layout.custom_native_admob_large,
            layoutMax = R.layout.custom_native_admob_large_max
        )
    }

    public fun preloadOnboarding3() {
        preloadOb3 = true
        if (!BaseUtils.isFinishObd()) {
            WaterfallNativeAdManager.preload(
                context = this,
                activityName = activityName,
                adPlace = BaseConstants.NATIVE_ONBOARD_3,
                configString = RemoteConfigManager.instance!!.nativeObd3Ids,
                layoutAdmob = R.layout.custom_full_screen_native_ads,
                layoutMax = R.layout.custom_full_screen_native_ads_max
            ) { result ->
                loadingOb3LiveData.postValue(false)
            }
        }
    }

    private fun preloadOnboarding4() {
        preload0b4 = true;
        WaterfallNativeAdManager.preload(
            context = this,
            activityName = activityName,
            adPlace = BaseConstants.NATIVE_ONBOARD_4,
            configString = RemoteConfigManager.instance!!.nativeObd4Ids,
            layoutAdmob = R.layout.custom_native_admob_large,
            layoutMax = R.layout.custom_native_admob_large_max
        )
    }


    private fun preloadOnboarding6() {
        preloadOb6 = true;
        if (!BaseUtils.isFinishObd()) {
            val listAdId: List<AdsInterMultiPreload.AdIdModel> = RemoteConfigManager.instance!!.getListAdIdInterFromRemote(BaseConstants.INTER_ONBOARD_6,
                RemoteConfigManager.instance!!.intersObd6Ids)
            AdsInterMultiPreload.preloadMultipleInterAds(
                this@OnboardingActivity,
                YNMAirBridge.AppData(activityName, "inter_odb6"),
                listAdId,
                BaseConstants.INTER_ONBOARD_6,
                object : YNMAdsCallbacks(
                    YNMAirBridge.AppData(
                        activityName,
                        BaseConstants.INTER_ONBOARD_6
                    ), YNMAds.INTERSTITIAL
                ) {
                }
            )
        }
    }
    private fun initViewPager() {
        val adapter = ViewPagerAddFragmentsAdapter(supportFragmentManager, lifecycle)
        var index: Int = 0;
        adapter.addFrag( OnboardingFragment(index++, R.drawable.obd1, R.string.obd_title1, R.string.obd_detail1))
        if (isFiveObd()) {
            adapter.addFrag(OnboardingFragment(index++, R.drawable.obd1, R.string.obd_title1, R.string.obd_title3))
        }
        adapter.addFrag(OnboardingFragment(index++, R.drawable.obd2, R.string.obd_title2, R.string.obd_detail2))
        adapter.addFrag(OnboardingFragment(index++, R.drawable.obd1, R.string.obd_title1, R.string.obd_title3))
        adapter.addFrag(OnboardingFragment(index++, R.drawable.obd3, R.string.obd_title3, R.string.obd_detail3))
        viewBinding.viewpagerOnboard.isUserInputEnabled = !RemoteConfigManager.instance!!.boostFNativeObd
        viewBinding.viewpagerOnboard.adapter = adapter
        if (isFiveObd()) {
            viewBinding.indicatorView.count = 5
            viewBinding.indicatorView2.count = 5
        }

        viewBinding.viewpagerOnboard.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                viewBinding.indicatorView.selection = position
                viewBinding.indicatorView2.selection = position
                showIndicatorView(position)
                Log.d("Onboarding", "onPageSelected: $position")
                when (position) {
                    0 -> {
                        YNMAirBridgeDefaultEvent.pushEventScreenView(YNMAirBridge.AppData("Ob1", ""))
                        if (!preloadOb3) {
                            preloadOnboarding3()
                        }
                        if (!preloadOb2) {
                            preloadOnboarding2()
                        }
                    }
                    1 -> {
                        YNMAirBridgeDefaultEvent.pushEventScreenView(YNMAirBridge.AppData("Ob2", ""))
                        if (!preload0b4 && !isFiveObd()) {
                            preloadOnboarding4()
                        }
                    }
                    2 -> {
                        YNMAirBridgeDefaultEvent.pushEventScreenView(YNMAirBridge.AppData("Ob3", ""))
                        if (!preload0b4 && isFiveObd()) {
                            preloadOnboarding4()
                        }
                    }
                    3 -> {
                        YNMAirBridgeDefaultEvent.pushEventScreenView(YNMAirBridge.AppData("Ob4", ""))
                        preloadOnboarding6();
                    }
                    4 -> {
                        YNMAirBridgeDefaultEvent.pushEventScreenView(YNMAirBridge.AppData("Ob5", ""))
                        if (isFiveObd() && !preloadOb6 && RemoteConfigManager.instance!!.preloadInterFinishObdIndex.toInt() == 4) {
                            preloadOnboarding6();
                        }
                    }
                }
            }
        })

        viewBinding.tvNext.setOnClickListener {
            onClickNext()
        }
        viewBinding.tvNext2.setOnClickListener {
            onClickNext()
        }
        viewBinding.btnContinue.setOnClickListener {
            onClickNext()
        }
    }

    public fun onClickNext() {
        if (viewBinding.viewpagerOnboard.currentItem == viewBinding.viewpagerOnboard.adapter!!.itemCount - 1) {
            goToNextActivity()
        } else {
            viewBinding.viewpagerOnboard.currentItem = viewBinding.viewpagerOnboard.currentItem + 1
        }
    }

    private fun goToNextActivity() {
        // Show preloaded waterfall ad
        AdsInterMultiPreload.showPreloadedInterAdWithLoading(
            this@OnboardingActivity,
            BaseConstants.INTER_ONBOARD_6,
            10000,
            object : YNMAdsCallbacks(YNMAirBridge.AppData(activityName, BaseConstants.INTER_ONBOARD_6), YNMAds.INTERSTITIAL) {
                override fun onNextAction(isShown: Boolean) {
                    super.onNextAction(isShown)
                    goHome()
                }

                override fun onAdFailedToLoad(adError: AdsError?) {
                    super.onAdFailedToLoad(adError)
                    goHome()
                }
            }
        )
    }
    private fun goHome() {
        YNMAds.getInstance().adConfig.setInterFlow(RemoteConfigManager.instance!!.getStartIndexInter(), RemoteConfigManager.instance!!.getDeltaIndexInter())
        YNMAds.isGoHome = true;
        BaseNavigator.getInstance()?.navigateToHome(this)
        finish()
    }

    public fun getListPosNativeFull() : List<Int> {
        if (isFiveObd()) {
            return listOf(1,3);
        }
        return listOf(2)
    }
    public fun getListPosNativeNormal() : List<Int> {
        if (isFiveObd()) {
            return listOf(0,4);
        }
        return listOf(0,3)
    }
    public fun getListNoNative() : List<Int> {
        return listOf()
        if (isFiveObd()) {
            return listOf(2)
        }
        return listOf(1)
    }
    public fun getLastPos(): Int {
        if (isFiveObd()) {
            return 4
        }
        return 3
    }


    var isFullNativeShow: Boolean = false

    fun showIndicatorView(pos: Int){
        viewBinding.tvNext2.visibility = View.GONE
//        viewBinding.tvNext.setTextColor(if (pos == getLastPos())
//            ContextCompat.getColor(this, R.color.obd_theme_color)
//            else ContextCompat.getColor(this, com.ads.nomyek_admob.R.color.textColor))
        viewBinding.tvNext.text = (if (pos == getLastPos()) resources.getString(R.string.start) else resources.getString(R.string.next))
        viewBinding.tvNext.requestLayout()
        if (pos in getListPosNativeNormal()) {
            viewBinding.indicatorView.visibility = View.VISIBLE
            viewBinding.tvNext.visibility = View.GONE
//            viewBinding.btnNext.visibility = View.VISIBLE
            viewBinding.indicatorView2.visibility = View.INVISIBLE
            viewBinding.layoutContinue2.visibility = View.VISIBLE
        }
        if (pos in getListNoNative()) {
            viewBinding.indicatorView.visibility = View.VISIBLE
            viewBinding.tvNext.visibility = View.VISIBLE
//            viewBinding.btnNext.visibility = View.VISIBLE
            viewBinding.indicatorView2.visibility = View.INVISIBLE
            viewBinding.layoutContinue2.visibility = View.INVISIBLE
            viewBinding.btnContinue.visibility = View.GONE

        }
        if (pos in getListPosNativeFull()) {
            if (!RemoteConfigManager.instance!!.boostFNativeObd) {
                viewBinding.loadingAnim.visibility = View.GONE
                viewBinding.tvNext2.visibility = View.VISIBLE
            } else {
                if (!isFullNativeShow && RemoteConfigManager.instance!!.boostFNativeObd) {
                    val loadingDuration = 3000L
//                    viewBinding.loadingAnim.visibility = View.VISIBLE
                    viewBinding.tvNext2.visibility = View.GONE
                    // Start loading animation timer (independent of ads)
                    var loadingHandler = Handler(Looper.getMainLooper())
                    loadingHandler?.postDelayed({
                        viewBinding.loadingAnim.visibility = View.GONE
                        viewBinding.tvNext2.visibility = View.VISIBLE
                    }, loadingDuration)
                    isFullNativeShow = true
                }
            }
            viewBinding.indicatorView.visibility = View.INVISIBLE
            viewBinding.tvNext.visibility = View.INVISIBLE
            viewBinding.indicatorView2.visibility = View.INVISIBLE
            viewBinding.layoutContinue2.visibility = View.INVISIBLE
        }
    }

}
