package com.nomyek.myapplication.data.entity

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nomyek.myapplication.data.model.BackgroundAsset
import com.nomyek.myapplication.data.model.WallpaperItem
import com.nomyek.myapplication.data.model.WallpaperSourceType
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "wallpapers")
data class WallpaperEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val category: String,
    val imageUrl: String,
    val previewUrl: String? = null,
    val sourceType: String,
    val isAnimated: Boolean = false,
    val localPath: String? = null,
    val isFavorite: Boolean = false,
    val addedAt: Long = System.currentTimeMillis(),
    val timeOpen: Long? = 0
) : Parcelable {
    companion object {
        fun fromBackgroundAsset(
            asset: BackgroundAsset,
        ): WallpaperEntity {
            return WallpaperEntity(
                id = "bg_${asset.id}",
                title = "${asset.category}_${asset.id}",
                category = asset.category,
                imageUrl = asset.background,
                previewUrl = asset.preview,
                sourceType = WallpaperSourceType.BACKGROUND_ASSET.name,
                isAnimated = false,
            )
        }

        fun fromWallpaperItem(
            item: WallpaperItem,
            category: String,
        ): WallpaperEntity {
            val imagePath =
                item.staticImage?.let { "${it.sourcePath.orEmpty()}${it.filename.orEmpty()}" }
                    ?: item.animatedGif?.let { "${it.sourcePath.orEmpty()}${it.filename.orEmpty()}" }
                    ?: ""
            val isAnimated = item.animatedGif != null

            return WallpaperEntity(
                id = "wp_${item.id}",
                title = item.id,
                category = category,
                imageUrl = imagePath,
                previewUrl = imagePath,
                sourceType = WallpaperSourceType.WALLPAPER_ITEM.name,
                isAnimated = isAnimated,
            )
        }

    }


    fun getDisplayUrl(): String {
        return previewUrl ?: imageUrl
    }


}



