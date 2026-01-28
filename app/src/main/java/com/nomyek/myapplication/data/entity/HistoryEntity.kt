package com.nomyek.myapplication.data.entity

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nomyek.myapplication.data.model.CropInfo
import kotlinx.parcelize.Parcelize

/**
 * Entity lưu lịch sử wallpaper đã set
 */
@Parcelize
@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val originalPath: String,
    val title: String,
    val category: String,
    val cropX: Int,
    val cropY: Int,
    val cropWidth: Int,
    val cropHeight: Int,
    val sourceImageWidth: Int,
    val sourceImageHeight: Int,
    val scaleFactor: Float = 1f,
    val translationX: Float = 0f,
    val translationY: Float = 0f,
    val sourceViewWidth: Int = 0,
    val sourceViewHeight: Int = 0,
    val isAnimated : Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable {

    /**
     * Convert to CropInfo
     */
    fun toCropInfo(): CropInfo {
        return CropInfo(
            originalPath = originalPath,
            cropX = cropX,
            cropY = cropY,
            cropWidth = cropWidth,
            cropHeight = cropHeight,
            sourceImageWidth = sourceImageWidth,
            sourceImageHeight = sourceImageHeight,
            scaleFactor = scaleFactor,
            translationX = translationX,
            translationY = translationY,
            sourceViewWidth = sourceViewWidth,
            sourceViewHeight = sourceViewHeight
        )
    }

    companion object {
        /**
         * Create from WallpaperEntity and CropInfo
         */
        fun fromWallpaperAndCropInfo(
            wallpaper: WallpaperEntity,
            cropInfo: CropInfo,
        ): HistoryEntity {
            return HistoryEntity(
                originalPath = cropInfo.originalPath.ifBlank { wallpaper.imageUrl },
                title = wallpaper.title,
                category = wallpaper.category,
                cropX = cropInfo.cropX,
                cropY = cropInfo.cropY,
                cropWidth = cropInfo.cropWidth,
                cropHeight = cropInfo.cropHeight,
                sourceImageWidth = cropInfo.sourceImageWidth,
                sourceImageHeight = cropInfo.sourceImageHeight,
                scaleFactor = cropInfo.scaleFactor,
                translationX = cropInfo.translationX,
                translationY = cropInfo.translationY,
                isAnimated = wallpaper.isAnimated,
                sourceViewWidth = cropInfo.sourceViewWidth,
                sourceViewHeight = cropInfo.sourceViewHeight
            )
        }
    }
}

