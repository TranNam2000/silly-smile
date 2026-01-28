package com.jrm.utils.purchase

import android.content.Context
import android.util.Log
import com.jrm.base.BaseEventLogger
import com.jrm.utils.BaseConstants
import com.jrm.utils.SharedPref

object IAPHelper {
    private const val TAG = "IAPHelper"

    
    /**
     * Check if user has premium subscription
     */
    @JvmStatic
    fun isPremium(): Boolean {
        if (!SharedPref.readBoolean(BaseConstants.ENABLE_ADS, true)) {
            return true
        }
        return false
    }
    

}
