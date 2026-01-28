package com.nomyek.myapplication.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nomyek.myapplication.data.entity.WallpaperEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WallpaperDao {

    @Query("SELECT * FROM wallpapers WHERE isFavorite = 1 ORDER BY addedAt DESC")
    fun getFavoriteWallpapers(): Flow<List<WallpaperEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallpapers(wallpapers: List<WallpaperEntity>)

    @Query("UPDATE wallpapers SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: String, isFavorite: Boolean)

    @Query("UPDATE wallpapers SET timeOpen = :timeOpen WHERE id = :id")
    suspend fun updateTimeOpen(id: String, timeOpen: Long)

    @Query("SELECT COUNT(*) FROM wallpapers")
    suspend fun getWallpaperCount(): Int

    // ===== Query methods for BACKGROUND_ASSET =====

    @Query("SELECT * FROM wallpapers WHERE sourceType = 'BACKGROUND_ASSET'")
    fun getAllBackgroundAssets(): Flow<List<WallpaperEntity>>

    // ===== Query methods for WALLPAPER_ITEM =====
    @Query(
        """SELECT * FROM wallpapers 
    WHERE sourceType = 'WALLPAPER_ITEM'
    ORDER BY isAnimated DESC"""
    )
    fun getAllWallpaperItems(): Flow<List<WallpaperEntity>>
}

