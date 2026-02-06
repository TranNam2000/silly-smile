package com.jrm.onboarding.navigation

import android.content.Context
import android.content.Intent
import com.ads.nomyek_admob.ads_components.YNMAds
import com.jrm.base.BaseActivity
import com.jrm.utils.remote_config.RemoteConfigManager
import kotlin.jvm.java

class BaseNavigator() {

    private var homeActivity: Class<*>? = null
    fun setHomeActivity(activity: Class<*>) {
        homeActivity = activity
    }
    
    /**
     * Navigate to Main/Home Activity
     */
    fun navigateToHome(context: Context){
        homeActivity?.let { homeActivity ->
            YNMAds.getInstance().adConfig.setInterFlow(
                RemoteConfigManager.instance!!.getStartIndexInter(),
                RemoteConfigManager.instance!!.getDeltaIndexInter()
            )
            YNMAds.isGoHome = true;
            val intent = Intent(context, homeActivity).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(intent)
        }
    }
    
    companion object {
        private var instance: BaseNavigator? = null

        fun getInstance(): BaseNavigator = instance?: BaseNavigator().also { instance = it }
    }
}
