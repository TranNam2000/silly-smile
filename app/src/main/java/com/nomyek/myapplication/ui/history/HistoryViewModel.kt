package com.nomyek.myapplication.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nomyek.myapplication.data.entity.HistoryEntity
import com.nomyek.myapplication.data.entity.WallpaperEntity
import com.nomyek.myapplication.data.repository.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: Repository
) : ViewModel() {

    private val _historyWallpapers = MutableStateFlow<List<HistoryEntity>>(emptyList())
    val historyWallpapers: StateFlow<List<HistoryEntity>> = _historyWallpapers.asStateFlow()

    private val _isLoading = MutableStateFlow(false)

    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent: SharedFlow<NavigationEvent> = _navigationEvent.asSharedFlow()

    sealed class NavigationEvent {
        data class NavigateToEditGif(val historyEntity: HistoryEntity) : NavigationEvent()
        data class NavigateToEditImage(val historyEntity: HistoryEntity) : NavigationEvent()
    }

    init {
        loadHistoryWallpapers()
    }

    private fun loadHistoryWallpapers() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getAllHistory().collect { historyEntities ->
                    _historyWallpapers.value = historyEntities
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _isLoading.value = false
            }
        }
    }

    /**
     * Handle history item click - navigate to appropriate edit screen
     */
    fun onHistoryItemClick(historyEntity: HistoryEntity) {
        viewModelScope.launch {
            if (historyEntity.isAnimated) {
                _navigationEvent.emit(NavigationEvent.NavigateToEditGif(historyEntity))
            } else {
                _navigationEvent.emit(NavigationEvent.NavigateToEditImage(historyEntity))
            }
        }
    }
}

