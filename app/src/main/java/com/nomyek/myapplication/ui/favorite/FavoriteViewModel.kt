package com.nomyek.myapplication.ui.favorite

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
class FavoriteViewModel @Inject constructor(
    private val repository: Repository
) : ViewModel() {

    private val _favoriteWallpapers = MutableStateFlow<List<WallpaperEntity>>(emptyList())
    val favoriteWallpapers: StateFlow<List<WallpaperEntity>> = _favoriteWallpapers.asStateFlow()

    private val _isLoading = MutableStateFlow(false)

    init {
        loadFavoriteWallpapers()
    }

    private fun loadFavoriteWallpapers() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getFavoriteWallpapers().collect { wallpapers ->
                _favoriteWallpapers.value = wallpapers
                _isLoading.value = false
            }
        }
    }

    fun removeFavorite(wallpaperId: String) {
        viewModelScope.launch {
            repository.toggleFavorite(wallpaperId, false)
        }
    }
}