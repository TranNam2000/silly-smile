package com.nomyek.myapplication.ui.edit.editgif

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nomyek.myapplication.R
import com.nomyek.myapplication.data.entity.HistoryEntity
import com.nomyek.myapplication.data.entity.WallpaperEntity
import com.nomyek.myapplication.data.model.CropInfo
import com.nomyek.myapplication.data.repository.Repository
import com.nomyek.myapplication.ui.main.MainActivity
import com.nomyek.myapplication.utils.GifUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditGitViewModel @Inject constructor(
    private val repository: Repository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private const val TAG = "EditGitViewModel"
    }

    private val _currentWallpaper = MutableStateFlow<WallpaperEntity?>(null)
    val currentWallpaper: StateFlow<WallpaperEntity?> = _currentWallpaper.asStateFlow()
    private val _wallpaperSetResult = MutableStateFlow<String?>(null)
    val wallpaperSetResult: StateFlow<String?> = _wallpaperSetResult.asStateFlow()
    
    private val _currentHistoryEntity = MutableStateFlow<HistoryEntity?>(null)
    val currentHistoryEntity: StateFlow<HistoryEntity?> = _currentHistoryEntity.asStateFlow()

    fun setWallpaper(wallpaper: WallpaperEntity) {
        _currentWallpaper.value = wallpaper
    }

    fun setHistoryEntity(historyEntity: HistoryEntity) {
        _currentHistoryEntity.value = historyEntity
    }

    fun setLiveWallpaper(
        activity: MainActivity,
        cropInfo: CropInfo,
        onHistoryEntity: (HistoryEntity) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val wallpaper = _currentWallpaper.value
                if (wallpaper == null) {
                    _wallpaperSetResult.value = context.getString(R.string.error_wallpaper_not_selected)
                    Log.e(TAG, "No wallpaper selected")
                    return@launch
                }

                val finalCropInfo = if (GifUtils.isOnlineUrl(cropInfo.originalPath)) {
                    _wallpaperSetResult.value = context.getString(R.string.downloading)

                    val cachedPath = GifUtils.downloadGifToCache(context, cropInfo.originalPath)
                    if (cachedPath == null) {
                        _wallpaperSetResult.value = context.getString(R.string.error_download_failed)
                        Log.e(TAG, "Failed to download GIF from: ${cropInfo.originalPath}")
                        return@launch
                    }

                    cropInfo.copy(originalPath = cachedPath)
                } else {
                    cropInfo
                }
                val historyEntity = HistoryEntity.fromWallpaperAndCropInfo(
                    wallpaper = wallpaper,
                    cropInfo = finalCropInfo
                )
                onHistoryEntity.invoke(historyEntity)
                GifUtils.setLiveGifWallpaper(activity, finalCropInfo)


            } catch (e: Exception) {
                Log.e(TAG, "Error setting live wallpaper", e)
                _wallpaperSetResult.value = context.getString(R.string.error_set_wallpaper_failed)
            }
        }
    }

}
