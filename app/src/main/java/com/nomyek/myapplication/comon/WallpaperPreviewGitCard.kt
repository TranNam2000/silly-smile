package com.nomyek.myapplication.comon

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.nomyek.myapplication.R
import com.nomyek.myapplication.data.model.CropInfo
import com.nomyek.myapplication.databinding.ViewWallpaperPreviewGifCardBinding

/**
 * Custom CardView để hiển thị preview GIF wallpaper với drag, zoom, rotate
 */
class WallpaperPreviewGitCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding: ViewWallpaperPreviewGifCardBinding

    private var isPreviewMode = false


    // Rounded corner overlay view
    private val roundedCornerOverlayView: RoundedCornerOverlayView? by lazy {
        binding.root.findViewById(R.id.view_rounded_corner_overlay)
    }

    private var showBorderView: Boolean = true


    init {
        binding = ViewWallpaperPreviewGifCardBinding.inflate(
            LayoutInflater.from(context),
            this, true
        )

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

        binding.layoutChargingBadge.setOnClickListener {
            resetTransform()
        }
    }


    fun getCropInfo() = roundedCornerOverlayView?.getCropInfo()

    fun loadWallpaper(imageUrl: String) =
        roundedCornerOverlayView?.loadWallpaper(imageUrl = imageUrl, isGif = true)

    fun resetTransform() {
        roundedCornerOverlayView?.resetTransform()
    }

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
        binding.layoutChargingBadge.visibility = if (visible) VISIBLE else GONE
    }

    fun setPreviewMode(isPreview: Boolean) {
        isPreviewMode = isPreview

        roundedCornerOverlayView?.setInteractionEnabled(!isPreview)

        setBadgeVisible(!isPreview)

        if (width == 0 || height == 0) {
            post { setPreviewMode(isPreview) }
            return
        }

    }

    fun applyCropInfo(cropInfo: CropInfo) = roundedCornerOverlayView?.applyCropInfo(cropInfo)

}


