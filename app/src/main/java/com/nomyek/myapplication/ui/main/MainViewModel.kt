package com.nomyek.myapplication.ui.main

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nomyek.myapplication.data.entity.HistoryEntity
import com.nomyek.myapplication.data.model.CropInfo
import com.nomyek.myapplication.data.repository.Repository
import com.nomyek.myapplication.utils.ImageUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

@HiltViewModel
class MainViewModel @Inject constructor(
    val repository: Repository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private var cropInfo: CropInfo? = null
    private var historyEntity: HistoryEntity? = null

    // Image processing states
    private val _isProcessingImage = MutableStateFlow(false)
    val isProcessingImage: StateFlow<Boolean> = _isProcessingImage

    private val _imageProcessResult = MutableStateFlow<Result<File>?>(null)
    val imageProcessResult: StateFlow<Result<File>?> = _imageProcessResult

    fun setCropInfo(cropInfo: CropInfo) {
        this.cropInfo = cropInfo
    }

    fun getCropInfo() = cropInfo

    fun addHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            historyEntity?.let { repository.insertHistory(it) }
        }
    }

    fun setHistory(historyEntity: HistoryEntity) {
        this.historyEntity = historyEntity
    }

    fun processSelectedImage(uri: Uri) {
        viewModelScope.launch {
            _isProcessingImage.value = true
            try {
                val cachedFile = ImageUtils.saveImageToCache(context, uri)
                _imageProcessResult.value = Result.success(cachedFile)
            } catch (e: Exception) {
                _imageProcessResult.value = Result.failure(e)
            } finally {
                _isProcessingImage.value = false
            }
        }
    }

    fun clearImageProcessResult() {
        _imageProcessResult.value = null
    }
}