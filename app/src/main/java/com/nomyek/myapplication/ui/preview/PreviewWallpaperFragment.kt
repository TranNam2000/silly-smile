package com.nomyek.myapplication.ui.preview

import android.app.WallpaperManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
import android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.jrm.base.BaseFragment
import com.nomyek.myapplication.R
import com.nomyek.myapplication.data.entity.WallpaperEntity
import com.nomyek.myapplication.data.model.CropInfo
import com.nomyek.myapplication.databinding.FragmentPreviewWallpaperBinding
import com.nomyek.myapplication.ui.main.MainActivity
import com.nomyek.myapplication.utils.click
import com.nomyek.myapplication.utils.gone
import com.nomyek.myapplication.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PreviewWallpaperFragment :
    BaseFragment<FragmentPreviewWallpaperBinding>(FragmentPreviewWallpaperBinding::inflate) {

    companion object {
        const val ARG_WALLPAPER = "wallpaper"
        const val ARG_CROP_INFO = "cropInfo"

        fun newInstance(wallpaper: WallpaperEntity, cropInfo: CropInfo): PreviewWallpaperFragment {
            return PreviewWallpaperFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_WALLPAPER, wallpaper)
                    putParcelable(ARG_CROP_INFO, cropInfo)
                }
            }
        }
    }

    private val viewModel: PreviewWallpaperViewModel by viewModels()

    override fun initView() {
        loadArguments()
        observeViewModel()
    }

    private fun findGCD(a: Int, b: Int): Int {
        var num1 = a
        var num2 = b
        while (num2 != 0) {
            val temp = num2
            num2 = num1 % num2
            num1 = temp
        }
        return num1
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupEdgeToEdge()
    }

    private fun setupEdgeToEdge() {
        activity?.window?.let { window ->
            originalStatusBarColor = window.statusBarColor
            originalNavigationBarColor = window.navigationBarColor
            originalDecorFitsSystemWindows = false

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                originalSystemUiVisibility = window.decorView.systemUiVisibility
            } else {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                originalLightStatusBars = insetsController?.isAppearanceLightStatusBars
                originalLightNavigationBars = insetsController?.isAppearanceLightNavigationBars
            }

            WindowCompat.setDecorFitsSystemWindows(window, false)

            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = false
                    isAppearanceLightNavigationBars = false
                }
            } else {

                var flags = window.decorView.systemUiVisibility
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    flags = flags and SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    flags = flags and SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
                }
                window.decorView.systemUiVisibility = flags
            }
        }

        // Remove padding from content view để full screen (BaseActivity đã set padding)
        activity?.findViewById<View>(android.R.id.content)?.let { contentView ->
            contentView.setPadding(0, 0, 0, 0)
            ViewCompat.setOnApplyWindowInsetsListener(contentView, null)
        }

        binding?.root?.let { rootView ->
            ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, windowInsets ->
                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

                binding?.btnBack?.let { backButton ->
                    val layoutParams =
                        backButton.layoutParams as? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
                    layoutParams?.topMargin =
                        insets.top + (16 * resources.displayMetrics.density).toInt()
                    backButton.layoutParams = layoutParams
                }

                binding?.btnApply?.let { applyButton ->
                    val layoutParams =
                        applyButton.layoutParams as? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
                    layoutParams?.bottomMargin =
                        insets.bottom + (24 * resources.displayMetrics.density).toInt()
                    applyButton.layoutParams = layoutParams
                }
                windowInsets
            }
        }
    }

    private fun restoreWindowState() {
        activity?.window?.let { window ->
            originalStatusBarColor?.let { window.statusBarColor = it }

            if (originalNavigationBarColor != null) {
                window.navigationBarColor = originalNavigationBarColor!!
            } else {
                val typedValue = TypedValue()
                activity?.theme?.resolveAttribute(
                    android.R.attr.navigationBarColor,
                    typedValue,
                    true
                )
                if (typedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT &&
                    typedValue.type <= TypedValue.TYPE_LAST_COLOR_INT
                ) {
                    window.navigationBarColor = typedValue.data
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowCompat.getInsetsController(window, window.decorView)?.apply {
                    originalLightStatusBars?.let { isAppearanceLightStatusBars = it }
                    originalLightNavigationBars?.let { isAppearanceLightNavigationBars = it }
                }
            } else {

                originalSystemUiVisibility?.let {
                    window.decorView.systemUiVisibility = it
                }
            }
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }

        activity?.findViewById<View>(android.R.id.content)?.let { contentView ->
            ViewCompat.setOnApplyWindowInsetsListener(contentView) { v, insets ->
                val topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
                val paddingTop = topInset.coerceAtLeast(50)
                v.setPadding(0, paddingTop, 0, 0)
                activity?.let {
                    v.setBackgroundColor(ContextCompat.getColor(it, com.jrm.R.color.white))
                }
                insets
            }
        }
    }

    private var cropInfo: CropInfo? = null

    private var originalStatusBarColor: Int? = null
    private var originalNavigationBarColor: Int? = null
    private var originalDecorFitsSystemWindows: Boolean? = null
    private var originalLightStatusBars: Boolean? = null
    private var originalLightNavigationBars: Boolean? = null
    private var originalSystemUiVisibility: Int? = null // Cho Android 10

    private fun loadArguments() {
        val wallpaper = arguments?.getParcelable<WallpaperEntity>(ARG_WALLPAPER)
        cropInfo = arguments?.getParcelable<CropInfo>(ARG_CROP_INFO)

        wallpaper?.let {
            viewModel.setWallpaper(it)
            Log.d(
                "PreviewWallpaperFragment",
                "Wallpaper received: ${it.title}, URL: ${it.imageUrl}, isAnimated: ${it.isAnimated}"
            )
            cropInfo?.let { info ->
                Log.d(
                    "PreviewWallpaperFragment",
                    "CropInfo received: crop(${info.cropX}, ${info.cropY}, ${info.cropWidth}, ${info.cropHeight})"
                )
            }
        } ?: run {
            Log.e("PreviewWallpaperFragment", "No wallpaper data received!")
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
    }

    private fun loadWallpaper(wallpaper: WallpaperEntity) {
        binding?.apply {
            wallpaperPreview.loadWallpaper(wallpaper.imageUrl)

            Log.d("PreviewWallpaperFragment", "Loading wallpaper: ${wallpaper.imageUrl}")

            // Apply crop info sau khi load xong
            cropInfo?.let { info ->

                wallpaperPreview.post {
                    try {
                        wallpaperPreview.applyCropInfo(cropInfo = info)
                    } catch (e: Exception) {

                    }
                }
            } ?: run {
                Log.w("PreviewWallpaperFragment", "⚠️ No crop info to apply")
            }
        }
    }

    override fun addEvent() {
        binding?.apply {
            btnBack.click {
                requireActivity().onBackPressed()
            }

            btnApply.click {
                showWallpaperOptionsDialog()
            }
        }
    }


    override fun setViewRootClick() {
        binding?.apply {
            if (!layoutTimeDate.isVisible && !layoutTimeDateWeather.isVisible) {
                layoutTimeDate.visible()
                layoutTimeDateWeather.gone()
                return
            } else if (layoutTimeDate.isVisible && !layoutTimeDateWeather.isVisible) {
                layoutTimeDate.gone()
                layoutTimeDateWeather.visible()
                return
            } else {
                layoutTimeDate.gone()
                layoutTimeDateWeather.gone()
                return
            }
        }
    }

    private fun showWallpaperOptionsDialog() {
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

        dialogView.findViewById<View>(R.id.option_home).click {
            dialog.dismiss()
            setStaticWallpaper(WallpaperManager.FLAG_SYSTEM)
        }

        dialogView.findViewById<View>(R.id.option_lock).click {
            dialog.dismiss()
            setStaticWallpaper(WallpaperManager.FLAG_LOCK)
        }

        dialogView.findViewById<View>(R.id.option_both).click {
            dialog.dismiss()
            setStaticWallpaper(
                WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
            )
        }

        dialogView.findViewById<View>(R.id.option_cancel).click {
            dialog.dismiss()
        }

        dialog.show()

    }

    private fun setStaticWallpaper(flags: Int) {
        try {
            viewModel.currentWallpaper.value ?: run {
                showToast(getString(R.string.error_no_wallpaper))
                return
            }
            val finalCropInfo = cropInfo ?: run {
                try {
                    binding?.wallpaperPreview?.getCroppedBitmap() ?: run {
                        showToast(getString(R.string.error_no_crop_info))
                        return
                    }
                } catch (e: Exception) {
                    showToast("Lỗi: ${e.message}")
                    return
                }
            }

            viewModel.setStaticWallpaper(finalCropInfo, flags) {
                (activity as MainActivity).setDataHistoryAndCropInfo(
                    cropInfo = finalCropInfo,
                    historyEntity = it
                )
                (activity as MainActivity).openSuccessActivity(isGif = false)
            }
        } catch (e: Exception) {
            showToast("❌ Lỗi: ${e.message}")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        restoreWindowState()
    }

    override fun getScreenName(): String = "PreviewWallpaperFragment"
    override var isClick: Boolean = false
}

