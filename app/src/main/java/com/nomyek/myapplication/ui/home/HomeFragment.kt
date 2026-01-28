package com.nomyek.myapplication.ui.home

import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.jrm.base.BaseFragment
import com.nomyek.myapplication.R
import com.nomyek.myapplication.databinding.FragmentHomeBinding
import com.nomyek.myapplication.ui.detail.DetailFragment
import com.nomyek.myapplication.ui.home.poup.CategoryId
import com.nomyek.myapplication.ui.home.poup.CategoryPopupMenu
import com.nomyek.myapplication.ui.main.MainActivity
import com.nomyek.myapplication.utils.click
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding>(FragmentHomeBinding::inflate) {

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var wallpaperAdapter: WallpaperAdapter
    private var currentTabIndex = 0

    override fun initView() {
        setupTabSelector()
        setupRecyclerView()
        observeData()
    }

    private fun setupTabSelector() {
        binding?.tabSelector?.apply {
            setOnTabSelectedListener { index ->
                currentTabIndex = index
                when (index) {
                    0 -> {
                        binding?.btnCategories?.isVisible = false
                        binding?.spacer?.isVisible = true
                        viewModel.loadSmileWallpapers()
                    }

                    1 -> {
                        binding?.btnCategories?.isVisible = true
                        binding?.spacer?.isVisible = false
                        viewModel.load4KWallpapers()
                    }
                }
            }
        }
    }

    private fun setupRecyclerView() {
        wallpaperAdapter = WallpaperAdapter(
            activity = activity,
            activityName = "HomeFragment",
            onItemClick = { selectedWallpaper ->
                val currentList = viewModel.wallpapers.value
                // Filter to get only wallpaper items
                val wallpaperOnlyList = currentList.filterIsInstance<HomeListItem.WallpaperItem>()
                    .map { it.wallpaper }
                val selectedIndex = wallpaperOnlyList.indexOf(selectedWallpaper)
                val currentTitle = when (currentTabIndex) {
                    0 -> getString(R.string.smile)
                    1 -> getString(R.string._4k_wallpaper)
                    else -> getString(R.string.app_name)
                }

                val detailFragment = DetailFragment.newInstance(
                    selectedWallpaper = selectedWallpaper,
                    wallpaperList = ArrayList(wallpaperOnlyList),
                    selectedIndex = selectedIndex,
                    title = currentTitle
                )

                (activity as MainActivity).navigate(
                    fragment = detailFragment,
                    addToBackStack = true
                )
            },
            onFavoriteClick = { item, isFavorite ->
                viewModel.toggleFavorite(item, isFavorite)
            },
        )

        val gridLayoutManager = GridLayoutManager(requireContext(), 2)
        // Native ads should span full width (2 columns)
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return when (wallpaperAdapter.getItemViewType(position)) {
                    1 -> 2 // Native ad spans 2 columns (VIEW_TYPE_NATIVE_AD = 1)
                    else -> 1 // Wallpaper items span 1 column
                }
            }
        }

        binding?.rvWallpapers?.apply {
            layoutManager = gridLayoutManager
            adapter = wallpaperAdapter
        }
    }

    override fun addEvent() {
        binding?.btnCategories?.click {
            showCategoryPopup(it)
        }
    }

    private fun showCategoryPopup(anchorView: android.view.View) {
        val currentCategory = viewModel.selectedCategory.value
        val categoryPopup = CategoryPopupMenu(
            context = requireContext(),
            selectedCategoryId = currentCategory
        ) { category ->
            viewModel.filterByCategory(category.id)
        }
        categoryPopup.show(anchorView)
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.wallpapers.collect { wallpapers ->
                wallpaperAdapter.submitList(wallpapers)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedCategory.collect { categoryId ->
                updateCategoryButtonText(categoryId)
            }
        }
    }

    private fun updateCategoryButtonText(categoryId: CategoryId) {
        binding?.tvCategoryName?.text = getString(categoryId.resId)
    }

    override fun getScreenName(): String = "HomeFragment"
}