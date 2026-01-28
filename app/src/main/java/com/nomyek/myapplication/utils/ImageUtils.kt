package com.nomyek.myapplication.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object ImageUtils {
    
    suspend fun saveImageToCache(context: Context, uri: Uri): File = withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, "selected_images")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        
        // Generate unique filename
        val timestamp = System.currentTimeMillis()
        val cachedFile = File(cacheDir, "selected_image_$timestamp.jpg")
        
        // Copy image from URI to cache with optimization
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            // First, get image dimensions without loading full bitmap
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            
            // Calculate sample size to reduce memory usage
            options.inSampleSize = calculateInSampleSize(options, Constants.IMAGE_TARGET_WIDTH, Constants.IMAGE_TARGET_HEIGHT)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.RGB_565 // Use less memory
            
            // Reopen stream and decode with sample size
            context.contentResolver.openInputStream(uri)?.use { newInputStream ->
                val bitmap = BitmapFactory.decodeStream(newInputStream, null, options)
                    ?: throw Exception("Cannot decode bitmap")
                
                try {
                    // Save compressed bitmap to cache
                    FileOutputStream(cachedFile).use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, Constants.IMAGE_COMPRESSION_QUALITY, outputStream)
                    }
                } finally {
                    bitmap.recycle()
                }
            } ?: throw Exception("Cannot reopen input stream")
            
        } ?: throw Exception("Cannot open input stream from URI")
        
        cachedFile
    }
    
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            
            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        
        return inSampleSize
    }
}