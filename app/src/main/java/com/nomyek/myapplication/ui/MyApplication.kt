package com.nomyek.myapplication.ui

import android.app.Activity
import android.os.Bundle
import com.jrm.base.BaseApplication
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApplication : BaseApplication() {

    override fun tokenAirBridge(): String = "2b74009ef07e4449a23a95ea1b981f32"
    override fun appNameAirBridge(): String = "sillysmilewallpaper"

    override fun onActivityDestroyed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivitySaveInstanceState(
        activity: Activity,
        outState: Bundle
    ) {
    }

    override fun onActivityStarted(activity: Activity) {}
}