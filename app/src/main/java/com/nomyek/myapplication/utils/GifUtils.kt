package com.nomyek.myapplication.utils

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.nomyek.myapplication.R
import com.nomyek.myapplication.data.model.CropInfo
import com.nomyek.myapplication.service.GifLiveWallpaperService
import com.nomyek.myapplication.service.GifLiveWallpaperService2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL
import java.security.MessageDigest

/**
 * Utility để set GIF wallpaper
 * ⚡ Tối ưu: Dùng crop info thay vì encode lại GIF
 */
object GifUtils {

    private const val TAG = "GifUtils"
    private var useService1: Boolean = false

    /**
     * Set static GIF wallpaper (first frame only)
     * @param flags WallpaperManager.FLAG_SYSTEM (home), FLAG_LOCK (lock), or both
     */
    suspend fun setStaticGifWallpaper(
        context: Context,
        cropInfo: CropInfo,
        flags: Int = WallpaperManager.FLAG_SYSTEM
    ): Result<String> = withContext(Dispatchers.IO) {


        val wallpaperManager = WallpaperManager.getInstance(context)
        val finalFlags =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                flags
            } else {
                WallpaperManager.FLAG_SYSTEM
            }
        

        val inputStream = openGifInputStream(context, cropInfo.originalPath)
            ?: return@withContext Result.failure(
                Exception("Cannot open: ${cropInfo.originalPath}")
            )

        inputStream.use { stream ->
            if (cropInfo.cropWidth > 0 && cropInfo.cropHeight > 0) {
                wallpaperManager.setStream(stream, cropInfo.toRect(), true, finalFlags)
            } else {
                wallpaperManager.setStream(stream, null, true, finalFlags)
            }
        }

        val message = when {
            flags == WallpaperManager.FLAG_SYSTEM -> context.getString(R.string.wallpaper_set_home)
            flags == WallpaperManager.FLAG_LOCK -> context.getString(R.string.wallpaper_set_lock)
            (flags and (WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)) == (WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK) -> context.getString(
                R.string.wallpaper_set_both
            )

            else -> context.getString(R.string.wallpaper_set_success)
        }

        Result.success(message)

    }

    fun setLiveGifWallpaper(
        activity: AppCompatActivity,
        cropInfo: CropInfo
    ) {
        try {
            val wallpaperManager = WallpaperManager.getInstance(activity)
            val currentWallpaperInfo = wallpaperManager.wallpaperInfo
            val currentComponent = currentWallpaperInfo?.component

            Log.d(TAG, "Current wallpaper component: $currentComponent")

            val service1Component = ComponentName(activity, GifLiveWallpaperService::class.java)
            val service2Component = ComponentName(activity, GifLiveWallpaperService2::class.java)

            val serviceClass = when (currentComponent) {
                service1Component -> {
                    GifLiveWallpaperService2::class.java
                }

                service2Component -> {
                    Log.d(TAG, "Service2 is running, switching to Service1")
                    GifLiveWallpaperService::class.java
                }

                else -> {
                    val cls = if (useService1) {
                        GifLiveWallpaperService::class.java
                    } else {
                        GifLiveWallpaperService2::class.java
                    }
                    useService1 = !useService1
                    cls
                }
            }

            GifLiveWallpaperService.apply {
                currentGifPath = cropInfo.originalPath
                cropX = cropInfo.cropX
                cropY = cropInfo.cropY
                cropWidth = cropInfo.cropWidth
                cropHeight = cropInfo.cropHeight
                sourceImageWidth = cropInfo.sourceImageWidth
                sourceImageHeight = cropInfo.sourceImageHeight
                scaleFactor = cropInfo.scaleFactor
                translationX = cropInfo.translationX
                translationY = cropInfo.translationY
                sourceViewWidth = cropInfo.sourceViewWidth
                sourceViewHeight = cropInfo.sourceViewHeight
                isWallpaperSet = false
            }

            GifLiveWallpaperService2.apply {
                currentGifPath = cropInfo.originalPath
                cropX = cropInfo.cropX
                cropY = cropInfo.cropY
                cropWidth = cropInfo.cropWidth
                cropHeight = cropInfo.cropHeight
                sourceImageWidth = cropInfo.sourceImageWidth
                sourceImageHeight = cropInfo.sourceImageHeight
                scaleFactor = cropInfo.scaleFactor
                translationX = cropInfo.translationX
                translationY = cropInfo.translationY
                sourceViewWidth = cropInfo.sourceViewWidth
                sourceViewHeight = cropInfo.sourceViewHeight
                isWallpaperSet = false
            }

            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(activity, serviceClass)
                )
            }

            activity.startActivityForResult(intent, Constants.REQUEST_CODE_WALLPAPER_PICKER)

        } catch (e: Exception) {
            Log.e(TAG, "Error setting live wallpaper", e)
        }
    }


    suspend fun downloadGifToCache(
        context: Context,
        url: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            // Generate cache filename từ URL hash
            val fileName = url.md5() + ".gif"
            val cacheDir = getWallpaperCacheDir(context)
            val cacheFile = File(cacheDir, fileName)

            // Check nếu đã có trong cache
            if (cacheFile.exists()) {
                Log.d(TAG, "GIF already in cache: ${cacheFile.absolutePath}")
                return@withContext cacheFile.absolutePath
            }

            Log.d(TAG, "Downloading GIF from: $url")

            // Download file
            val connection = URL(url).openConnection()
            connection.connectTimeout = 30000
            connection.readTimeout = 30000

            connection.getInputStream().use { input ->
                FileOutputStream(cacheFile).use { output ->
                    input.copyTo(output)
                }
            }

            cacheFile.absolutePath

        } catch (e: Exception) {
            Log.e(TAG, "Error downloading GIF", e)
            null
        }
    }

    /**
     * Check nếu path là online URL
     */
    fun isOnlineUrl(path: String): Boolean {
        return path.startsWith("http://") || path.startsWith("https://")
    }

    /**
     * Generate MD5 hash cho string (dùng làm cache key)
     */
    private fun String.md5(): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Parse image path to proper format
     */
    fun parseImagePath(path: String): String {
        return when {
            path.startsWith("assets/") -> {
                "file:///android_asset/${path.removePrefix("assets/")}"
            }

            else -> path
        }
    }

    /**
     * Open GIF input stream from various sources
     */
    private fun openGifInputStream(context: Context, path: String): InputStream? {
        return try {
            when {
                path.startsWith("assets/") -> {
                    context.assets.open(path.removePrefix("assets/"))
                }

                path.startsWith("file:///android_asset/") -> {
                    context.assets.open(path.removePrefix("file:///android_asset/"))
                }

                path.startsWith("file://") -> {
                    FileInputStream(path.removePrefix("file://"))
                }

                else -> {
                    val file = File(path)
                    if (file.exists()) FileInputStream(file) else null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening GIF: $path", e)
            null
        }
    }

    /**
     * Get cache directory for wallpapers
     */
    private fun getWallpaperCacheDir(context: Context): File {
        val cacheDir = File(context.cacheDir, "wallpapers")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        return cacheDir
    }
}
