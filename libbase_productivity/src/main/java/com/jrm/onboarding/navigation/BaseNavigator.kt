package com.jrm.onboarding.navigation

import android.content.Context

interface BaseNavigator {
    
    /**
     * Navigate to Dictionary Activity with a specific word
     */
    fun navigateToDictionary(context: Context, word: String)
    
    /**
     * Navigate to Translate Activity with optional text
     */
    fun navigateToTranslate(context: Context, text: String = "")
    
    /**
     * Navigate to Camera Activity
     */
    fun navigateToCamera(context: Context)
    
    /**
     * Navigate to Main/Home Activity
     */
    fun navigateToHome(context: Context)
    
    companion object {
        private var instance: BaseNavigator? = null
        
        fun setInstance(navigator: BaseNavigator) {
            instance = navigator
        }
        
        fun getInstance(): BaseNavigator? = instance
    }
}
