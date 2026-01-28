package com.nomyek.myapplication.data.repository

import com.nomyek.myapplication.data.dao.HistoryDao
import com.nomyek.myapplication.data.dao.WallpaperDao
import com.nomyek.myapplication.data.entity.HistoryEntity
import com.nomyek.myapplication.data.entity.WallpaperEntity
import com.nomyek.myapplication.data.source.WallpaperDataSource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Repository @Inject constructor(
    private val dataSource: WallpaperDataSource,
    private val wallpaperDao: WallpaperDao,
    private val historyDao: HistoryDao
) {

    suspend fun loadAllWallpaperItemsToDatabase() {
        val items = dataSource.getAllWallpaperItems()
        val entities = items.map { (category, item) ->
            WallpaperEntity.fromWallpaperItem(item, category)
        }
        wallpaperDao.insertWallpapers(entities)
    }

    suspend fun loadAllBackgroundAssetsToDatabase() {
        val assets = dataSource.getAllBackgroundAssets()
        val entities = assets.map { asset ->
            WallpaperEntity.fromBackgroundAsset(asset)
        }
        wallpaperDao.insertWallpapers(entities)
    }
    suspend fun loadAllDataToDatabase() {
        loadAllWallpaperItemsToDatabase()
        loadAllBackgroundAssetsToDatabase()
    }
    // ===== Query methods =====
    
    fun getFavoriteWallpapers(): Flow<List<WallpaperEntity>> {
        return wallpaperDao.getFavoriteWallpapers()
    }
    
    // ===== Update methods =====
    
    suspend fun toggleFavorite(id: String, isFavorite: Boolean) {
        wallpaperDao.updateFavoriteStatus(id, isFavorite)
    }
    
    // ===== Count & Stats methods =====
    
    suspend fun getWallpaperCount(): Int {
        return wallpaperDao.getWallpaperCount()
    }
    
    // ===== Query methods for BACKGROUND_ASSET =====
    
    fun getAllBackgroundAssets(): Flow<List<WallpaperEntity>> {
        return wallpaperDao.getAllBackgroundAssets()
    }
    
    // ===== Query methods for WALLPAPER_ITEM =====
    
    fun getAllWallpaperItems(): Flow<List<WallpaperEntity>> {
        return wallpaperDao.getAllWallpaperItems()
    }
    
    // ===== Query methods for HISTORY =====
    
    suspend fun updateTimeOpen(wallpaperId: String, timeOpen: Long) {
        wallpaperDao.updateTimeOpen(wallpaperId, timeOpen)
    }
    
    // ===== History methods =====
    
    /**
     * Insert wallpaper history
     */
    suspend fun insertHistory(history: HistoryEntity): Long {
        return historyDao.insert(history)
    }
    
    /**
     * Get all history
     */
    fun getAllHistory(): Flow<List<HistoryEntity>> {
        return historyDao.getAllHistory()
    }
    

}
