package com.nomyek.myapplication.ui.history

import com.nomyek.myapplication.data.entity.HistoryEntity
import com.nomyek.myapplication.utils.DateUtils

/**
 * Extension functions cho History
 */

/**
 * Nhóm danh sách HistoryEntity theo ngày và tạo list HistoryDateSection
 */
fun List<HistoryEntity>.toDateSections(): List<HistoryDateSection> {
    if (isEmpty()) return emptyList()
    
    // Nhóm theo ngày (sắp xếp theo thời gian giảm dần)
    val groupedByDate = this
        .sortedByDescending { it.createdAt }
        .groupBy { DateUtils.formatGroupDate(it.createdAt) }
    
    // Tạo list sections
    return groupedByDate.map { (dateKey, historyList) ->
        val displayDate = DateUtils.formatDisplayDate(historyList.first().createdAt)
        HistoryDateSection(
            date = dateKey,
            formattedDate = displayDate,
            wallpapers = historyList.sortedByDescending { it.createdAt }
        )
    }
}