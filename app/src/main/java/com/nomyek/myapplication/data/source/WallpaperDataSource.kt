package com.nomyek.myapplication.data.source

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nomyek.myapplication.R
import com.nomyek.myapplication.data.model.BackgroundAsset
import com.nomyek.myapplication.data.model.WallpaperItem
import com.nomyek.myapplication.data.model.WallpapersData
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WallpaperDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {

    // Lazy load data JSON
    private val wallpapersData: WallpapersData by lazy {
        loadWallpapersData()
    }

    private val backgroundAssets: List<BackgroundAsset> by lazy {
        loadBackgroundAssets()
    }

    private fun loadWallpapersData(): WallpapersData {
        val jsonString = context.resources
            .openRawResource(R.raw.wallpapers)
            .bufferedReader()
            .use { it.readText() }

        return gson.fromJson(jsonString, WallpapersData::class.java)
    }

    fun getAllWallpaperItems(): List<Pair<String, WallpaperItem>> {
        val result = mutableListOf<Pair<String, WallpaperItem>>()

        wallpapersData.wallpapers.sillySmile.items.forEach { item ->
            result.add(Pair(wallpapersData.wallpapers.sillySmile.name, item))
        }

        wallpapersData.wallpapers.evilSmile.items.forEach { item ->
            result.add(Pair(wallpapersData.wallpapers.evilSmile.name, item))
        }

        wallpapersData.wallpapers.scaryFace.items.forEach { item ->
            result.add(Pair(wallpapersData.wallpapers.scaryFace.name, item))
        }

        wallpapersData.wallpapers.funkySmile.items.forEach { item ->
            result.add(Pair(wallpapersData.wallpapers.funkySmile.name, item))
        }

        return result
    }

    private fun loadBackgroundAssets(): List<BackgroundAsset> {
        val jsonString = context.resources
            .openRawResource(R.raw.background_asset)
            .bufferedReader()
            .use { it.readText() }

        val type = object : TypeToken<List<BackgroundAsset>>() {}.type
        return gson.fromJson(jsonString, type)
    }


    fun getAllBackgroundAssets(): List<BackgroundAsset> {
        return backgroundAssets
    }


}

