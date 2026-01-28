package com.jrm.utils

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import com.ads.nomyek_admob.event.YNMAirBridge
import com.google.firebase.analytics.FirebaseAnalytics
import com.jrm.base.BaseEventLogger
import com.jrm.utils.remote_config.RemoteConfigManager
import java.text.SimpleDateFormat
import java.util.*

object BaseUtils {
    @JvmStatic
    var dialog: Dialog? = null
    
    var firstOpen: Boolean = true
    
    // Retention event constants
    private const val RETENTION_D1 = "RetentionD1"
    private const val RETENTION_D3 = "RetentionD3"
    private const val RETENTION_D7 = "RetentionD7"
    private const val RETENTION_D14 = "RetentionD14"
    private const val RETENTION_D30 = "RetentionD30"
    
    // SharedPreferences keys
    private const val KEY_INSTALL_DATE = "install_date"
    private const val KEY_RETENTION_D1_SENT = "retention_d1_sent"
    private const val KEY_RETENTION_D3_SENT = "retention_d3_sent"
    private const val KEY_RETENTION_D7_SENT = "retention_d7_sent"
    private const val KEY_RETENTION_D14_SENT = "retention_d14_sent"
    private const val KEY_RETENTION_D30_SENT = "retention_d30_sent"
    @JvmStatic
    fun isFirstOpenUser(): Boolean {
        return SharedPref.readBoolean(BaseConstants.FIRST_OPEN, true)
    }
    
    @JvmStatic
    fun setFirstOpenApp(isFirstOpen: Boolean) {
        SharedPref.saveBoolean(BaseConstants.FIRST_OPEN, isFirstOpen)
    }
    @JvmStatic var checkFinish = false

    @JvmStatic
    fun setFinishObd(isFinish: Boolean) {
        if (!BaseUtils.checkFinish) {
            BaseUtils.checkFinish = true
            var numberFinish = SharedPref.readLong("number_finish_obd", 0)
            numberFinish++
            SharedPref.saveLong("number_finish_obd", numberFinish)
            if (numberFinish >= RemoteConfigManager.instance!!.numberFinishObd) {
                if (isFinish && !SharedPref.readBoolean(BaseConstants.FINISH_OBD, false)) {
                    BaseEventLogger.logEvent("view_home")
                }
                SharedPref.saveBoolean(BaseConstants.FINISH_OBD, isFinish)
            }
        }
    }
    
    @JvmStatic
    fun isFinishObd(): Boolean {
        return SharedPref.readBoolean(BaseConstants.FINISH_OBD, false)
    }

    @JvmStatic
    fun getUserState(): String {
        if (!isFinishObd()) {
            return "first_user"
        }
        return when {
            isFirstOpenUser() -> "first_user"
            isFinishObd() -> "return"
            else -> "return_obd"
        }
    }
    
    @JvmStatic
    fun getSessionNumber(): Int {
        var sessionNumber = SharedPref.readInteger("session_number", 0)
        if (firstOpen) {
            firstOpen = false
            sessionNumber++
            SharedPref.saveInteger("session_number", sessionNumber)
            return sessionNumber
        }
        return sessionNumber
    }

    /**
     * Initialize install date if not already set
     */
    @JvmStatic
    fun initializeInstallDate() {
        val installDate = SharedPref.readString(KEY_INSTALL_DATE, "")
        if (installDate.isEmpty()) {
            val currentDate = getCurrentDateString()
            SharedPref.saveString(KEY_INSTALL_DATE, currentDate)
        }
    }

    /**
     * Check and push retention events based on days since install
     */
    @JvmStatic
    fun checkAndPushRetentionEvents(context: Context) {
        initializeInstallDate()

        val installDateStr = SharedPref.readString(KEY_INSTALL_DATE, "")
        if (installDateStr.isEmpty()) return

        val daysSinceInstall = getDaysSinceInstall(installDateStr)

        // Check and push retention events
        when {
            daysSinceInstall >= 30 && !SharedPref.readBoolean(KEY_RETENTION_D30_SENT, false) -> {
                pushRetentionEvent(context, RETENTION_D30)
                SharedPref.saveBoolean(KEY_RETENTION_D30_SENT, true)
            }
            daysSinceInstall >= 14 && !SharedPref.readBoolean(KEY_RETENTION_D14_SENT, false) -> {
                pushRetentionEvent(context, RETENTION_D14)
                SharedPref.saveBoolean(KEY_RETENTION_D14_SENT, true)
            }
            daysSinceInstall >= 7 && !SharedPref.readBoolean(KEY_RETENTION_D7_SENT, false) -> {
                pushRetentionEvent(context, RETENTION_D7)
                SharedPref.saveBoolean(KEY_RETENTION_D7_SENT, true)
            }
            daysSinceInstall >= 3 && !SharedPref.readBoolean(KEY_RETENTION_D3_SENT, false) -> {
                pushRetentionEvent(context, RETENTION_D3)
                SharedPref.saveBoolean(KEY_RETENTION_D3_SENT, true)
            }
            daysSinceInstall >= 1 && !SharedPref.readBoolean(KEY_RETENTION_D1_SENT, false) -> {
                pushRetentionEvent(context, RETENTION_D1)
                SharedPref.saveBoolean(KEY_RETENTION_D1_SENT, true)
            }
        }
    }

    /**
     * Push retention event to Firebase Analytics
     */
    private fun pushRetentionEvent(context: Context, eventName: String) {
        try {
            val firebaseAnalytics = FirebaseAnalytics.getInstance(context)
            val bundle = Bundle().apply {
                putString("event_type", "retention")
                putString("retention_day", eventName)
            }
            firebaseAnalytics.logEvent(eventName, bundle)
            YNMAirBridge.getInstance().logCustomEvent(eventName)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Get current date as string in yyyy-MM-dd format
     */
    private fun getCurrentDateString(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }

    /**
     * Calculate days since install date
     */
    private fun getDaysSinceInstall(installDateStr: String): Int {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val installDate = dateFormat.parse(installDateStr)
            val currentDate = Date()

            if (installDate != null) {
                val diffInMillis = currentDate.time - installDate.time
                val diffInDays = diffInMillis / (1000 * 60 * 60 * 24)
                diffInDays.toInt()
            } else {
                0
            }
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }
}
