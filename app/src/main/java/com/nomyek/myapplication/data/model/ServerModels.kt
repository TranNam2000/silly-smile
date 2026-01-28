package com.nomyek.myapplication.data.model

import com.google.gson.annotations.SerializedName

/**
 * Server API response models for wallpaper data
 */

data class ServerWallpaperItem(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("filename")
    val filename: String,
    
    @SerializedName("size_bytes")
    val sizeBytes: Long,
    
    @SerializedName("size_mb")
    val sizeMb: Double,
    
    @SerializedName("download_url")
    val downloadUrl: String
)

/**
 * API Response wrapper
 */
data class WallpaperApiResponse(
    @SerializedName("data")
    val data: List<ServerWallpaperItem>,
    
    @SerializedName("success")
    val success: Boolean = true,
    
    @SerializedName("message")
    val message: String? = null
)

/**
 * Extension functions to convert server models to local models
 */
fun ServerWallpaperItem.toWallpaperItem(): WallpaperItem {
    return WallpaperItem(
        id = this.name,
        order = this.id,
        staticImage = null,
        animatedGif = AnimatedGif(
            filename = this.filename,
            sourcePath = this.downloadUrl,
            type = "animated_gif",
            linked = true // Mark as linked to indicate it's from server
        )
    )
}

fun List<ServerWallpaperItem>.toWallpaperCollection(
    collectionName: String = "Silly Smile Collection",
    description: String = "Server-based wallpapers"
): WallpaperCollection {
    return WallpaperCollection(
        name = collectionName,
        description = description,
        folderPath = "server/",
        items = this.map { it.toWallpaperItem() }
    )
}