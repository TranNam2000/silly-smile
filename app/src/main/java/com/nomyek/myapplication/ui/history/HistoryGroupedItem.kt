package com.nomyek.myapplication.ui.history

import com.nomyek.myapplication.data.entity.HistoryEntity

/**
 * Data class để represent một section ngày với danh sách wallpapers
 */
data class HistoryDateSection(
    val date: String,
    val formattedDate: String,
    val wallpapers: List<HistoryEntity>
)