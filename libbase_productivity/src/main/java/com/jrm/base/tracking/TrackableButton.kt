package com.jrm.base.tracking

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatButton
import com.jrm.R

/**
 * Custom Button that automatically tracks click events
 */
class TrackableButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.buttonStyle
) : AppCompatButton(context, attrs, defStyleAttr), TrackableView {

    override var enableTracking: Boolean = true
    override var customScreenName: String? = null
    override var customElementType: String? = null

    init {
        // Parse custom attributes
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.TrackableView)
            try {
                enableTracking = typedArray.getBoolean(R.styleable.TrackableView_enableTracking, true)
                customScreenName = typedArray.getString(R.styleable.TrackableView_customScreenName)
                customElementType = typedArray.getString(R.styleable.TrackableView_customElementType)
            } finally {
                typedArray.recycle()
            }
        }
    }

    override fun setOnClickListener(listener: OnClickListener?) {
        if (enableTracking) {
            // Wrap listener with tracking
            super.setOnClickListener { view ->
                logElementClick()
                listener?.onClick(view)
            }
        } else {
            // No tracking, use original listener
            super.setOnClickListener(listener)
        }
    }

    override fun getScreenName(): String? {
        return customScreenName ?: TrackingHelper.getCurrentScreenName()
    }

    override fun logElementClick() {
        TrackingHelper.logElementClick(this, getScreenName(), customElementType)
    }
}

