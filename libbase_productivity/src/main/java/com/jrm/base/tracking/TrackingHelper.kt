package com.jrm.base.tracking

import android.view.View
import com.jrm.base.BaseEventLogger

/**
 * Helper class for automatic view tracking
 */
object TrackingHelper {
    
    /**
     * Get current screen name from BaseEventLogger
     */
    fun getCurrentScreenName(): String? {
        return BaseEventLogger.getCurrentScreen()
    }
    
    /**
     * Get element type from view class
     */
    fun getElementType(view: View): String {
        return when (view::class.java.simpleName) {
            "TrackableButton", "AppCompatButton", "Button" -> "button"
            "TrackableImageView", "AppCompatImageView", "ImageView" -> "imageview"
            "TrackableTextView", "AppCompatTextView", "TextView" -> "textview"
            "TrackableLinearLayout", "LinearLayout" -> "linearlayout"
            "TrackableConstraintLayout", "ConstraintLayout" -> "constraintlayout"
            "TrackableRelativeLayout", "RelativeLayout" -> "relativelayout"
            "TrackableCardView", "MaterialCardView", "CardView" -> "cardview"
            else -> view::class.java.simpleName.lowercase()
        }
    }
    
    /**
     * Get element ID from view
     */
    fun getElementId(view: View): String {
        return try {
            if (view.id != View.NO_ID) {
                view.resources.getResourceEntryName(view.id)
            } else {
                "no_id"
            }
        } catch (e: Exception) {
            "unknown"
        }
    }
    
    /**
     * Log element click event
     */
    fun logElementClick(
        view: View,
        screenName: String?,
        elementType: String? = null
    ) {
        val elementId = getElementId(view)
        val type = elementType ?: getElementType(view)
        
        BaseEventLogger.logCustomEvent(
            eventName = "element_click",
            action = null,
            label = elementId,
            custom = mapOf(
                "element_id" to elementId,
                "tag_screen" to (screenName ?: "unknown"),
                "element_type" to type
            )
        )
    }
}

