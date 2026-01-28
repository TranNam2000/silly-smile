package com.nomyek.myapplication.data.model

import com.google.gson.annotations.SerializedName


data class WallpapersData(
    @SerializedName("app_info")
    val appInfo: AppInfo,
    
    @SerializedName("wallpapers")
    val wallpapers: Wallpapers,
    
    @SerializedName("statistics")
    val statistics: Statistics
)

data class AppInfo(
    @SerializedName("name")
    val name: String,
    
    @SerializedName("package")
    val packageName: String,
    
    @SerializedName("version")
    val version: String
)

data class Wallpapers(
    @SerializedName("silly_smile")
    val sillySmile: WallpaperCollection,
    
    @SerializedName("evil_smile")
    val evilSmile: WallpaperCollection,
    
    @SerializedName("scary_face")
    val scaryFace: WallpaperCollection,
    
    @SerializedName("funky_smile")
    val funkySmile: WallpaperCollection
)


data class WallpaperCollection(
    @SerializedName("name")
    val name: String,
    
    @SerializedName("description")
    val description: String,
    
    @SerializedName("folder_path")
    val folderPath: String,
    
    @SerializedName("items")
    val items: List<WallpaperItem>
)


data class WallpaperItem(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("order")
    val order: Int,
    
    @SerializedName("static_image")
    val staticImage: StaticImage?,
    
    @SerializedName("animated_gif")
    val animatedGif: AnimatedGif?
)

/**
 * Static image info
 */
data class StaticImage(
    @SerializedName("filename")
    val filename: String,
    
    @SerializedName("source_path")
    val sourcePath: String,
    
    @SerializedName("type")
    val type: String
)

/**
 * Animated GIF info
 */
data class AnimatedGif(
    @SerializedName("filename")
    val filename: String,
    
    @SerializedName("source_path")
    val sourcePath: String,
    
    @SerializedName("type")
    val type: String,
    
    @SerializedName("linked")
    val linked: Boolean
)

/**
 * Statistics info
 */
data class Statistics(
    @SerializedName("total_collections")
    val totalCollections: Int,
    
    @SerializedName("total_items")
    val totalItems: Int,
    
    @SerializedName("items_with_animation")
    val itemsWithAnimation: Int,
    
    @SerializedName("items_static_only")
    val itemsStaticOnly: Int,
    
    @SerializedName("collections")
    val collections: Map<String, CollectionStats>
)

/**
 * Stats cho từng collection
 */
data class CollectionStats(
    @SerializedName("total")
    val total: Int,
    
    @SerializedName("with_animation")
    val withAnimation: Int,
    
    @SerializedName("static_only")
    val staticOnly: Int
)

