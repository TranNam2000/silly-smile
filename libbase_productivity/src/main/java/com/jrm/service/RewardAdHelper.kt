package com.jrm.service

import android.app.Activity
import com.ads.nomyek_admob.ads_components.wrappers.AdsRewardItem
import com.ads.nomyek_admob.utils.AdsCallback
import com.ads.nomyek_admob.utils.AdsRewardMultiPreload

object RewardAdHelper {

    @JvmStatic
    fun showReward(adPlace: String, activity: Activity, adIds:  List<AdsRewardMultiPreload.AdIdModel>, callback: Runnable) {
        var earn = false
        AdsRewardMultiPreload.Companion.getAndShowRewardAdWithMultiId(
            activity,
            adIds,
            adPlace,
            object : AdsCallback() {
                override fun onUserEarnedReward(rewardItem: AdsRewardItem) {
                    super.onUserEarnedReward(rewardItem)
                    earn = true
                }

                override fun onNextAction(isShown: Boolean) {
                    super.onNextAction(isShown)
                    if (earn) {
                        callback.run()
                    }
                }
            }
        )
    }
}