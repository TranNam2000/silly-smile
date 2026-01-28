package com.nomyek.myapplication.ui.favorite

import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.jrm.base.BaseFragment
import com.nomyek.myapplication.data.entity.WallpaperEntity
import com.nomyek.myapplication.databinding.FragmentFavoriteBinding
import com.nomyek.myapplication.ui.edit.editgif.EditGitFragment
import com.nomyek.myapplication.ui.edit.editimage.EditImageFragment
import com.nomyek.myapplication.ui.home.WallpaperAdapter
import com.nomyek.myapplication.ui.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FavoriteFragment : BaseFragment<FragmentFavoriteBinding>(FragmentFavoriteBinding::inflate) {

    private val viewModel: FavoriteViewModel by viewModels()
    private lateinit var wallpaperAdapter: WallpaperAdapter

    override fun initView() {
        setupEdgeToEdge()
        setupRecyclerView()
        observeData()
    }
    
    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(binding!!.rootLayout) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = insets.top)
            windowInsets
        }
    }

    private fun setupRecyclerView() {
        wallpaperAdapter = WallpaperAdapter(
            activity = null, // Favorite screen doesn't need ads
            activityName = "",
            onFavoriteClick = { wallpaper, isFavorite ->
                if (!isFavorite) {
                    viewModel.removeFavorite(wallpaper.id)
                }
            },
            onItemClick = {
                navigateToEditScreen(wallpaper = it)
            }
        )

        binding?.rvFavoriteWallpapers?.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = wallpaperAdapter
        }
    }
    private fun navigateToEditScreen(wallpaper: WallpaperEntity) {
        val fragment: Fragment = if (wallpaper.isAnimated) {
            EditGitFragment.newInstance(wallpaper)
        } else {
            EditImageFragment.newInstance(wallpaper)
        }
        (activity as? MainActivity)?.navigate(
            fragment = fragment,
            addToBackStack = true
        )

    }


    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.favoriteWallpapers.collect { wallpapers ->
                // Convert WallpaperEntity list to HomeListItem list
                val homeListItems = wallpapers.map { 
                    com.nomyek.myapplication.ui.home.HomeListItem.WallpaperItem(it) 
                }
                wallpaperAdapter.submitList(homeListItems)

                binding?.layoutEmptyState?.isVisible = wallpapers.isEmpty()
                binding?.rvFavoriteWallpapers?.isVisible = wallpapers.isNotEmpty()
            }
        }
    }

    override fun addEvent() {

    }
    
    override fun getScreenName(): String = "FavoriteFragment"
}

