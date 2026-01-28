package com.nomyek.myapplication.ui.edit.editgif

import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.jrm.base.BaseFragment
import com.nomyek.myapplication.R
import com.nomyek.myapplication.comon.WallpaperPreviewGitCard
import com.nomyek.myapplication.data.entity.HistoryEntity
import com.nomyek.myapplication.data.entity.WallpaperEntity
import com.nomyek.myapplication.data.model.CropInfo
import com.nomyek.myapplication.databinding.FragmentEditGitBinding
import com.nomyek.myapplication.ui.main.MainActivity
import com.nomyek.myapplication.utils.Constants.ARG_TITLE
import com.nomyek.myapplication.utils.click
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EditGitFragment : BaseFragment<FragmentEditGitBinding>(FragmentEditGitBinding::inflate) {

    companion object {
        private const val TAG = "EditGitFragment"
        private const val ARG_WALLPAPER = "wallpaper"
        private const val ARG_HISTORY_ENTITY = "history_entity"

        fun newInstance(
            wallpaper: WallpaperEntity,
            title: String = ""
        ): EditGitFragment {
            return EditGitFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_WALLPAPER, wallpaper)
                    putString(ARG_TITLE, title)

                }
            }
        }

        fun newInstanceFromHistory(historyEntity: HistoryEntity): EditGitFragment {
            return EditGitFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_HISTORY_ENTITY, historyEntity)
                }
            }
        }
    }

    private val viewModel: EditGitViewModel by viewModels()

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
            val metrics = Resources.getSystem().displayMetrics
            val screenWidth = metrics.widthPixels
            val screenHeight = metrics.heightPixels
            val ratio = screenWidth.toFloat() / screenHeight

            val imageView: WallpaperPreviewGitCard? = binding?.wallpaperPreview
            val params = imageView?.layoutParams as ConstraintLayout.LayoutParams
            params.dimensionRatio = "W,${ratio}:1"
            imageView.layoutParams = params
        }
    }


    private fun loadArguments() {
        val wallpaper = arguments?.getParcelable<WallpaperEntity>(ARG_WALLPAPER)
        val historyEntity = arguments?.getParcelable<HistoryEntity>(ARG_HISTORY_ENTITY)
        val title = arguments?.getString(ARG_TITLE, getString(R.string.app_name))
            ?: getString(R.string.app_name)
        binding?.title?.text = title
        when {
            wallpaper != null -> {
                viewModel.setWallpaper(wallpaper)
                Log.d(
                    "EditGitFragment",
                    "Wallpaper: ${wallpaper.title}, URL: ${wallpaper.imageUrl}"
                )
            }

            historyEntity != null -> {
                // Convert HistoryEntity to WallpaperEntity
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
                Log.d(
                    "EditGitFragment",
                    "History: ${historyEntity.title}, Path: ${historyEntity.originalPath}"
                )
            }

            else -> {
                Log.e("EditGitFragment", "No wallpaper data or history entity!")
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentWallpaper.collect { wallpaper ->
                wallpaper?.let { loadWallpaper(it) }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.wallpaperSetResult.collect { result ->
                result?.let {
                    Toast.makeText(
                        requireContext(),
                        "✅ $it",
                        Toast.LENGTH_LONG
                    ).show()
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
            wallpaperPreview.loadWallpaper(wallpaper.imageUrl)
        }
    }

    override fun addEvent() {
        binding?.apply {
            btnBack.click {
                requireActivity().onBackPressed()
            }

            btnPreview.click {
                setLiveWallpaper()
            }

            btnSave.click {
                setLiveWallpaper()
            }

            btnApply.click {
                val cropInfo = CropInfo(
                    viewModel.currentWallpaper.value?.imageUrl ?: ""
                )
                viewModel.setLiveWallpaper(
                    activity = activity as MainActivity,
                    cropInfo
                ) {
                    (activity as MainActivity).setDataHistoryAndCropInfo(
                        cropInfo = cropInfo,
                        historyEntity = it
                    )
                }
            }
        }
    }


    private fun setLiveWallpaper() {
        getCropInfoAndExecute { cropInfo ->
            viewModel.setLiveWallpaper(activity = activity as MainActivity, cropInfo) {
                (activity as MainActivity).setDataHistoryAndCropInfo(
                    cropInfo = cropInfo,
                    historyEntity = it
                )
            }
        }
    }


    private fun getCropInfoAndExecute(action: (CropInfo) -> Unit) {
        try {
            viewModel.currentWallpaper.value ?: run {
                showToast(getString(R.string.error_no_wallpaper))
                return
            }

            val cropInfo = binding?.wallpaperPreview?.getCropInfo() ?: run {
                showToast(getString(R.string.error_no_crop_info))
                return
            }

            action(cropInfo)

        } catch (e: Exception) {
            Log.e(TAG, "Error getting crop info", e)
            showToast(getString(R.string.error_processing_image, e.message ?: "Unknown"))
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun applyCropInfoFromHistory(cropInfo: CropInfo) {
        binding?.wallpaperPreview?.applyCropInfo(cropInfo)
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    override fun getScreenName(): String = TAG
}
