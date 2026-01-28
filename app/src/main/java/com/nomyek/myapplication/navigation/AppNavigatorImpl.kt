package com.nomyek.myapplication.navigation

import android.content.Context
import android.content.Intent
import com.ads.nomyek_admob.ads_components.YNMAds
import com.jrm.onboarding.navigation.BaseNavigator
import com.jrm.utils.remote_config.RemoteConfigManager
import com.nomyek.myapplication.ui.main.MainActivity

class AppNavigatorImpl : BaseNavigator {
    override fun navigateToDictionary(context: Context, word: String) {
        TODO("Not yet implemented")
    }

    override fun navigateToTranslate(context: Context, text: String) {
        TODO("Not yet implemented")
    }

    override fun navigateToCamera(context: Context) {

    }

    override fun navigateToHome(context: Context) {
        YNMAds.getInstance().adConfig.setInterFlow(RemoteConfigManager.instance!!.getStartIndexInter(), RemoteConfigManager.instance!!.getDeltaIndexInter())
        YNMAds.isGoHome = true;
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
    }
}