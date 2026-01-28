package com.nomyek.myapplication.utils

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    
    private const val DATE_FORMAT_DISPLAY = "dd/MM/yyyy"
    private const val DATE_FORMAT_GROUP = "yyyy-MM-dd"
    
    /**
     * Format timestamp thành string hiển thị
     */
    fun formatDisplayDate(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        
        calendar.timeInMillis = timestamp
        
        return when {
            isSameDay(calendar, today) -> "Hôm nay"
            isSameDay(calendar, yesterday) -> "Hôm qua"
            else -> {
                val dateFormat = SimpleDateFormat(DATE_FORMAT_DISPLAY, Locale.getDefault())
                dateFormat.format(Date(timestamp))
            }
        }
    }
    
    /**
     * Format timestamp thành string để group
     */
    fun formatGroupDate(timestamp: Long): String {
        val dateFormat = SimpleDateFormat(DATE_FORMAT_GROUP, Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }
    
    /**
     * Kiểm tra 2 calendar có cùng ngày không
     */
    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}