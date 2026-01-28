package com.nomyek.myapplication.ui.detail

import android.view.View
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs

class CarouselPageTransformer : ViewPager2.PageTransformer {
    
    companion object {
        private const val MIN_SCALE = 0.8f
        private const val MAX_ROTATION = 35f
    }

    override fun transformPage(view: View, position: Float) {
        view.apply {
            // Camera distance cho hiệu ứng 3D mượt mà hơn
            cameraDistance = width * 10f
            
            when {
                position < -1 -> {
                    alpha = 0f
                }
                position <= 1 -> {
                  
                    val absPosition = abs(position)
                    
                    // Scale: item giữa to nhất
                    val scale = MIN_SCALE + (1 - MIN_SCALE) * (1 - absPosition)
                    scaleX = scale
                    scaleY = scale
                    
                    rotationY = -MAX_ROTATION * position
                    
                    translationX = -position * width * 0.25f
                    
                    alpha = 0.4f + (1 - absPosition) * 0.6f
                    
                    translationZ = -absPosition * 150
                    
                    elevation = (1 - absPosition) * 10
                }
                else -> {
                    alpha = 0f
                }
            }
        }
    }
}

