package com.nomyek.myapplication.service

import android.graphics.Canvas
import android.graphics.Movie
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

@AndroidEntryPoint
class GifLiveWallpaperService2 : WallpaperService() {

    companion object {
        private const val TAG = "GifLiveWallpaper"
        var currentGifPath: String? = null
        var cropX: Int = 0
        var cropY: Int = 0
        var cropWidth: Int = 0
        var cropHeight: Int = 0
        var sourceImageWidth: Int = 0
        var sourceImageHeight: Int = 0
        var scaleFactor: Float = 1f
        var translationX: Float = 0f
        var translationY: Float = 0f
        var sourceViewWidth: Int = 0
        var sourceViewHeight: Int = 0

        var isWallpaperSet: Boolean = false
        private var engineInstance: Engine? = null

    }


    override fun onCreateEngine(): Engine {
        return GifWallpaperEngine()
    }

    inner class GifWallpaperEngine : Engine() {
        private var movie: Movie? = null
        private var movieStart: Long = 0
        private val handler = Handler(Looper.getMainLooper())
        private var isVisible = false
        private var gifDuration = 0

        // Cache để detect khi cần reload
        private var loadedGifPath: String? = null

        private val drawRunnable = Runnable {
            if (isVisible) {
                draw()
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            Log.d(TAG, "Engine onCreate - loading GIF")
            loadGif()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            Log.d(TAG, "onVisibilityChanged: $visible, isPreview: $isPreview")
            isVisible = visible
            if (visible && !isPreview) {
                isWallpaperSet = true
            } else if (visible && isPreview) {
                isWallpaperSet = false
            }
            if (visible) {
                // Check nếu GIF path đã thay đổi, reload
                if (loadedGifPath != currentGifPath) {
                    Log.d(
                        TAG,
                        "GIF path changed from '$loadedGifPath' to '$currentGifPath', reloading"
                    )
                    loadGif()
                }
                movieStart = System.currentTimeMillis()
                draw()
            } else {
                handler.removeCallbacks(drawRunnable)
            }
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder?,
            format: Int,
            width: Int,
            height: Int
        ) {
            super.onSurfaceChanged(holder, format, width, height)
            Log.d(TAG, "onSurfaceChanged: ${width}x${height}")
            // Check nếu GIF path đã thay đổi, reload
            if (loadedGifPath != currentGifPath) {
                Log.d(TAG, "GIF path changed on surface change, reloading")
                loadGif()
            }
            draw()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder?) {
            super.onSurfaceDestroyed(holder)
            Log.d(
                TAG, "onSurfaceDestroyed, is" +
                        "Preview: $isPreview"
            )
            isVisible = false
            handler.removeCallbacks(drawRunnable)
        }

        private fun loadGif() {
            try {
                val gifPath = currentGifPath
                if (gifPath == null) {
                    Log.e(TAG, "currentGifPath is null")
                    return
                }

                Log.d(TAG, "Loading GIF - path: $gifPath")
                Log.d(TAG, "Crop info - x:$cropX, y:$cropY, w:$cropWidth, h:$cropHeight")
                Log.d(
                    TAG,
                    "Source - imgW:$sourceImageWidth, imgH:$sourceImageHeight, viewW:$sourceViewWidth, viewH:$sourceViewHeight"
                )

                val inputStream = openGifInputStream(gifPath)
                if (inputStream == null) {
                    Log.e(TAG, "Failed to open input stream for: $gifPath")
                    return
                }

                inputStream.use {
                    movie = Movie.decodeStream(it)
                    gifDuration = movie?.duration() ?: 1000
                    loadedGifPath = gifPath
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading GIF", e)
            }
        }

        private fun openGifInputStream(path: String): InputStream? {
            return try {
                when {
                    path.startsWith("assets/") -> {
                        assets.open(path.removePrefix("assets/"))
                    }

                    path.startsWith("file:///android_asset/") -> {
                        assets.open(path.removePrefix("file:///android_asset/"))
                    }

                    path.startsWith("file://") -> {
                        FileInputStream(path.removePrefix("file://"))
                    }

                    else -> {
                        val file = File(path)
                        if (file.exists()) {
                            FileInputStream(file)
                        } else {
                            Log.e(TAG, "File does not exist: $path")
                            null
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error opening GIF: $path", e)
                null
            }
        }

        private fun draw() {
            val holder = surfaceHolder
            if (holder == null || !holder.surface.isValid) {
                Log.w(TAG, "Surface holder is null or invalid, skipping draw")
                return
            }

            var canvas: Canvas? = null

            try {
                canvas = holder.lockCanvas()
                if (canvas == null) {
                    Log.w(TAG, "Canvas is null after lockCanvas(), skipping draw")
                    return
                }
                drawGif(canvas)
            } catch (e: Exception) {
                Log.e(TAG, "Error drawing", e)
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (e: Throwable) {
                        Log.e(TAG, "Error unlocking canvas", e)
                    }
                }
            }

            // Schedule next frame
            handler.removeCallbacks(drawRunnable)
            if (isVisible) {
                handler.postDelayed(drawRunnable, 40) // ~25 FPS
            }
        }

        private fun drawGif(canvas: Canvas) {
            try {
                val movie = this.movie
                if (movie == null) {
                    Log.w(TAG, "Movie is null, cannot draw")
                    return
                }

                val now = System.currentTimeMillis()
                if (movieStart == 0L) movieStart = now
                movie.setTime(((now - movieStart) % gifDuration).toInt())

                canvas.save()

                val canvasWidth = canvas.width.toFloat()
                val canvasHeight = canvas.height.toFloat()
                val hasCrop = cropWidth > 0 && cropHeight > 0

                if (hasCrop) {
                    val movieWidth = movie.width()
                    val movieHeight = movie.height()

                    val scaleRatioX = if (sourceImageWidth > 0) {
                        movieWidth.toFloat() / sourceImageWidth
                    } else 1f
                    val scaleRatioY = if (sourceImageHeight > 0) {
                        movieHeight.toFloat() / sourceImageHeight
                    } else 1f

                    val actualCropX = (cropX * scaleRatioX).toInt()
                    val actualCropY = (cropY * scaleRatioY).toInt()
                    val actualCropWidth = (cropWidth * scaleRatioX).toInt()
                    val actualCropHeight = (cropHeight * scaleRatioY).toInt()


                    // Scale cropped region to fill canvas
                    val scale = maxOf(
                        canvasWidth / actualCropWidth,
                        canvasHeight / actualCropHeight
                    )

                    // Center the scaled content
                    val scaledWidth = actualCropWidth * scale
                    val scaledHeight = actualCropHeight * scale
                    val offsetX = (canvasWidth - scaledWidth) / 2f
                    val offsetY = (canvasHeight - scaledHeight) / 2f

                    // Apply transformations
                    canvas.translate(offsetX, offsetY)
                    canvas.scale(scale, scale)
                    canvas.translate(-actualCropX.toFloat(), -actualCropY.toFloat())
                } else {
                    // No crop - center and scale to fill
                    val movieWidth = movie.width().toFloat()
                    val movieHeight = movie.height().toFloat()

                    val scale = maxOf(
                        canvasWidth / movieWidth,
                        canvasHeight / movieHeight
                    )

                    val scaledWidth = movieWidth * scale
                    val scaledHeight = movieHeight * scale
                    val offsetX = (canvasWidth - scaledWidth) / 2f
                    val offsetY = (canvasHeight - scaledHeight) / 2f

                    canvas.translate(offsetX, offsetY)
                    canvas.scale(scale, scale)
                }

                movie.draw(canvas, 0f, 0f)
                canvas.restore()
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            handler.removeCallbacks(drawRunnable)
            movie = null
            loadedGifPath = null
        }
    }
}