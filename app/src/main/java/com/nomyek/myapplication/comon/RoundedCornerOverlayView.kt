package com.nomyek.myapplication.comon

import android.content.Context
import android.graphics.Matrix
import android.graphics.Outline
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import androidx.constraintlayout.utils.widget.ImageFilterView
import androidx.core.view.GestureDetectorCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.nomyek.myapplication.R
import com.nomyek.myapplication.data.model.CropInfo
import com.nomyek.myapplication.utils.GifUtils.parseImagePath
import com.nomyek.myapplication.utils.dpToPx
import kotlin.math.max
import kotlin.math.min

class RoundedCornerOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val cornerRadius: Float = 36.dpToPx(context.resources).toFloat()

    // Gesture detectors
    private val gestureDetector: GestureDetectorCompat
    private val scaleGestureDetector: ScaleGestureDetector
    private var originalPath: String? = null


    private var scaleFactor = 1.0f
    private var posX = 0f
    private var posY = 0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    private var isInteractionEnabled = true
    private var isApplyingCropInfo =
        false  // Flag để skip applyInitialScale khi đang apply cropInfo

    val imageView: ImageFilterView

    init {
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                val radius = min(cornerRadius, min(view.width, view.height) / 2f)
                outline.setRoundRect(0, 0, view.width, view.height, radius)
            }
        }

        clipToOutline = true
        isClickable = true
        isFocusable = true

        imageView = LayoutInflater.from(context).inflate(
            R.layout.item_wallpaper_image_in_overlay,
            this,
            false
        ) as ImageFilterView

        imageView.scaleType = android.widget.ImageView.ScaleType.MATRIX

        addView(imageView)

        gestureDetector = GestureDetectorCompat(context, GestureListener())
        scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        invalidateOutline()
    }

    fun loadWallpaper(imageUrl: String, isGif: Boolean = false) {
        originalPath = parseImagePath(imageUrl)
        
        // Check if context is an Activity and if it's destroyed
        if (context is android.app.Activity) {
            val activity = context as android.app.Activity
            if (activity.isFinishing || activity.isDestroyed) {
                Log.e("RoundedCornerOverlay", "Cannot load wallpaper: Activity is destroyed")
                return
            }
        }
        
        try {
            if (isGif) {
                Glide.with(context)
                    .asGif()
                    .load(originalPath)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(object :
                        CustomTarget<com.bumptech.glide.load.resource.gif.GifDrawable>() {
                        override fun onResourceReady(
                            resource: com.bumptech.glide.load.resource.gif.GifDrawable,
                            transition: Transition<in com.bumptech.glide.load.resource.gif.GifDrawable>?
                        ) {
                            imageView.setImageDrawable(resource)
                            applyInitialScale()
                            resource.start()
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                            imageView.setImageDrawable(placeholder)
                        }
                    })

            } else {
                Glide.with(context)
                    .load(originalPath)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(object :
                        CustomTarget<Drawable>() {
                        override fun onResourceReady(
                            resource: Drawable,
                            transition: Transition<in Drawable>?
                        ) {
                            imageView.setImageDrawable(resource)
                            applyInitialScale()
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                            imageView.setImageDrawable(placeholder)
                        }
                    })
            }
        } catch (e: IllegalArgumentException) {
            Log.e("RoundedCornerOverlay", "Error loading wallpaper with Glide: ${e.message}")
        } catch (e: Exception) {
            Log.e("RoundedCornerOverlay", "Unexpected error loading wallpaper: ${e.message}")
        }
    }

    private fun applyInitialScale() {
        // Skip nếu đang apply cropInfo
        if (isApplyingCropInfo) {
            Log.d("RoundedCornerOverlay", "Skipping applyInitialScale - applying cropInfo")
            return
        }

        if (width == 0 || height == 0) {
            post { applyInitialScale() }
            return
        }

        val drawable = imageView.drawable ?: run {
            // Nếu chưa có drawable, đợi một chút rồi thử lại
            postDelayed({ applyInitialScale() }, 50)
            return
        }

        val imageWidth = drawable.intrinsicWidth
        val imageHeight = drawable.intrinsicHeight

        // Kiểm tra ảnh đã load xong chưa
        if (imageWidth <= 0 || imageHeight <= 0 ||
            imageWidth == Int.MAX_VALUE || imageHeight == Int.MAX_VALUE
        ) {
            // Đợi ảnh load xong
            postDelayed({ applyInitialScale() }, 50)
            return
        }

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        val imageRatio = imageWidth.toFloat() / imageHeight

        // Tính baseScale: ảnh dọc -> full height, ảnh ngang -> full width
        val baseScale = if (imageRatio < 1f) {
            // Ảnh dọc (height > width) -> full height
            viewHeight / imageHeight
        } else {
            // Ảnh ngang (width >= height) -> full width
            viewWidth / imageWidth
        }

        // scaleFactor là giá trị tương đối, ban đầu = 1.0 để fill view
        scaleFactor = 1.0f
        posX = 0f
        posY = 0f

        // Đảm bảo ImageView scaleType là matrix để có thể transform thủ công
        imageView.scaleType = android.widget.ImageView.ScaleType.MATRIX

        // Apply transform bằng Matrix để đảm bảo scale được áp dụng
        applyTransformWithMatrix(baseScale * scaleFactor, posX, posY)

        // Force invalidate để đảm bảo view được redraw
        imageView.invalidate()

        Log.d("RoundedCornerOverlay", "applyInitialScale completed: scaleFactor=$scaleFactor")
    }

    fun getCropInfo(): CropInfo {
        val imageView = imageView
            ?: throw Exception("No imageView in RoundedCornerOverlayView")
        val drawable = imageView.drawable
            ?: throw Exception("No drawable loaded")

        val imageWidth = drawable.intrinsicWidth
        val imageHeight = drawable.intrinsicHeight
        val transformState = getTransformState()

        val cropRect = calculateCropRect(
            viewWidth = this.width,
            viewHeight = this.height,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            scaleFactor = transformState.scaleX,
            posX = transformState.translationX,
            posY = transformState.translationY
        )

        return CropInfo(
            originalPath = originalPath ?: "",
            cropX = cropRect.left,
            cropY = cropRect.top,
            cropWidth = cropRect.width(),
            cropHeight = cropRect.height(),
            sourceImageWidth = imageWidth,
            sourceImageHeight = imageHeight,
            scaleFactor = transformState.scaleX,
            translationX = transformState.translationX,
            translationY = transformState.translationY,
            sourceViewWidth = this.width,
            sourceViewHeight = this.height
        )
    }

    private fun calculateCropRect(
        viewWidth: Int,
        viewHeight: Int,
        imageWidth: Int,
        imageHeight: Int,
        scaleFactor: Float,
        posX: Float,
        posY: Float
    ): Rect {
        val imageRatio = imageWidth.toFloat() / imageHeight

        // Tính baseScale: ảnh dọc -> full height, ảnh ngang -> full width
        val baseScale = if (imageRatio < 1f) {
            // Ảnh dọc (height > width) -> full height
            viewHeight.toFloat() / imageHeight
        } else {
            viewWidth.toFloat() / imageWidth
        }

        val totalScale = baseScale * scaleFactor

        val visibleWidth = viewWidth / totalScale
        val visibleHeight = viewHeight / totalScale

        val centerX = imageWidth / 2f
        val centerY = imageHeight / 2f

        val panX = -posX / totalScale
        val panY = -posY / totalScale

        val left = (centerX - visibleWidth / 2f + panX)
            .coerceIn(0f, (imageWidth - visibleWidth).coerceAtLeast(0f))
        val top = (centerY - visibleHeight / 2f + panY)
            .coerceIn(0f, (imageHeight - visibleHeight).coerceAtLeast(0f))

        return Rect(
            left.toInt(),
            top.toInt(),
            (left + visibleWidth).toInt().coerceAtMost(imageWidth),
            (top + visibleHeight).toInt().coerceAtMost(imageHeight)
        )
    }


    /**
     * Enable/disable interaction (zoom, drag)
     */

    fun setInteractionEnabled(enabled: Boolean) {
        isInteractionEnabled = enabled
        isClickable = enabled
        isFocusable = enabled
    }

    /**
     * Reset transform
     */
    fun resetTransform() {
        scaleFactor = 1.0f
        posX = 0f
        posY = 0f
        applyTransform()
    }

    /**
     * Get current transform state
     */
    fun getTransformState(): TransformState {
        return TransformState(
            scaleX = scaleFactor,
            scaleY = scaleFactor,
            translationX = posX,
            translationY = posY
        )
    }

    fun applyCropInfo(cropInfo: CropInfo) {

        if (imageView == null) {
            Log.e("RoundedCornerOverlay", "imageView is null")
            return
        }

        if (width == 0 || height == 0) {
            Log.d("RoundedCornerOverlay", "View not laid out yet, retrying...")
            post { applyCropInfo(cropInfo) }
            return
        }

        val drawable = imageView.drawable
        if (drawable == null) {
            Log.d("RoundedCornerOverlay", "Drawable not loaded yet, retrying...")
            postDelayed({ applyCropInfo(cropInfo) }, 100)
            return
        }

        val imgWidth = drawable.intrinsicWidth
        val imgHeight = drawable.intrinsicHeight

        if (imgWidth <= 0 || imgHeight <= 0 || imgWidth == Int.MAX_VALUE || imgHeight == Int.MAX_VALUE) {
            Log.d("RoundedCornerOverlay", "Image not ready, retrying...")
            postDelayed({ applyCropInfo(cropInfo) }, 100)
            return
        }

        isApplyingCropInfo = true
        val currentViewW = width.toFloat()
        val currentViewH = height.toFloat()
        val sourceViewW =
            if (cropInfo.sourceViewWidth > 0) cropInfo.sourceViewWidth.toFloat() else currentViewW
        val sourceViewH =
            if (cropInfo.sourceViewHeight > 0) cropInfo.sourceViewHeight.toFloat() else currentViewH

        val imgRatio = imgWidth.toFloat() / imgHeight
        val sourceBaseScale = if (imgRatio < 1f) {
            sourceViewH / imgHeight  // Ảnh dọc -> fit height
        } else {
            sourceViewW / imgWidth   // Ảnh ngang -> fit width
        }

        val currentBaseScale = if (imgRatio < 1f) {
            currentViewH / imgHeight
        } else {
            currentViewW / imgWidth
        }

        val sourceTotalScale = sourceBaseScale * cropInfo.scaleFactor
        val newScaleFactor = (sourceTotalScale / currentBaseScale).coerceIn(1.0f, 3.0f)
        val currentTotalScale = currentBaseScale * newScaleFactor
        val scaleRatio = currentTotalScale / sourceTotalScale

        val newPosX = cropInfo.translationX * scaleRatio
        val newPosY = cropInfo.translationY * scaleRatio


        scaleFactor = newScaleFactor
        posX = newPosX
        posY = newPosY
        applyTransform()
        isApplyingCropInfo = false
    }


    /**
     * Set transform state
     */
    fun setTransformState(state: TransformState) {
        scaleFactor = state.scaleX
        posX = state.translationX
        posY = state.translationY
        applyTransform()
    }

    /**
     * Apply transform using Matrix
     */
    private fun applyTransformWithMatrix(scale: Float, translateX: Float, translateY: Float) {
        val matrix = Matrix()
        val drawable = imageView.drawable ?: return

        val imageWidth = drawable.intrinsicWidth.toFloat()
        val imageHeight = drawable.intrinsicHeight.toFloat()
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        if (imageWidth <= 0 || imageHeight <= 0) return

        // Center của image
        val imageCenterX = imageWidth / 2f
        val imageCenterY = imageHeight / 2f

        // Center của view
        val viewCenterX = viewWidth / 2f
        val viewCenterY = viewHeight / 2f

        // Reset matrix
        matrix.reset()

        // Scale từ center của image
        matrix.postScale(scale, scale, imageCenterX, imageCenterY)

        // Translate để center image vào center view, sau đó apply pan
        matrix.postTranslate(
            viewCenterX - imageCenterX + translateX,
            viewCenterY - imageCenterY + translateY
        )

        imageView.imageMatrix = matrix
    }

    private fun applyTransform() {
        val drawable = imageView.drawable
        if (drawable != null && width > 0 && height > 0) {
            val imageWidth = drawable.intrinsicWidth
            val imageHeight = drawable.intrinsicHeight

            if (imageWidth > 0 && imageHeight > 0) {
                val viewWidth = width.toFloat()
                val viewHeight = height.toFloat()
                val imageRatio = imageWidth.toFloat() / imageHeight

                val baseScale = if (imageRatio < 1f) {
                    // Ảnh dọc (height > width) -> full height
                    viewHeight / imageHeight
                } else {

                    viewWidth / imageWidth
                }

                applyTransformWithMatrix(baseScale * scaleFactor, posX, posY)
                return
            }
        }

        imageView.apply {
            scaleX = scaleFactor
            scaleY = scaleFactor
            translationX = posX
            translationY = posY
        }
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        if (!isInteractionEnabled) {
            return false
        }

        if (event.pointerCount > 1) {
            parent?.requestDisallowInterceptTouchEvent(true)
            return true
        }
        return super.onInterceptTouchEvent(event)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isInteractionEnabled) {
            return false
        }

        gestureDetector.onTouchEvent(event)
        var handled = scaleGestureDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                lastTouchX = event.x
                lastTouchY = event.y
                handled = true
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                handled = true
            }

            MotionEvent.ACTION_MOVE -> {
                if (!scaleGestureDetector.isInProgress && event.pointerCount == 1) {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY

                    val newPosX = posX + dx
                    val newPosY = posY + dy

                    val drawable = imageView.drawable
                    if (drawable != null) {
                        val viewWidth = width.toFloat()
                        val viewHeight = height.toFloat()
                        val imageWidth = drawable.intrinsicWidth.toFloat()
                        val imageHeight = drawable.intrinsicHeight.toFloat()
                        val imageRatio = imageWidth / imageHeight

                        val baseScale = if (imageRatio < 1f) {
                            // Ảnh dọc (height > width) -> full height
                            viewHeight / imageHeight
                        } else {
                            // Ảnh ngang (width >= height) -> full width
                            viewWidth / imageWidth
                        }
                        val totalScale = baseScale * scaleFactor
                        val scaledImageWidth = imageWidth * totalScale
                        val scaledImageHeight = imageHeight * totalScale
                        val maxTranslateX = if (scaledImageWidth > viewWidth) {
                            (scaledImageWidth - viewWidth) / 2f
                        } else {
                            0f
                        }

                        val maxTranslateY = if (scaledImageHeight > viewHeight) {
                            (scaledImageHeight - viewHeight) / 2f
                        } else {
                            0f
                        }

                        // Giới hạn translation để ảnh luôn sát viền
                        posX = newPosX.coerceIn(-maxTranslateX, maxTranslateX)
                        posY = newPosY.coerceIn(-maxTranslateY, maxTranslateY)
                    } else {
                        posX = newPosX
                        posY = newPosY
                    }

                    applyTransform()
                    lastTouchX = event.x
                    lastTouchY = event.y
                    handled = true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                handled = true
            }
        }

        return handled || super.onTouchEvent(event)
    }

    /**
     * Gesture listener for double tap (reset)
     */
    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            resetTransform()
            return true
        }
    }

    /**
     * Scale gesture listener for pinch zoom
     */
    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val oldScaleFactor = scaleFactor
            scaleFactor *= detector.scaleFactor
            // Cho phép zoom nhưng không quá mức ban đầu (chỉ zoom in, không zoom out)
            // scaleFactor >= 1.0f để đảm bảo ảnh luôn full view
            scaleFactor = max(1.0f, min(scaleFactor, 3.0f))
            Log.d(
                "RoundedCornerOverlay",
                "onScale: $oldScaleFactor -> $scaleFactor (detector=${detector.scaleFactor})"
            )
            applyTransform()
            return true
        }
    }

    /**
     * Data class để lưu transform state
     */
    data class TransformState(
        val scaleX: Float,
        val scaleY: Float,
        val translationX: Float,
        val translationY: Float
    )
}
