package com.jrm.onboarding.onboarding

import android.util.Log
import android.view.View
import androidx.lifecycle.Observer
import kotlinx.coroutines.*
import com.bumptech.glide.Glide
import com.jrm.R
import com.jrm.ads.WaterfallNativeAdManager
import com.jrm.utils.BaseConstants
import com.jrm.base.BaseFragment
import com.jrm.databinding.FragmentObdSlideBinding
import com.jrm.utils.purchase.IAPHelper


class OnboardingFragment(
    private var position: Int = 0,
    var idImage: Int = R.drawable.obd1,
    var idText: Int = 0,
    var idTextDetail: Int = 0
) : BaseFragment<FragmentObdSlideBinding>(FragmentObdSlideBinding::inflate) {
    private var isFirstPause = true
    companion object {
        var isClickNativeFull = false
    }
    private var showMax = false;
    lateinit var activity : OnboardingActivity;
    private var autoNextJob: Job? = null

    private fun safeGetString(resId: Int): String? {
        return try {
            if (resId != 0) getString(resId) else null
        } catch (e: Exception) {
            null
        }
    }

    override fun initView() {
        activity = requireActivity() as OnboardingActivity
        binding?.imgInside?.setImageResource(idImage)

        // Safe string resource handling
        safeGetString(idText)?.let { binding?.tvInside?.text = it }
        safeGetString(idTextDetail)?.let { binding?.tvDetail?.text = it }

        when (position) {
            0 -> {
                if (!IAPHelper.isPremium()) {
                } else {
                    binding?.nativeOnboarding?.visibility = View.GONE
                }
                Log.d("nativeOB", "ob2NativeHigh")
            }

            1 -> {

            }

            2 -> {
                if (activity.isFiveObd()) {
                    binding?.clFrad?.visibility = View.VISIBLE
                    binding?.nativeOnboarding?.visibility = View.GONE
                } else {
                    binding?.nativeOnboarding?.visibility = View.GONE
                    binding?.nativeOnboardingFull?.visibility = View.VISIBLE
                }
            }

            3 -> {

            }
            4 -> {

            }
        }
    }

    override fun addEvent() {
    }

    private fun setupLoadingOb3Observer() {
        val activity = requireActivity() as OnboardingActivity
        activity.loadingOb3LiveData.observe(this, Observer { isLoading ->
            if (!isLoading) {
                // When loadingOb3 becomes false, show native ad
                showNativeObd3()
            }
        })
    }
    private var isFirstResume = true
    override fun onResume() {
        super.onResume()
        if (isFirstResume) {
            Log.d("Onboarding Fragment", "onResume: " + position)
            when (position) {
                0 -> {
                    if (!IAPHelper.isPremium()) {
                        context?.let {
                            WaterfallNativeAdManager.show(
                                activity,
                                adView = binding!!.nativeOnboarding,
                                adPlace = BaseConstants.NATIVE_ONBOARD_1,
                                waitForLoad = true // true: chờ nếu đang loading, false: fail ngay
                            ) { success ->
                                if (success) {
                                }
                            }
                        }
                    } else {
                        binding?.nativeOnboarding?.visibility = View.GONE
                    }
                    Log.d("nativeOB", "ob2NativeHigh")
                }

                1 -> {
                    context?.let {
                        WaterfallNativeAdManager.show(
                            activity,
                            adView = binding!!.nativeOnboarding,
                            adPlace = BaseConstants.NATIVE_ONBOARD_2,
                            waitForLoad = true // true: chờ nếu đang loading, false: fail ngay
                        ) { success ->
                            if (success) {
                            }
                        }
//                        AdsNativeMultiPreload.showPreloadedNativeAd(
//                            it,
//                            binding!!.nativeOnboarding,
//                            BaseConstants.NATIVE_ONBOARD_2,
//                            R.layout.custom_native_admob_large,
//                            R.layout.custom_native_admob_large,
//                            null,
//                            null
//                        )
                    }
                }

                2 -> {
                    if (activity.isFiveObd()) {
                        binding?.clFrad?.visibility = View.VISIBLE
//                    binding?.lottie?.setVisibility(View.VISIBLE)
                        binding?.nativeOnboarding?.visibility = View.GONE
                    } else {
                        binding?.nativeOnboarding?.visibility = View.GONE
                        binding?.nativeOnboardingFull?.visibility = View.VISIBLE
                        setupLoadingOb3Observer()
                    }
                }

                3 -> {
                    if (activity.isFiveObd()) {
                        binding?.nativeOnboarding?.visibility = View.GONE
                        binding?.nativeOnboardingFull?.visibility = View.VISIBLE
                        context?.let {
                            WaterfallNativeAdManager.show(
                                activity,
                                adView = binding!!.nativeOnboardingFull,
                                adPlace = BaseConstants.NATIVE_ONBOARD_4,
                                waitForLoad = true // true: chờ nếu đang loading, false: fail ngay
                            ) { success ->
                                if (success) {
                                }
                            }
//                            AdsNativeMultiPreload.showPreloadedNativeAd(
//                                it,
//                                binding!!.nativeOnboardingFull,
//                                BaseConstants.NATIVE_ONBOARD_4,
//                                R.layout.custom_full_screen_native_ads,
//                                R.layout.custom_full_screen_native_ads,
//                                object : YNMAdsCallbacks() {
//                                    override fun onAdFailedToLoad(adError: AdsError?) {
//                                        super.onAdFailedToLoad(adError)
//                                        binding?.nativeOnboardingFull?.visibility = View.GONE
//                                        showDefaultScreen()
//                                    }
//
//                                    override fun onAdClicked() {
//                                        super.onAdClicked()
//                                        isClickNativeFull = true;
//                                    }
//                                },
//                                null
//                            )
                        }
                    } else {
                        context?.let {
                            WaterfallNativeAdManager.show(
                                activity,
                                adView = binding!!.nativeOnboarding,
                                adPlace = BaseConstants.NATIVE_ONBOARD_4,
                                waitForLoad = true // true: chờ nếu đang loading, false: fail ngay
                            ) { success ->
                                if (success) {
                                }
                            }
//                            AdsNativeMultiPreload.showPreloadedNativeAd(
//                                it,
//                                binding!!.nativeOnboarding,
//                                BaseConstants.NATIVE_ONBOARD_4,
//                                R.layout.custom_native_admob_large,
//                                R.layout.custom_native_admob_large,
//                                null,
//                                null
//                            )
                        }
                    }
                }
                4 -> {
                    if (activity.isFiveObd()) {
                        context?.let {
                            WaterfallNativeAdManager.show(
                                activity,
                                adView = binding!!.nativeOnboarding,
                                adPlace = BaseConstants.NATIVE_ONBOARD_5,
                                waitForLoad = true // true: chờ nếu đang loading, false: fail ngay
                            ) { success ->
                                if (success) {
                                }
                            }
//                            AdsNativeMultiPreload.showPreloadedNativeAd(
//                                it,
//                                binding!!.nativeOnboarding,
//                                BaseConstants.NATIVE_ONBOARD_5,
//                                R.layout.custom_native_admob_large,
//                                R.layout.custom_native_admob_large,
//                                null,
//                                null
//                            )
                        }
                    }
                }
            }
        } else {
            if (position in activity.getListPosNativeFull()) {
                if (isClickNativeFull) {
                    isClickNativeFull = false;
                    (requireActivity() as OnboardingActivity).onClickNext()
                } else {
                    // Start auto next job after 5 seconds
                    startAutoNextJob()
                }
                return
            }
        }
        isFirstResume = false
    }

    private fun showNativeObd3() {
        WaterfallNativeAdManager.show(
            activity,
            adView = binding!!.nativeOnboardingFull,
            adPlace = BaseConstants.NATIVE_ONBOARD_3,
            waitForLoad = true // true: chờ nếu đang loading, false: fail ngay
        ) { success ->
            if (success) {
            }
        }
    }

    private fun reShowNativeOnboarding(position: Int) {
        return
    }

    private fun showDefaultScreen() {
        binding?.llMain?.visibility = View.VISIBLE
        binding?.imgInside?.visibility = View.VISIBLE
        binding?.imgInside?.setImageResource(idImage)

        // Safe string resource handling
        safeGetString(idText)?.let { binding?.tvInside?.text = it }

        binding?.let {
            Glide.with(requireContext()).load(idImage)
                .into(it.imgInside)
        }

        binding?.nativeOnboarding?.visibility = View.GONE
        binding?.nativeOnboardingFull?.visibility = View.GONE
        (requireActivity() as OnboardingActivity).showIndicatorView(0)
    }

    private fun startAutoNextJob() {
        // Cancel any existing job first
        cancelAutoNextJob()

        autoNextJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                delay(7000) // Wait 5 seconds
                if (isAdded && !isDetached && isResumed) {
                    (requireActivity() as OnboardingActivity).onClickNext()
                }
            } catch (e: CancellationException) {
                // Job was cancelled, do nothing
                Log.d("OnboardingFragment", "Auto next job cancelled")
            }
        }
    }

    private fun cancelAutoNextJob() {
        if (autoNextJob != null) {
            autoNextJob?.cancel()
            autoNextJob = null
        }
    }


    override fun onPause() {
        super.onPause()
        if (isFirstPause) {
            isFirstPause = false
        }
        // Cancel auto next job when app goes to background
        cancelAutoNextJob()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Cancel auto next job when fragment is destroyed
        cancelAutoNextJob()
    }

}
