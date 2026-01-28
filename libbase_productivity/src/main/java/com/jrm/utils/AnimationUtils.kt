package com.jrm.utils

import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils as AndroidAnimationUtils
import com.jrm.R

/**
 * Utility class for applying reusable animations to views
 */
object AnimationUtils {
    
    /**
     * Apply shake animation to a view (rotation + scale)
     * @param view The view to animate
     * @param onAnimationEnd Optional callback when animation ends
     */
    fun shakeView(view: View, onAnimationEnd: (() -> Unit)? = null) {
        val animation = AndroidAnimationUtils.loadAnimation(view.context, R.anim.shake_animation)
        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationRepeat(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                onAnimationEnd?.invoke()
            }
        })
        view.startAnimation(animation)
    }
    
    /**
     * Apply horizontal shake animation to a view
     * @param view The view to animate
     * @param onAnimationEnd Optional callback when animation ends
     */
    fun shakeHorizontal(view: View, onAnimationEnd: (() -> Unit)? = null) {
        val animation = AndroidAnimationUtils.loadAnimation(view.context, R.anim.shake_horizontal)
        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationRepeat(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                onAnimationEnd?.invoke()
            }
        })
        view.startAnimation(animation)
    }
    
    /**
     * Apply vertical shake animation to a view
     * @param view The view to animate
     * @param onAnimationEnd Optional callback when animation ends
     */
    fun shakeVertical(view: View, onAnimationEnd: (() -> Unit)? = null) {
        val animation = AndroidAnimationUtils.loadAnimation(view.context, R.anim.shake_vertical)
        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationRepeat(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                onAnimationEnd?.invoke()
            }
        })
        view.startAnimation(animation)
    }
    
    /**
     * Apply bounce animation to a view
     * @param view The view to animate
     * @param onAnimationEnd Optional callback when animation ends
     */
    fun bounceView(view: View, onAnimationEnd: (() -> Unit)? = null) {
        val animation = AndroidAnimationUtils.loadAnimation(view.context, R.anim.bounce_animation)
        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationRepeat(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                onAnimationEnd?.invoke()
            }
        })
        view.startAnimation(animation)
    }
    
    /**
     * Apply custom animation from resource to a view
     * @param view The view to animate
     * @param animResId Animation resource ID
     * @param onAnimationEnd Optional callback when animation ends
     */
    fun applyAnimation(view: View, animResId: Int, onAnimationEnd: (() -> Unit)? = null) {
        val animation = AndroidAnimationUtils.loadAnimation(view.context, animResId)
        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationRepeat(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                onAnimationEnd?.invoke()
            }
        })
        view.startAnimation(animation)
    }
    
    /**
     * Stop any running animation on a view
     * @param view The view to clear animation
     */
    fun clearAnimation(view: View) {
        view.clearAnimation()
    }
}


