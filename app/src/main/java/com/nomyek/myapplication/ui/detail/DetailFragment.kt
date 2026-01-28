package com.nomyek.myapplication.ui.detail

import android.app.Activity
import android.os.Bundle
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.jrm.base.BaseFragment
import com.nomyek.myapplication.R
import com.nomyek.myapplication.data.entity.WallpaperEntity
import com.nomyek.myapplication.databinding.FragmentDetailBinding
import com.nomyek.myapplication.ui.detail.dialog.UnlockWallpaperDialog
import com.nomyek.myapplication.ui.edit.editgif.EditGitFragment
import com.nomyek.myapplication.ui.edit.editimage.EditImageFragment
import com.nomyek.myapplication.ui.main.MainActivity
import com.nomyek.myapplication.utils.Constants.ARG_TITLE
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DetailFragment : BaseFragment<FragmentDetailBinding>(FragmentDetailBinding::inflate) {

    companion object {
        private const val ARG_WALLPAPER = "wallpaper"
        private const val ARG_WALLPAPER_LIST = "wallpaper_list"
        private const val ARG_SELECTED_INDEX = "selected_index"


        fun newInstance(
            selectedWallpaper: WallpaperEntity,
            wallpaperList: ArrayList<WallpaperEntity>,
            selectedIndex: Int = 0,
            title: String = ""
        ): DetailFragment {
            return DetailFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_WALLPAPER, selectedWallpaper)
                    putParcelableArrayList(ARG_WALLPAPER_LIST, wallpaperList)
                    putInt(ARG_SELECTED_INDEX, selectedIndex)
                    putString(ARG_TITLE, title)
                }
            }
        }
    }

    private val viewModel: DetailViewModel by viewModels()
    private lateinit var wallpaperAdapter: WallpaperDetailAdapter
    private var selectedIndex: Int = 0
    private var isInitialLoad = true
    private var title: String = ""

    override fun initView() {
        setupEdgeToEdge()
        setupViewPager()
        loadArguments()
        observeData()
    }

    private fun setupEdgeToEdge() {
        binding?.root?.let { root ->
            ViewCompat.setOnApplyWindowInsetsListener(root) { view, windowInsets ->
                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                binding?.layoutHeader?.updatePadding(top = insets.top)
                windowInsets
            }
        }
    }

    private fun setupViewPager() {
        wallpaperAdapter = WallpaperDetailAdapter { item, isFavorite ->
            viewModel.toggleFavorite(item, isFavorite)
        }
        binding?.viewpagerWallpapers?.apply {
            adapter = wallpaperAdapter
            offscreenPageLimit = 3

            setPageTransformer(CarouselPageTransformer())

            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    val wallpaper = wallpaperAdapter.currentList.getOrNull(position)
                    wallpaper?.let {
                        viewModel.setCurrentWallpaper(it)
                        viewModel.updateTimeOpen(it.id)
                    }
                }
            })
        }
    }

    private fun loadArguments() {
        val selectedWallpaper = arguments?.getParcelable<WallpaperEntity>(ARG_WALLPAPER)
        val wallpaperList = arguments?.getParcelableArrayList<WallpaperEntity>(ARG_WALLPAPER_LIST)
        selectedIndex = arguments?.getInt(ARG_SELECTED_INDEX, 0) ?: 0
        arguments?.getString(ARG_TITLE, getString(R.string.app_name))
            ?: getString(R.string.app_name)


        binding?.title?.text = title

        selectedWallpaper?.let {
            viewModel.setCurrentWallpaper(it)
            viewModel.updateTimeOpen(it.id)
        }

        wallpaperList?.let {
            viewModel.setWallpaperList(it)
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allWallpapers.collect { wallpapers ->
                wallpaperAdapter.submitList(wallpapers) {
                    if (isInitialLoad && wallpapers.isNotEmpty()) {
                        binding?.viewpagerWallpapers?.setCurrentItem(selectedIndex, false)
                        isInitialLoad = false
                    }
                }
            }
        }
    }


    override fun addEvent() {
        binding?.apply {
            btnApply.setOnClickListener {
                viewModel.currentWallpaper.value?.let { wallpaper ->
                    showUnlockDialog()
                }
            }

            btnBack.setOnClickListener {
                requireActivity().onBackPressed()
            }
        }
    }

    private fun showUnlockDialog() {
        (activity as MainActivity).let {
            val dialog = UnlockWallpaperDialog(requireContext(), it) {
                viewModel.currentWallpaper.value?.let { wallpaper ->
                    navigateToEditScreen(wallpaper)
                }
            }
            dialog.show()
        }

    }

    private fun navigateToEditScreen(wallpaper: WallpaperEntity) {
        val fragment: Fragment = if (wallpaper.isAnimated) {
            EditGitFragment.newInstance(wallpaper = wallpaper, title = title)
        } else {
            EditImageFragment.newInstance(wallpaper = wallpaper, title = title)
        }
        (activity as? MainActivity)?.navigate(
            fragment = fragment,
            addToBackStack = true,
            showInter = false
        )

    }

    override fun getScreenName(): String = "DetailFragment"

    override fun onResume() {
        super.onResume()
        (activity as MainActivity).let {
            it.showPreloadedCollapsibleNativeAd()
        }
    }
}