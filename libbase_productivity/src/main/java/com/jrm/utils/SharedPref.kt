package com.jrm.utils

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager

object SharedPref {
    private var sharedPreferences: SharedPreferences? = null
    var isShowPolicychange = false
    
    @JvmStatic
    fun init(context: Context) {
        if (sharedPreferences == null) {
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        }
    }

    @JvmStatic
    fun saveString(key: String, value: String) {
        sharedPreferences?.edit()?.putString(key, value)?.apply()
    }

    @JvmStatic
    fun readString(key: String, defaultValue: String): String {
        return sharedPreferences?.getString(key, defaultValue) ?: defaultValue
    }

    @JvmStatic
    fun saveInteger(key: String, value: Int) {
        sharedPreferences?.edit()?.putInt(key, value)?.apply()
    }

    @JvmStatic
    fun readInteger(key: String, defaultValue: Int): Int {
        return sharedPreferences?.getInt(key, defaultValue) ?: defaultValue
    }

    @JvmStatic
    fun saveBoolean(key: String, value: Boolean) {
        sharedPreferences?.edit()?.putBoolean(key, value)?.apply()
    }

    @JvmStatic
    fun readBoolean(key: String, defaultValue: Boolean): Boolean {
        return sharedPreferences?.getBoolean(key, defaultValue) ?: defaultValue
    }

    @JvmStatic
    fun saveLong(key: String, value: Long) {
        sharedPreferences?.edit()?.putLong(key, value)?.apply()
    }

    @JvmStatic
    fun readLong(key: String, defaultValue: Long): Long {
        return sharedPreferences?.getLong(key, defaultValue) ?: defaultValue
    }

    @JvmStatic
    fun saveFloat(key: String, value: Float) {
        sharedPreferences?.edit()?.putFloat(key, value)?.apply()
    }

    @JvmStatic
    fun readFloat(key: String, defaultValue: Float): Float {
        return sharedPreferences?.getFloat(key, defaultValue) ?: defaultValue
    }

    @JvmStatic
    fun remove(key: String) {
        sharedPreferences?.edit()?.remove(key)?.apply()
    }

    @JvmStatic
    fun clear() {
        sharedPreferences?.edit()?.clear()?.apply()
    }
}