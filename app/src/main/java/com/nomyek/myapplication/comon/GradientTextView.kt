package com.nomyek.myapplication.comon

import android.content.Context
import android.graphics.LinearGradient
import android.graphics.Shader
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.nomyek.myapplication.R

class GradientTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private var startColor: Int = 0xFF5936C8.toInt()
    private var centerColor: Int = 0xFFC54DE2.toInt()
    private var endColor: Int = 0xFFFFFFFF.toInt()
    private var gradientAngle: Float = 0f // 0 = horizontal, 90 = vertical

    init {
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.GradientTextView)
            startColor = typedArray.getColor(R.styleable.GradientTextView_gradientStartColor, startColor)
            centerColor = typedArray.getColor(R.styleable.GradientTextView_gradientCenterColor, centerColor)
            endColor = typedArray.getColor(R.styleable.GradientTextView_gradientEndColor, endColor)
            gradientAngle = typedArray.getFloat(R.styleable.GradientTextView_gradientAngle, gradientAngle)
            typedArray.recycle()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        applyGradient()
    }

    override fun setText(text: CharSequence?, type: BufferType?) {
        super.setText(text, type)
        applyGradient()
    }

    private fun applyGradient() {
        val textWidth = paint.measureText(text.toString())
        if (textWidth <= 0) return

        val radians = Math.toRadians(gradientAngle.toDouble())
        val endX = (textWidth * Math.cos(radians)).toFloat()
        val endY = (textHeight * Math.sin(radians)).toFloat()

        val gradient = LinearGradient(
            0f, 0f, endX, endY,
            intArrayOf(startColor, centerColor, endColor),
            null,
            Shader.TileMode.CLAMP
        )

        paint.shader = gradient
        invalidate()
    }

    private val textHeight: Float
        get() = paint.fontMetrics.descent - paint.fontMetrics.ascent

    fun setGradientColors(start: Int, center: Int, end: Int) {
        startColor = start
        centerColor = center
        endColor = end
        applyGradient()
    }
}