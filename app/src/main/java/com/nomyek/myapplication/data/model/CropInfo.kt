package com.nomyek.myapplication.data.model

import android.graphics.Rect
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CropInfo(
    val originalPath: String,
    val cropX: Int = 0,
    val cropY: Int = 0,
    val cropWidth: Int = 0,
    val cropHeight: Int = 0,
    val sourceImageWidth: Int = 0,
    val sourceImageHeight: Int = 0,
    val scaleFactor: Float = 1f,
    val translationX: Float = 0f,
    val translationY: Float = 0f,
    val sourceViewWidth: Int = 0,
    val sourceViewHeight: Int = 0
) : Parcelable {

    /**
     * Convert to Android Rect for WallpaperManager
     */
    fun toRect(): Rect {
        return Rect(cropX, cropY, cropX + cropWidth, cropY + cropHeight)
    }

}


