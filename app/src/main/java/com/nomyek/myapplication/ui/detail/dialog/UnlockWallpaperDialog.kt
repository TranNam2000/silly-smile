package com.nomyek.myapplication.ui.detail.dialog

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.Window
import androidx.core.view.isVisible
import com.ads.nomyek_admob.ads_components.ads_reward.YNMRewardAds
import com.ads.nomyek_admob.ads_components.wrappers.AdsRewardItem
import com.ads.nomyek_admob.utils.AdsCallback
import com.jrm.BuildConfig
import com.jrm.utils.AdsHelper
import com.jrm.utils.remote_config.RemoteConfigManager
import com.nomyek.myapplication.databinding.DialogUnlockWallpaperBinding
import com.nomyek.myapplication.ui.main.MainActivity

class UnlockWallpaperDialog(
    private val context: Context,
    private val activity: Activity,
    private val onWatchVideoClick: () -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogUnlockWallpaperBinding

    init {
        setupDialog()
    }

    private fun setupDialog() {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setCancelable(true)

        binding = DialogUnlockWallpaperBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)
        setupViews()
    }

    private fun setupViews() {
        binding.apply {
            btnClose.setOnClickListener {
                dismiss()
            }

            btnWatchVideo.setOnClickListener {
                var adIds = RemoteConfigManager.instance!!.getListAdIdRewardFromRemote("unlock",
                    RemoteConfigManager.instance!!.rewardHighIds)
                if (BuildConfig.FLAVOR == "appDev") {
                    onWatchVideoClick()
                    dismiss()
                } else {
                    AdsHelper.showReward("unlock", activity, adIds, {
                        onWatchVideoClick()
                        dismiss()
                    })
                }
            }
        }
    }
}