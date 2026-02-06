package com.jrm.utils

import android.app.Activity
import android.view.View
import com.google.android.datatransport.runtime.scheduling.persistence.EventStoreModule_PackageNameFactory.packageName

 inline fun <reified T : View> Activity.findViewByName(viewName: String): T? {
    return try {
        val resourceId = resources.getIdentifier(viewName, "id", packageName)
        if (resourceId != 0) {
            findViewById<T>(resourceId)
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}