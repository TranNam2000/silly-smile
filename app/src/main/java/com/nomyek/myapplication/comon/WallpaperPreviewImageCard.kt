package com.nomyek.myapplication.comon

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.nomyek.myapplication.R
import com.nomyek.myapplication.data.model.CropInfo
import com.nomyek.myapplication.databinding.ViewWallpaperPreviewImageCardBinding
import com.nomyek.myapplication.utils.click

/**
 * Custom CardView để hiển thị preview static image wallpaper với drag, zoom, rotate
 */
class WallpaperPreviewImageCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding: ViewWallpaperPreviewImageCardBinding =
        ViewWallpaperPreviewImageCardBinding.inflate(
            LayoutInflater.from(context),
            this,
            true
        )

    private var isPreviewMode = false

    private val borderView: View? by lazy {
        binding.root.findViewById<View>(R.id.view_border)
    }

    private val roundedCornerOverlayView: RoundedCornerOverlayView? by lazy {
        binding.root.findViewById(R.id.view_rounded_corner_overlay)
    }

    private var showBorderView: Boolean = true

    init {
        // Disable ImageView touch handling - RoundedCornerOverlayView will handle it
        roundedCornerOverlayView?.imageView?.isClickable = false
        roundedCornerOverlayView?.imageView?.isFocusable = false

        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.WallpaperPreviewCard,
            0, 0
        ).apply {
            try {
                val showBadge = getBoolean(R.styleable.WallpaperPreviewCard_showBadge, true)
                setBadgeVisible(showBadge)

                val badgeText = getString(R.styleable.WallpaperPreviewCard_badgeText)
                badgeText?.let { setBadgeText(it) }

                showBorderView = getBoolean(R.styleable.WallpaperPreviewCard_showBorderView, true)

                val isPreview = getBoolean(R.styleable.WallpaperPreviewCard_preview, false)
                if (isPreview) {
                    setPreviewMode(true)
                }
            } finally {
                recycle()
            }
        }
        binding.layoutChargingBadge.click {
            resetTransform()
        }
    }

    fun loadWallpaper(imageUrl: String) = roundedCornerOverlayView?.loadWallpaper(imageUrl)


    /**
     * Set badge text
     */
    fun setBadgeText(text: String) {
        binding.tvBadgeText.text = text
    }

    /**
     * Show/hide charging badge
     */
    fun setBadgeVisible(visible: Boolean) {
        binding.layoutChargingBadge.visibility = if (visible) View.VISIBLE else View.GONE
    }


    fun setPreviewMode(isPreview: Boolean) {
        isPreviewMode = isPreview

        roundedCornerOverlayView?.setInteractionEnabled(!isPreview)

        setBadgeVisible(!isPreview)

        if (width == 0 || height == 0) {
            post { setPreviewMode(isPreview) }
            return
        }
        borderView?.visibility = GONE
    }

    /**
     * Reset transform
     */
    private fun resetTransform() {
        roundedCornerOverlayView?.resetTransform()
    }

    fun getCroppedBitmap(): CropInfo? = roundedCornerOverlayView?.getCropInfo()

    fun applyCropInfo(cropInfo: CropInfo) {
        android.util.Log.d(
            "WallpaperPreviewImageCard",
            "applyCropInfo called with: scale=${cropInfo.scaleFactor}, tx=${cropInfo.translationX}, ty=${cropInfo.translationY}"
        )
        roundedCornerOverlayView?.applyCropInfo(cropInfo)
    }


}
