package com.nomyek.myapplication.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
import android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import com.jrm.base.BaseActivity
import com.jrm.utils.AdsHelper
import com.jrm.utils.BaseUtils
import com.jrm.utils.remote_config.RemoteConfigManager
import com.nomyek.myapplication.R
import com.nomyek.myapplication.data.entity.HistoryEntity
import com.nomyek.myapplication.data.model.CropInfo
import com.nomyek.myapplication.databinding.ActivityMainBinding
import com.nomyek.myapplication.service.GifLiveWallpaperService
import com.nomyek.myapplication.service.GifLiveWallpaperService2
import com.nomyek.myapplication.ui.succse.SuccessActivity
import com.nomyek.myapplication.utils.Constants
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>() {

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun getLayoutActivity(): Int = R.layout.activity_main
    private val viewModel: MainViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            showNotificationPermissionDialog()
        }
    }

    override fun initViews() {
        BaseUtils.setFinishObd(true)
        BaseUtils.setSessionNumber()
        setupEdgeToEdge()
        navigate(MainFragment(), addToBackStack = false, showInter = false)
        requestNotificationPermission()
    }

    override fun onResumeAfterInter() {
        super.onResumeAfterInter()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableCollapsibleNativeAd = true
        disableAutoLoadCollapsibleNative = true // Manage collapsible native manually
        super.onCreate(savedInstanceState)
        AdsHelper.preloadCollapsibleNativeAdHome(this, activityName)

    }

    /**
     * Show preloaded collapsible native ad and set callback to start waterfall ads when user closes it
     * Uses BaseActivity's showPreloadedCollapsibleNativeAdSmart method
     */
    public fun showPreloadedCollapsibleNativeAd() {
        val adPlace = "draw_cl"
        val adConfigString = RemoteConfigManager.instance?.nativeClDrawIds ?: ""

        showPreloadedCollapsibleNativeAdSmart(
            adPlace = adPlace,
            adConfigString = adConfigString,
            onCollapse = {
                AdsHelper.preloadCollapsibleNativeAdHome(this, activityName)
            },
            onAdFailed = {
                AdsHelper.preloadCollapsibleNativeAdHome(this, activityName)
            }
        )
    }

    private fun setupEdgeToEdge() {
        enableEdgeToEdge()

        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowCompat.getInsetsController(window, window.decorView)?.apply {
                isAppearanceLightStatusBars = true
                isAppearanceLightNavigationBars = true
            }
        } else {
            var flags = window.decorView.systemUiVisibility
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags = flags or SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                flags = flags or SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }
            window.decorView.systemUiVisibility = flags
        }
    }

    fun navigate(fragment: Fragment, addToBackStack: Boolean = true, showInter: Boolean = true) {
        if (showInter) {
            AdsHelper.showInterPreload(this, activityName) {
                // Check if activity is in a valid state before committing fragment transaction
                if (!isFinishing && !isDestroyed) {
                    try {
                        supportFragmentManager.beginTransaction().apply {
                            setCustomAnimations(
                                android.R.anim.slide_in_left,
                                android.R.anim.slide_out_right,
                                android.R.anim.slide_in_left,
                                android.R.anim.slide_out_right
                            )
                            replace(R.id.main, fragment)
                            if (addToBackStack) {
                                addToBackStack(fragment::class.java.simpleName)
                            }
                            commitAllowingStateLoss()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error navigating to fragment after ad: ${e.message}")
                    }
                }
            }
        } else {
            if (!isFinishing && !isDestroyed) {
                try {
                    supportFragmentManager.beginTransaction().apply {
                        setCustomAnimations(
                            android.R.anim.slide_in_left,
                            android.R.anim.slide_out_right,
                            android.R.anim.slide_in_left,
                            android.R.anim.slide_out_right
                        )
                        replace(R.id.main, fragment)
                        if (addToBackStack) {
                            addToBackStack(fragment::class.java.simpleName)
                        }
                        commit()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error navigating to fragment: ${e.message}")
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == Constants.REQUEST_CODE_WALLPAPER_PICKER) {
            val isService1Set = GifLiveWallpaperService.isWallpaperSet
            val isService2Set = GifLiveWallpaperService2.isWallpaperSet

            if (isService1Set || isService2Set) {
                GifLiveWallpaperService2.isWallpaperSet = false
                GifLiveWallpaperService.isWallpaperSet = false
                viewModel.addHistory()
                openSuccessActivity()
            }
        }
    }


    fun openSuccessActivity(isGif: Boolean = true) {
        val cropInfo = viewModel.getCropInfo() ?: return
        val intent = Intent(this, SuccessActivity::class.java)
        intent.putExtra(SuccessActivity.KEY_CROP_INFO, cropInfo)
        intent.putExtra(SuccessActivity.KEY_GIF, isGif)
        startActivity(intent)
    }

    fun setDataHistoryAndCropInfo(cropInfo: CropInfo, historyEntity: HistoryEntity) {
        viewModel.setCropInfo(cropInfo)
        viewModel.setHistory(historyEntity)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Show explanation dialog
                    showNotificationPermissionDialog()
                }
                else -> {
                    // Request permission
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun showNotificationPermissionDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.permission_required))
            .setMessage(getString(R.string.notification_permission_message))
            .setPositiveButton(getString(R.string.allow)) { _, _ ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            .setNegativeButton(getString(R.string.not_now)) { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }
}