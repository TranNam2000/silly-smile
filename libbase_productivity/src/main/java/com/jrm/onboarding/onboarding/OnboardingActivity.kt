package com.jrm.onboarding.onboarding

import android.os.Handler
import android.os.Looper
import android.view.View
import com.jrm.utils.Logger
import androidx.lifecycle.MutableLiveData
import androidx.viewpager2.widget.ViewPager2
import com.ads.nomyek_admob.ads_components.YNMAds
import com.ads.nomyek_admob.event.YNMAirBridge
import com.ads.nomyek_admob.event.YNMAirBridgeDefaultEvent
import com.jrm.R
import com.jrm.base.BaseActivity
import com.jrm.base.ViewPagerAddFragmentsAdapter
import com.jrm.databinding.ActivityOnboardingScreenBinding
import com.jrm.model.DataPage
import com.jrm.onboarding.navigation.BaseNavigator
import com.jrm.utils.BaseConstants
import com.jrm.utils.BaseUtils
import com.jrm.utils.remote_config.RemoteConfigManager

class OnboardingActivity : BaseActivity<ActivityOnboardingScreenBinding>() {
    var preload0b4 : Boolean = false
    var preloadOb5 : Boolean = false
    var preloadOb6 : Boolean = false
    var preloadOb3 : Boolean = false
    var preloadOb2 : Boolean = false
    // MutableLiveData to observe loadingOb3 state changes
    val loadingOb3LiveData = MutableLiveData<Boolean>().apply { value = true }
    // Map to store ad type for each position (position -> ad type)
    private val positionAdTypeMap = mutableMapOf<Int, String>()
    override fun getLayoutActivity(): Int {
        return R.layout.activity_onboarding_screen
    }

    override fun initViews() {
        try {
            initViewPager()
        } catch (e: Exception) {
            Logger.e("OnboardingActivity - Error in initViewPager(): ${e.message}")
            e.printStackTrace()
        }
    }

    private fun getTotalPages(): Int {
        return RemoteConfigManager.instance?.adConfig?.screenObd?.onboarding?.numberScreen 
            ?: RemoteConfigManager.instance?.numberScreenObd 
            ?: 4
    }

    private fun getOnboardingPlacementId(index: Int): String {
        return when (index) {
            1 -> BaseConstants.PLACEMENT_ONBOARDING_1
            2 -> BaseConstants.PLACEMENT_ONBOARDING_2
            3 -> BaseConstants.PLACEMENT_ONBOARDING_3
            4 -> BaseConstants.PLACEMENT_ONBOARDING_4
            5 -> BaseConstants.PLACEMENT_ONBOARDING_5
            else -> "onboarding_$index"
        }
    }

    public fun isFiveObd() : Boolean {
        return getTotalPages() == 5;
    }

    fun preloadOnboarding2() {
        preloadOb2 = true

        preloadAds(
            placementId = BaseConstants.PLACEMENT_ONBOARDING_2,
        )
    }

    public fun preloadOnboarding3() {
        preloadOb3 = true
        if (!BaseUtils.isFinishObd()) {
            preloadAds(
                placementId = BaseConstants.PLACEMENT_ONBOARDING_3,
                onSuccess = {
                    loadingOb3LiveData.postValue(false)
                },
                onFailure = {
                    loadingOb3LiveData.postValue(false)

                })
        }
    }

    private fun preloadOnboarding4() {
        preload0b4 = true;

        preloadAds(
            placementId = BaseConstants.PLACEMENT_ONBOARDING_4,
        )
    }


    private fun preloadOnboarding6() {
        preloadOb6 = true;
        if (!BaseUtils.isFinishObd()) {
            preloadAds(placementId = BaseConstants.PLACEMENT_ONBOARDING_INTER)
        }
    }
    private fun initViewPager() {
        val adapter = ViewPagerAddFragmentsAdapter(supportFragmentManager, lifecycle)
        val config = RemoteConfigManager.instance?.adConfig
        val totalPages = getTotalPages()
        
        // Clear previous mapping
        positionAdTypeMap.clear()
        
        // Generate placement IDs based on totalPages from config
        val listAdsPlacement = (1..totalPages).map { getOnboardingPlacementId(it) }
        
        var fragmentPosition = 0
        var dataPageIndex = 0
        
        // Count total fragments to determine last position
        val totalFragments = listAdsPlacement.count { config?.adPlacements?.get(it) != null }
        val lastFragmentPosition = totalFragments - 1
        
        listAdsPlacement.forEach { placementId ->
            val placement = config?.adPlacements?.get(placementId)
            if (placement != null) {
                val adType = placement.type
                
                // Store ad type for this fragment position
                positionAdTypeMap[fragmentPosition] = adType
                
                // Check ad type to determine if data page is needed
                val isNativeAd = adType == "native_view" || adType == "native_fsn"
                
                // All screens (both native and non-native) can have data pages
                val dataPage: DataPage? = if (listDataPage != null && listDataPage.isNotEmpty()) {
                    if (fragmentPosition == lastFragmentPosition) {
                        // Last screen always uses the last data page (even if data is less than total screens)
                        listDataPage.last()
                    } else {
                        // Priority: use data in order first, then cycle if not enough
                        // If dataPageIndex < listDataPage.size: use data[dataPageIndex]
                        // If dataPageIndex >= listDataPage.size: cycle from beginning
                        val selectedIndex = if (dataPageIndex < listDataPage.size) {
                            // Use data in order (priority)
                            dataPageIndex
                        } else {
                            // Cycle from beginning if not enough data
                            val cycledIndex = (dataPageIndex - listDataPage.size) % listDataPage.size
                            cycledIndex
                        }
                        listDataPage[selectedIndex]
                    }
                } else {
                    null
                }
                
                if (isNativeAd) {
                    // For native ads (native_view, native_fsn), add fragment with or without data page
                    adapter.addFrag(
                        OnboardingFragment(
                            fragmentPosition,
                            dataPage?.image ?: 0,
                            dataPage?.title ?: 0,
                            dataPage?.detail ?: 0
                        )
                    )
                } else {
                    // For non-native ads, add fragment with data page
                    adapter.addFrag(
                        OnboardingFragment(
                            fragmentPosition,
                            dataPage?.image ?: 0,
                            dataPage?.title ?: 0,
                            dataPage?.detail ?: 0
                        )
                    )
                }
                
                // Increment data page index for all screens (both native and non-native)
                dataPageIndex++
                
                fragmentPosition++
            }
        }

        viewBinding.viewpagerOnboard.setOffscreenPageLimit(totalPages);

        viewBinding.viewpagerOnboard.isUserInputEnabled = !RemoteConfigManager.instance!!.boostFNativeObd
        viewBinding.viewpagerOnboard.adapter = adapter

        // Set indicator count based on total pages
        viewBinding.indicatorView.count = totalPages
        viewBinding.indicatorView2.count = totalPages

        viewBinding.viewpagerOnboard.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                viewBinding.indicatorView.selection = position
                viewBinding.indicatorView2.selection = position
                showIndicatorView(position)
                
                val totalPages = getTotalPages()
                val lastPos = totalPages - 1
                
                // Track screen view
                YNMAirBridgeDefaultEvent.pushEventScreenView(YNMAirBridge.AppData("Ob${position + 1}", ""))
                
                // Preload logic based on position and total pages
                when (position) {
                    0 -> {
                        // First screen: preload screens 2 and 3
                        if (!preloadOb2) {
                            preloadOnboarding2()
                        }
                        if (!preloadOb3) {
                            preloadOnboarding3()
                        }
                    }
                    1 -> {
                        // Second screen: preload screen 4 if exists
                        if (totalPages >= 4 && !preload0b4) {
                            preloadOnboarding4()
                        }
                    }
                    2 -> {
                        // Third screen: preload screen 4 if 5 screens, or preload inter if 4 screens
                        if (totalPages == 5 && !preload0b4) {
                            preloadOnboarding4()
                        } else if (totalPages == 4 && !preloadOb6) {
                            preloadOnboarding6()
                        }
                    }
                }
                
                // Preload interstitial before last screen
                if (position == lastPos - 1 && !preloadOb6) {
                    preloadOnboarding6()
                }
                
                // Preload interstitial on last screen if condition matches
                if (position == lastPos && totalPages == 5 && !preloadOb6 
                    && RemoteConfigManager.instance!!.preloadInterFinishObdIndex.toInt() == 4) {
                    preloadOnboarding6()
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
        loadAds(BaseConstants.PLACEMENT_ONBOARDING_INTER, onSuccess = { goHome() }, onFailure = { goHome() })
    }
    private fun goHome() {
        YNMAds.getInstance().adConfig.setInterFlow(RemoteConfigManager.instance!!.getStartIndexInter(), RemoteConfigManager.instance!!.getDeltaIndexInter())
        YNMAds.isGoHome = true;
        BaseNavigator.getInstance().navigateToHome(this)
        finish()
    }

    public fun getListPosNativeFull() : List<Int> {
        // Return positions where ad type is native_fsn (full screen native)
        return positionAdTypeMap.filter { it.value == "native_fsn" }
            .keys
            .sorted()
    }
    
    public fun getListPosNativeNormal() : List<Int> {
        // Return positions where ad type is native_view (normal native)
        return positionAdTypeMap.filter { it.value == "native_view" }
            .keys
            .sorted()
    }
    
    public fun getListNoNative() : List<Int> {
        // Return positions where ad type is not native_view or native_fsn
        return positionAdTypeMap.filter { 
            it.value != "native_view" && it.value != "native_fsn" 
        }
            .keys
            .sorted()
    }
    
    public fun getLastPos(): Int {
        return getTotalPages() - 1
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

    companion object {
        /**
         * List of onboarding pages data
         * Default values are set for 5 pages, can be overridden using setListDataPage()
         */
        private var listDataPage: List<DataPage> = arrayListOf()

        /**
         * Set custom onboarding pages data
         * Call this before starting OnboardingActivity if you want to use custom pages
         *
         * @param list List of DataPage objects containing image, title, and detail resources
         */
        fun setListDataPage(list: List<DataPage>) {
            listDataPage = list
        }

    }
}