package com.jrm.utils.purchase

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
