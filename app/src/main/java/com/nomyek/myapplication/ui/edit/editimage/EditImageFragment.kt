package com.nomyek.myapplication.ui.edit.editimage

import android.app.WallpaperManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.jrm.base.BaseFragment
import com.nomyek.myapplication.R
import com.nomyek.myapplication.comon.WallpaperPreviewImageCard
import com.nomyek.myapplication.data.entity.HistoryEntity
import com.nomyek.myapplication.data.entity.WallpaperEntity
import com.nomyek.myapplication.data.model.CropInfo
import com.nomyek.myapplication.databinding.FragmentEditImageBinding
import com.nomyek.myapplication.ui.main.MainActivity
import com.nomyek.myapplication.ui.preview.PreviewWallpaperFragment
import com.nomyek.myapplication.utils.Constants.ARG_TITLE
import com.nomyek.myapplication.utils.click
import com.nomyek.myapplication.utils.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EditImageFragment :
    BaseFragment<FragmentEditImageBinding>(FragmentEditImageBinding::inflate) {

    companion object {
        const val ARG_WALLPAPER = "wallpaper"
        const val ARG_IMAGE_URI = "image_uri"
        const val ARG_HISTORY_ENTITY = "history_entity"

        fun newInstance(wallpaper: WallpaperEntity, title: String = ""): EditImageFragment {
            return EditImageFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_WALLPAPER, wallpaper)
                    putString(ARG_TITLE, title)
                }
            }
        }

        fun newInstanceFromUri(imageUri: String): EditImageFragment {
            return EditImageFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_IMAGE_URI, imageUri)
                }
            }
        }

        fun newInstanceFromHistory(historyEntity: HistoryEntity): EditImageFragment {
            return EditImageFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_HISTORY_ENTITY, historyEntity)
                }
            }
        }
    }

    private val viewModel: EditImageViewModel by viewModels()

    override fun initView() {
        loadArguments()
        observeViewModel()
        setupScreenRatio()
    }

    override fun onResume() {
        super.onResume()
        (activity as MainActivity).let {
            it.showPreloadedCollapsibleNativeAd()
        }
    }

    private fun setupScreenRatio() {
        binding?.wallpaperPreview?.post {
            val metrics = resources.displayMetrics
            val screenWidth = metrics.widthPixels
            val screenHeight = metrics.heightPixels
            val ratio = screenWidth.toFloat() / screenHeight
            val imageView: WallpaperPreviewImageCard? = binding?.wallpaperPreview
            val params = imageView?.layoutParams as ConstraintLayout.LayoutParams
            params.dimensionRatio = "W,${ratio}:1"
            imageView.layoutParams = params
        }
    }


    private fun loadArguments() {
        val wallpaper = arguments?.getParcelable<WallpaperEntity>(ARG_WALLPAPER)
        val imageUri = arguments?.getString(ARG_IMAGE_URI)
        val historyEntity = arguments?.getParcelable<HistoryEntity>(ARG_HISTORY_ENTITY)

        val title = arguments?.getString(ARG_TITLE, getString(R.string.app_name))
            ?: getString(R.string.app_name)
        binding?.title?.text = title
        when {
            wallpaper != null -> {
                viewModel.setWallpaper(wallpaper)
            }

            imageUri != null -> {
                // Create a temporary WallpaperEntity from URI
                val tempWallpaper = WallpaperEntity(
                    id = "user_${System.currentTimeMillis()}",
                    title = "Selected Image",
                    category = "user_selected",
                    imageUrl = imageUri,
                    previewUrl = imageUri,
                    sourceType = "USER_SELECTED",
                    isAnimated = false,
                    localPath = imageUri
                )
                viewModel.setWallpaper(tempWallpaper)
            }

            historyEntity != null -> {
                val wallpaperFromHistory = WallpaperEntity(
                    id = "history_${historyEntity.id}",
                    title = historyEntity.title,
                    category = historyEntity.category,
                    imageUrl = historyEntity.originalPath,
                    previewUrl = historyEntity.originalPath,
                    sourceType = "HISTORY",
                    isAnimated = historyEntity.isAnimated,
                    localPath = historyEntity.originalPath
                )
                viewModel.setWallpaper(wallpaperFromHistory)
                viewModel.setHistoryEntity(historyEntity)
            }

            else -> {
                Log.e(
                    "EditImageFragment",
                    "No wallpaper data, image URI, or history entity received!"
                )
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentWallpaper.collect { wallpaper ->
                wallpaper?.let {
                    loadWallpaper(it)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.errorMessage.collect { error ->
                error?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.wallpaperSetResult.collect { result ->
                result?.let {
                    showToast(it)
                }
            }
        }

        // Apply CropInfo from history if available
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentHistoryEntity.collect { historyEntity ->
                historyEntity?.let {
                    applyCropInfoFromHistory(it.toCropInfo())
                }
            }
        }
    }

    private fun loadWallpaper(wallpaper: WallpaperEntity) {
        binding?.apply {
            // Load wallpaper using custom view
            wallpaperPreview.loadWallpaper(wallpaper.imageUrl)
        }
    }

    private fun loadWallpaperFromUri(imageUri: String) {
        binding?.apply {
            // Load wallpaper directly from URI
            wallpaperPreview.loadWallpaper(imageUri)
        }
    }

    override fun addEvent() {
        binding?.apply {
            btnBack.click {
                requireActivity().onBackPressed()
            }

            btnPreview.click {
                navigateToPreview()
            }

            btnSave.click {
                showWallpaperOptionsDialog(false)
            }

            btnApply.click {
                showWallpaperOptionsDialog(true)

            }
        }
    }

    private fun navigateToPreview() {
        val wallpaper = viewModel.currentWallpaper.value
        if (wallpaper == null) {
            showToast(getString(R.string.error_no_wallpaper))
            return
        }

        val cropInfo = try {
            binding?.wallpaperPreview?.getCroppedBitmap() ?: run {
                showToast(getString(R.string.error_no_crop_info))
                return
            }
        } catch (e: Exception) {
            Log.e("EditImageFragment", "Error getting crop info for preview", e)
            showToast(getString(R.string.error_processing_image, e.message ?: "Unknown"))
            return
        }

        val previewFragment = PreviewWallpaperFragment.newInstance(wallpaper, cropInfo)
        (activity as? MainActivity)?.navigate(
            fragment = previewFragment,
            addToBackStack = true
        )
    }

    /**
     * Set static wallpaper (image only, no live wallpaper option)
     */
    private fun setStaticWallpaper(flags: Int, isApply: Boolean = false) {
        try {
            viewModel.currentWallpaper.value ?: run {
                showToast(getString(R.string.error_no_wallpaper))
                return
            }
            if (isApply) {
                val cropInfo = CropInfo(
                    viewModel.currentWallpaper.value?.imageUrl ?: ""
                )
                viewModel.setStaticWallpaper(
                    cropInfo = cropInfo, flags
                ) {
                    (activity as MainActivity).setDataHistoryAndCropInfo(
                        cropInfo = cropInfo,
                        historyEntity = it
                    )
                    (activity as MainActivity).openSuccessActivity(isGif = false)
                }
            } else {
                val cropInfo = try {
                    binding?.wallpaperPreview?.getCroppedBitmap() ?: run {
                        showToast(getString(R.string.error_no_crop_info))
                        return
                    }
                } catch (e: Exception) {
                    Log.e("EditImageFragment", "Error getting crop info", e)
                    showToast(getString(R.string.error_processing_image, e.message ?: "Unknown"))
                    return
                }

                viewModel.setStaticWallpaper(cropInfo = cropInfo, flags) {
                    (activity as MainActivity).setDataHistoryAndCropInfo(
                        cropInfo = cropInfo,
                        historyEntity = it
                    )
                    (activity as MainActivity).openSuccessActivity(isGif = false)
                }
            }
        } catch (e: Exception) {
        }
    }

    private fun showWallpaperOptionsDialog(isApply: Boolean) {
        val dialogView = layoutInflater.inflate(
            R.layout.dialog_wallpaper_option,
            null
        )

        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialog.window?.let { window ->
            val params = window.attributes
            params.gravity = android.view.Gravity.BOTTOM
            params.horizontalMargin = 0f
            params.verticalMargin = 0f
            params.width = android.view.ViewGroup.LayoutParams.MATCH_PARENT
            window.attributes = params
        }

        dialogView.findViewById<View>(R.id.option_home)
            .setOnClickListener {
                dialog.dismiss()
                setStaticWallpaper(WallpaperManager.FLAG_SYSTEM, isApply)
            }

        dialogView.findViewById<View>(R.id.option_lock)
            .setOnClickListener {
                dialog.dismiss()
                setStaticWallpaper(WallpaperManager.FLAG_LOCK, isApply)
            }

        dialogView.findViewById<View>(R.id.option_both)
            .setOnClickListener {
                dialog.dismiss()
                setStaticWallpaper(
                    WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK, isApply
                )
            }

        dialogView.findViewById<View>(R.id.option_cancel)
            .setOnClickListener {
                dialog.dismiss()
            }

        dialog.show()
    }

    private fun applyCropInfoFromHistory(cropInfo: CropInfo) {
        binding?.wallpaperPreview?.applyCropInfo(cropInfo)
    }

    override fun getScreenName(): String = "EditImageFragment"
}
