package com.nomyek.myapplication.ui.succse

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.jrm.base.BaseActivity
import com.nomyek.myapplication.R
import com.nomyek.myapplication.data.model.CropInfo
import com.nomyek.myapplication.databinding.ActivitySuccessBinding
import com.nomyek.myapplication.ui.main.MainActivity
import com.nomyek.myapplication.utils.click
import com.nomyek.myapplication.utils.gone
import com.nomyek.myapplication.utils.visible
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SuccessActivity : BaseActivity<ActivitySuccessBinding>() {

    companion object {
        const val KEY_CROP_INFO = "KeyCropInfo"
        const val KEY_GIF = "KeyGif"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun getLayoutActivity(): Int = R.layout.activity_success

    override fun initViews() {
        viewBinding.btnBack.click {
            onBackPressed()
        }

        val cropInfo = intent.getParcelableExtra<CropInfo>(KEY_CROP_INFO)

        val isGif = intent.getBooleanExtra(KEY_GIF, false)

        if (isGif) {
            viewBinding.wallpaperPreviewImage.gone()
            viewBinding.wallpaperPreviewGif.visible()
            if (cropInfo == null) {
                return
            }

            val imagePath = cropInfo.originalPath
            if (imagePath.isNullOrEmpty()) {
                return
            }

            viewBinding.wallpaperPreviewGif.post {
                viewBinding.wallpaperPreviewGif.loadWallpaper(imagePath)
                viewBinding.wallpaperPreviewGif.postDelayed({
                    viewBinding.wallpaperPreviewGif.applyCropInfo(cropInfo = cropInfo)
                }, 100)
            }
        } else {
            viewBinding.wallpaperPreviewImage.visible()
            viewBinding.wallpaperPreviewGif.gone()

            if (cropInfo == null) {
                return
            }

            val imagePath = cropInfo.originalPath
            if (imagePath.isNullOrEmpty()) {
                return
            }

            viewBinding.wallpaperPreviewImage.post {
                viewBinding.wallpaperPreviewImage.loadWallpaper(imagePath)
                viewBinding.wallpaperPreviewImage.postDelayed({
                    viewBinding.wallpaperPreviewImage.applyCropInfo(cropInfo = cropInfo)
                }, 100)
            }
        }
        setupScreenRatioImage()
        setupScreenRatioGif()
    }

    override fun onBackPressed() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }

    private fun setupScreenRatioImage() {
        viewBinding.wallpaperPreviewImage.post {
            val metrics = resources.displayMetrics
            val screenWidth = metrics.widthPixels
            val screenHeight = metrics.heightPixels

            val imageView = viewBinding.wallpaperPreviewImage
            val params = imageView.layoutParams as? ConstraintLayout.LayoutParams ?: return@post

            val topMargin = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._24sdp)
            val bottomMargin = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._24sdp)
            val marginHorizontal =
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._24sdp) * 2
            val headerHeight = viewBinding.tvSuccess.bottom
            val availableHeight = screenHeight - headerHeight - topMargin - bottomMargin
            val calculatedWidth = (availableHeight * screenWidth) / screenHeight
            val maxWidth = screenWidth - marginHorizontal

            val finalWidth = minOf(calculatedWidth, maxWidth)
            val finalHeight = (finalWidth * screenHeight) / screenWidth

            params.width = finalWidth
            params.height = finalHeight
            params.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
            params.dimensionRatio = null

            params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID

            params.topMargin = topMargin
            params.bottomMargin = bottomMargin
            imageView.layoutParams = params

        }
    }

    private fun setupScreenRatioGif() {
        viewBinding.wallpaperPreviewGif.post {
            val metrics = resources.displayMetrics
            val screenWidth = metrics.widthPixels
            val screenHeight = metrics.heightPixels

            val imageView = viewBinding.wallpaperPreviewGif
            val params = imageView.layoutParams as? ConstraintLayout.LayoutParams ?: return@post

            val topMargin = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._24sdp)
            val bottomMargin = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._24sdp)
            val marginHorizontal =
                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._24sdp) * 2
            val headerHeight = viewBinding.tvSuccess.bottom
            val availableHeight = screenHeight - headerHeight - topMargin - bottomMargin

            val calculatedWidth = (availableHeight * screenWidth) / screenHeight
            val maxWidth = screenWidth - marginHorizontal

            val finalWidth = minOf(calculatedWidth, maxWidth)
            val finalHeight = (finalWidth * screenHeight) / screenWidth

            params.width = finalWidth
            params.height = finalHeight
            params.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
            params.dimensionRatio = null

            params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID

            params.topMargin = topMargin
            params.bottomMargin = bottomMargin
            imageView.layoutParams = params
        }
    }


}