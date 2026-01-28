package com.nomyek.myapplication.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nomyek.myapplication.data.dao.HistoryDao
import com.nomyek.myapplication.data.dao.WallpaperDao
import com.nomyek.myapplication.data.entity.HistoryEntity
import com.nomyek.myapplication.data.entity.WallpaperEntity

@Database(
    entities = [
        WallpaperEntity::class,
        HistoryEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun wallpaperDao(): WallpaperDao
    abstract fun historyDao(): HistoryDao
    
    companion object {
        const val DATABASE_NAME = "wallpaper_database"
    }
}

