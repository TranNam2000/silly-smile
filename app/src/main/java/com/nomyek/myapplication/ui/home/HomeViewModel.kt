package com.nomyek.myapplication.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nomyek.myapplication.data.entity.WallpaperEntity
import com.nomyek.myapplication.data.repository.Repository
import com.nomyek.myapplication.ui.home.poup.CategoryId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: Repository
) : ViewModel() {

    private val _wallpapers = MutableStateFlow<List<HomeListItem>>(emptyList())
    val wallpapers: StateFlow<List<HomeListItem>> = _wallpapers.asStateFlow()

    private val _selectedCategory = MutableStateFlow(CategoryId.ALL)
    val selectedCategory: StateFlow<CategoryId> = _selectedCategory.asStateFlow()
    
    private var collectJob: Job? = null

    init {
        initializeData()
        loadSmileWallpapers()
    }

    private fun initializeData() {
        viewModelScope.launch {
            try {
                val count = repository.getWallpaperCount()
                if (count == 0) {
                    repository.loadAllDataToDatabase()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadSmileWallpapers() {
        collectJob?.cancel()
        collectJob = viewModelScope.launch {
            try {
                repository.getAllWallpaperItems().collect { allWallpapers ->
                    _wallpapers.value = insertNativeAds(allWallpapers)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _wallpapers.value = emptyList()
            }
        }
    }
    

    fun load4KWallpapers() {
        collectJob?.cancel()
        collectJob = viewModelScope.launch {
            try {
                repository.getAllBackgroundAssets().collect { allWallpapers ->
                    _wallpapers.value = insertNativeAds(allWallpapers)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _wallpapers.value = emptyList()
            }
        }
    }


    fun toggleFavorite(wallpaper: WallpaperEntity, isFavorite: Boolean) {
        viewModelScope.launch {
            try {
                repository.toggleFavorite(wallpaper.id, isFavorite)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    

    
    fun filterByCategory(categoryId: CategoryId) {
        _selectedCategory.value = categoryId
        collectJob?.cancel()
        collectJob = viewModelScope.launch {
            try {
                repository.getAllBackgroundAssets().collect { allWallpapers ->
                    val filteredWallpapers = when (categoryId) {
                        CategoryId.ALL -> allWallpapers
                        else -> allWallpapers.filter { wallpaper ->
                            wallpaper.category?.lowercase() == categoryId.name.lowercase()
                        }
                    }
                    _wallpapers.value = insertNativeAds(filteredWallpapers)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _wallpapers.value = emptyList()
            }
        }
    }

    /**
     * Insert native ads after every 2 rows (4 wallpapers in grid layout with 2 columns)
     */
    private fun insertNativeAds(wallpapers: List<WallpaperEntity>): List<HomeListItem> {
        val result = mutableListOf<HomeListItem>()
        val itemsPerRow = 2
        val rowsBeforeAd = 2
        val itemsBeforeAd = itemsPerRow * rowsBeforeAd // 4 items before each ad

        wallpapers.forEachIndexed { index, wallpaper ->
            result.add(HomeListItem.WallpaperItem(wallpaper))
            
            // Insert native ad after every 4 items (2 rows)
            if ((index + 1) % itemsBeforeAd == 0 && index < wallpapers.size - 1) {
                result.add(HomeListItem.NativeAdItem)
            }
        }

        return result
    }
}
