package com.nomyek.myapplication.ui.preview

import android.app.WallpaperManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nomyek.myapplication.data.entity.HistoryEntity
import com.nomyek.myapplication.data.entity.WallpaperEntity
import com.nomyek.myapplication.data.model.CropInfo
import com.nomyek.myapplication.data.repository.Repository
import com.nomyek.myapplication.utils.GifUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PreviewWallpaperViewModel @Inject constructor(
    private val repository: Repository,
    @ApplicationContext val context: Context,
) : ViewModel() {

    private val _currentWallpaper = MutableStateFlow<WallpaperEntity?>(null)
    val currentWallpaper: StateFlow<WallpaperEntity?> = _currentWallpaper.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun setWallpaper(wallpaper: WallpaperEntity) {
        _currentWallpaper.value = wallpaper
    }

    private val _wallpaperSetResult = MutableStateFlow<String?>(null)
    val wallpaperSetResult: StateFlow<String?> = _wallpaperSetResult.asStateFlow()

    fun setStaticWallpaper(
        cropInfo: CropInfo,
        flags: Int = WallpaperManager.FLAG_SYSTEM,
        onHistoryEntity: (HistoryEntity) -> Unit

    ) {
        viewModelScope.launch {
            try {
                val wallpaper = _currentWallpaper.value
                if (wallpaper == null) {
                    _wallpaperSetResult.value = "Error: No wallpaper selected"
                    return@launch
                }

                val finalCropInfo = if (GifUtils.isOnlineUrl(cropInfo.originalPath)) {
                    _wallpaperSetResult.value = "Downloading..."

                    val cachedPath = GifUtils.downloadGifToCache(context, cropInfo.originalPath)
                    if (cachedPath == null) {
                        _wallpaperSetResult.value = "Error: Failed to download GIF"
                        return@launch
                    }

                    cropInfo.copy(originalPath = cachedPath)
                } else {
                    cropInfo
                }
                val historyEntity =
                    HistoryEntity.fromWallpaperAndCropInfo(
                        wallpaper = wallpaper,
                        cropInfo = finalCropInfo
                    )
                onHistoryEntity.invoke(historyEntity)
                GifUtils.setStaticGifWallpaper(
                    context = context,
                    cropInfo = finalCropInfo,
                    flags = flags
                )

            } catch (e: Exception) {
                e.printStackTrace()
                _wallpaperSetResult.value = "Error: ${e.message}"
            }
        }
    }
}

