package com.jrm.base.tracking

/**
 * Interface for views that support automatic click tracking
 */
interface TrackableView {
    /**
     * Enable or disable tracking for this view
     */
    var enableTracking: Boolean

    /**
     * Custom screen name override (optional)
     * If null, will auto-detect from Activity/Fragment
     */
    var customScreenName: String?
    
    /**
     * Custom element type override (optional)
     * If null, will use default type based on view class
     */
    var customElementType: String?
    
    /**
     * Get the current screen name for tracking
     */
    fun getScreenName(): String?
    
    /**
     * Log click event for this view
     */
    fun logElementClick()
}



