package com.jrm.base

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.ads.nomyek_admob.ads_components.YNMAds
import com.ads.nomyek_admob.event.YNMAirBridge
import com.google.firebase.analytics.FirebaseAnalytics

class BaseEventLogger {
    
    companion object {
        private const val TAG = "BaseEventLogger"
        private var firebaseAnalytics: FirebaseAnalytics? = null
        
        // Screen tracking variables
        private var currentScreen: String? = null
        private var currentScreenStartTime: Long = 0
        private var previousScreen: String? = null
        private var previousScreenDuration: Long = 0
        private var currentScreenResume: String = ""
        
        @JvmStatic
        fun initialize(context: Context) {
            try {
                firebaseAnalytics = FirebaseAnalytics.getInstance(context)
                Log.d(TAG, "BaseEventLogger initialized successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing BaseEventLogger: ${e.message}", e)
            }
        }
        
        @JvmStatic
        private fun logCustomEventInternal(
            eventName: String,
            action: String?,
            label: String?,
            value: Number?,
            custom: Map<String, Any?>?
        ) {
            try {
                val eventData = mutableMapOf<String, Any?>().apply {
                    put("event_name", eventName)
                    action?.let{put("action", action)}
                    label?.let{put("label", label)}
                    value?.let { put("value", it) }
                    custom?.let { putAll(it) }
                }
                
                Log.d(TAG, "Custom Event: $eventData")
                
                // Push to Firebase Analytics
                pushToFirebase(eventName, action, label, value, custom)
                
                // Push to AirBridge
                pushToAirBridge(eventName, action, label, value, custom)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error logging custom event: ${e.message}", e)
            }
        }
        
        /**
         * Alternative method for eventName + custom map only
         * Kept for backward compatibility
         */
        @JvmStatic
        fun logCustomEventWithMap(
            eventName: String,
            custom: Map<String, Any?>?
        ) {
            logCustomEventInternal(eventName, null, null, null, custom)
        }
        
        @JvmStatic
        fun logEvent(eventName: String) {
            logCustomEvent(eventName)
        }
        
        @JvmStatic
        fun logEvent(eventName: String, custom: Map<String, Any?>?) {
            logCustomEvent(eventName, custom = custom)
        }
        
        @JvmStatic
        @JvmOverloads
        fun logEvent(
            eventName: String,
            action: String,
            label: String,
            value: Number? = null,
            custom: Map<String, Any?>? = null
        ) {
            logCustomEvent(eventName, action, label, value, custom)
        }
        
        // ===== SIMPLIFIED logCustomEvent WITH DEFAULT PARAMETERS =====
        
        /**
         * Log custom event with optional parameters
         * Can be called with 1, 2, 3, 4, or 5 parameters using default values
         */
        @JvmStatic
        @JvmOverloads
        fun logCustomEvent(
            eventName: String,
            action: String? = null,
            label: String? = null,
            value: Number? = null,
            custom: Map<String, Any?>? = null
        ) {
            logCustomEventInternal(eventName, action, label, value, custom)
        }
        
        /**
         * Push event to Firebase Analytics
         */
        private fun pushToFirebase(
            eventName: String,
            action: String?,
            label: String?,
            value: Number?,
            custom: Map<String, Any?>?
        ) {
            try {
                firebaseAnalytics?.let { analytics ->
                    val bundle = Bundle().apply {
                        action?.let { putString("action", it) }
                        label?.let { putString("label", it) }
                        value?.let { 
                            when (it) {
                                is Int -> putInt("value", it)
                                is Long -> putLong("value", it)
                                is Float -> putFloat("value", it)
                                is Double -> putDouble("value", it)
                                else -> putString("value", it.toString())
                            }
                        }
                        
                        // Add custom parameters
                        custom?.forEach { (key, customValue) ->
                            when (customValue) {
                                is String -> putString(key, customValue)
                                is Int -> putInt(key, customValue)
                                is Long -> putLong(key, customValue)
                                is Float -> putFloat(key, customValue)
                                is Double -> putDouble(key, customValue)
                                is Boolean -> putBoolean(key, customValue)
                                null -> putString(key, "null")
                                else -> putString(key, customValue.toString())
                            }
                        }
                    }
                    
                    analytics.logEvent(eventName, bundle)
                    Log.d(TAG, "Event pushed to Firebase: $eventName")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error pushing to Firebase: ${e.message}", e)
            }
        }
        
        /**
         * Push event to AirBridge using YNMAirBridge
         */
        private fun pushToAirBridge(
            eventName: String,
            action: String?,
            label: String?,
            value: Number?,
            custom: Map<String, Any?>?
        ) {
            try {
                YNMAirBridge.getInstance()?.let { bridge ->
                    // Convert custom map to match AirBridge expected format
                    val customMap = custom?.let { map ->
                        val convertedMap = mutableMapOf<String, Any>()
                        map.forEach { (key, customValue) ->
                            customValue?.let { convertedMap[key] = it }
                        }
                        convertedMap
                    }
                    // Use YNMAirBridge logCustomEvent method
                    bridge.logCustomEvent(eventName, action?: "", label?: "", value?: 0, customMap)
                    Log.d(TAG, "Event pushed to AirBridge: $eventName")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error pushing to AirBridge: ${e.message}", e)
            }
        }
        
        // ===== SCREEN TRACKING METHODS =====
        
        /**
         * Log screen view event when entering a new screen
         * Automatically logs screen_view_complete for previous screen if exists
         * @param screenName Current screen name (activity or fragment name)
         */
        @JvmStatic
        fun logScreenView(screenName: String) {
            try {
                val currentTime = System.currentTimeMillis()
                
                // Auto-log screen_view_complete for previous screen if we have one
                if (currentScreen != null && currentScreenStartTime > 0) {
                    val currentScreenDuration = (currentTime - currentScreenStartTime) / 1000
                    
                    // Create custom parameters for complete event (same as screen_view)
                    val completeCustomParams = mutableMapOf<String, Any?>().apply {
                        previousScreen?.let { put("previous_screen", it) }
                        if (previousScreenDuration > 0) {
                            put("duration_previous_screen", previousScreenDuration)
                        }
                    }
                    
                    // Log the screen_view_complete event for previous screen
                    logCustomEventInternal("screen_view_complete", null, currentScreen!!, currentScreenDuration, completeCustomParams)
                    Log.d(TAG, "Auto screen view complete logged: $currentScreen, duration: ${currentScreenDuration}s")
                    
                    // Update previous screen info for the new screen_view event
                    previousScreenDuration = currentScreenDuration
                    previousScreen = currentScreen
                } else {
                    previousScreenDuration = 0
                    previousScreen = null
                }
                
                // Update current screen tracking
                currentScreen = screenName
                YNMAds.setCurrentScreen(screenName)
                currentScreenStartTime = currentTime
                
                // Create custom parameters for new screen_view event
                val customParams = mutableMapOf<String, Any?>().apply {
                    previousScreen?.let { put("previous_screen", it) }
                    if (previousScreenDuration > 0) {
                        put("duration_previous_screen", previousScreenDuration)
                    }
                }
                
                // Log the screen_view event for new screen
                logCustomEventInternal("screen_view", null, screenName, null, customParams)
                
                Log.d(TAG, "Screen view logged: $screenName, previous: $previousScreen, duration: ${previousScreenDuration}s")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error logging screen view: ${e.message}", e)
            }
        }
        
        /**
         * Log screen view complete event when leaving a screen
         * This method is now mainly used for final screen completion (app closing)
         * Regular screen transitions are handled automatically in logScreenView()
         * @param screenName Current screen name that is being completed
         */
        @JvmStatic
        fun logScreenViewComplete(screenName: String) {
            try {
                val currentTime = System.currentTimeMillis()
                
                // Calculate current screen duration
                val currentScreenDuration = if (currentScreenStartTime > 0) {
                    (currentTime - currentScreenStartTime) / 1000
                } else {
                    0L
                }
                
                // Create custom parameters (same as screen_view)
                val customParams = mutableMapOf<String, Any?>().apply {
                    previousScreen?.let { put("previous_screen", it) }
                    if (previousScreenDuration > 0) {
                        put("duration_previous_screen", previousScreenDuration)
                    }
                }
                
                // Log the screen_view_complete event with current screen duration as value
                logCustomEventInternal("screen_view_complete", null, screenName, currentScreenDuration, customParams)
                
                Log.d(TAG, "Manual screen view complete logged: $screenName, duration: ${currentScreenDuration}s")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error logging screen view complete: ${e.message}", e)
            }
        }
        
        /**
         * Log final screen complete when app is closing or last activity is destroyed
         * This should only be called manually when you're certain it's the final screen
         * (e.g., in Application.onTerminate() or specific exit points)
         */
        @JvmStatic
        fun logFinalScreenComplete() {
            try {
                currentScreen?.let { screenName ->
                    logScreenViewComplete(screenName)
                    // Clear tracking variables
                    currentScreen = null
                    currentScreenStartTime = 0
                    previousScreen = null
                    previousScreenDuration = 0
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error logging final screen complete: ${e.message}", e)
            }
        }
        
        /**
         * Reset tracking state - useful for testing or when you want to start fresh
         */
        @JvmStatic
        fun resetScreenTracking() {
            currentScreen = null
            currentScreenStartTime = 0
            previousScreen = null
            previousScreenDuration = 0
            Log.d(TAG, "Screen tracking state reset")
        }
        
        /**
         * Get current screen name for debugging purposes
         */
        @JvmStatic
        fun getCurrentScreen(): String? = currentScreen
        
        /**
         * Get previous screen name for debugging purposes
         */
        @JvmStatic
        fun getPreviousScreen(): String? = previousScreen
        
        // ===== ELEMENT CLICK TRACKING METHODS =====
        
        /**
         * Log element click event
         * @param elementId The ID of the element (from XML resource ID)
         * @param screenName The screen name (activity or fragment name)
         * @param elementType The type of element (button, imageview, layout, etc.)
         */
        @JvmStatic
        fun logElementClick(
            elementId: String,
            screenName: String?,
            elementType: String
        ) {
            logCustomEvent(
                eventName = "element_click",
                custom = mapOf(
                    "element_id" to elementId,
                    "tag_screen" to (screenName ?: "unknown"),
                    "element_type" to elementType
                )
            )
        }

        @JvmStatic
        fun logScreenViewResume(screenName: String) {
            try {
                val currentTime = System.currentTimeMillis()
                // Update current screen tracking
                currentScreenResume = screenName
                
                // Create custom parameters for new screen_view event
                val customParams = mutableMapOf<String, Any?>().apply {
                    previousScreen?.let { put("previous_screen", it) }
                }
                
                // Log the screen_view event for new screen
                logCustomEventInternal("screen_view_fix", null, screenName, null, customParams)
                Log.d(TAG, "Screen view logged: $screenName, previous: $previousScreen, duration: ${previousScreenDuration}s")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error logging screen view: ${e.message}", e)
            }
        }
    }
}