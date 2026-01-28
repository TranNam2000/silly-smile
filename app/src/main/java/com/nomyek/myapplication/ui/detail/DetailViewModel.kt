package com.nomyek.myapplication.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nomyek.myapplication.data.entity.WallpaperEntity
import com.nomyek.myapplication.data.repository.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: Repository
) : ViewModel() {

    private val _currentWallpaper = MutableStateFlow<WallpaperEntity?>(null)
    val currentWallpaper: StateFlow<WallpaperEntity?> = _currentWallpaper.asStateFlow()

    private val _allWallpapers = MutableStateFlow<List<WallpaperEntity>>(emptyList())
    val allWallpapers: StateFlow<List<WallpaperEntity>> = _allWallpapers.asStateFlow()

    fun setCurrentWallpaper(wallpaper: WallpaperEntity) {
        _currentWallpaper.value = wallpaper
    }

    fun setWallpaperList(wallpapers: List<WallpaperEntity>) {
        _allWallpapers.value = wallpapers
    }

    fun toggleFavorite(wallpaper: WallpaperEntity, isFavorite: Boolean) {
        viewModelScope.launch {
            try {
                repository.toggleFavorite(wallpaper.id, isFavorite)
                
                if (_currentWallpaper.value?.id == wallpaper.id) {
                    _currentWallpaper.value = wallpaper.copy(isFavorite = isFavorite)
                }
                
                val updatedList = _allWallpapers.value.map { item ->
                    if (item.id == wallpaper.id) {
                        item.copy(isFavorite = isFavorite)
                    } else {
                        item
                    }
                }
                _allWallpapers.value = updatedList
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateTimeOpen(wallpaperId: String) {
        viewModelScope.launch {
            try {
                repository.updateTimeOpen(wallpaperId, System.currentTimeMillis())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}