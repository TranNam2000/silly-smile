package com.jrm.onboarding.onboarding

import android.view.View
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.jrm.R
import com.jrm.base.BaseFragment
import com.jrm.databinding.FragmentObdSlideBinding
import com.jrm.utils.AdsHelper
import com.jrm.utils.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class OnboardingFragment(
    private var position: Int = 0,
    var idImage: Int = R.drawable.obd1,
    var idText: Int = 0,
    var idTextDetail: Int = 0,
    var placementName: String,
    var type: String
) : BaseFragment<FragmentObdSlideBinding>(FragmentObdSlideBinding::inflate) {
    private var isFirstPause = true

    lateinit var activity: OnboardingActivity;
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
    }

    override fun addEvent() {
    }

    private var isFirstResume = true
    override fun onResume() {
        super.onResume()
        if (isFirstResume) {
            if (!AdsHelper.isDisableObdAd()) {
                if (type == "native_view")
                    activity.loadAds(
                        placementName,
                        adView = binding!!.nativeOnboarding,
                        onSuccess = { },
                        onFailure = { },
                        lifecycleOwner = this
                    ) else
                    activity.loadAds(
                        placementName,
                        adView = binding!!.nativeOnboardingFull,
                        onSuccess = { },
                        onFailure = { },
                        lifecycleOwner = this
                    )
            } else {
                binding?.nativeOnboarding?.isVisible = true
                binding?.nativeOnboardingFull?.isVisible = true
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
                Logger.d("Auto next job cancelled")
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
