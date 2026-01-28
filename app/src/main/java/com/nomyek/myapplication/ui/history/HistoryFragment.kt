package com.nomyek.myapplication.ui.history

import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.jrm.base.BaseFragment
import com.nomyek.myapplication.databinding.FragmentHistoryBinding
import com.nomyek.myapplication.ui.edit.editgif.EditGitFragment
import com.nomyek.myapplication.ui.edit.editimage.EditImageFragment
import com.nomyek.myapplication.ui.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HistoryFragment : BaseFragment<FragmentHistoryBinding>(FragmentHistoryBinding::inflate) {

    private val viewModel: HistoryViewModel by viewModels()
    private lateinit var historyAdapter: HistoryGroupedAdapter

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
        historyAdapter = HistoryGroupedAdapter(
            onItemClick = { historyEntity ->
                viewModel.onHistoryItemClick(historyEntity)
            }
        )

        binding?.rvHistoryWallpapers?.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = historyAdapter
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.historyWallpapers.collect { historyEntities ->
                val dateSections = historyEntities.toDateSections()
                historyAdapter.submitList(dateSections)
                
                binding?.layoutEmptyState?.isVisible = historyEntities.isEmpty()
                binding?.rvHistoryWallpapers?.isVisible = historyEntities.isNotEmpty()
            }
        }
        
        // Observe navigation events
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.navigationEvent.collect { event ->
                when (event) {
                    is HistoryViewModel.NavigationEvent.NavigateToEditGif -> {
                        val editGifFragment = EditGitFragment.newInstanceFromHistory(event.historyEntity)
                        (activity as? MainActivity)?.navigate(editGifFragment, addToBackStack = true)
                    }
                    is HistoryViewModel.NavigationEvent.NavigateToEditImage -> {
                        val editImageFragment = EditImageFragment.newInstanceFromHistory(event.historyEntity)
                        (activity as? MainActivity)?.navigate(editImageFragment, addToBackStack = true)
                    }
                }
            }
        }
    }

    override fun addEvent() {

    }

    override fun getScreenName(): String = "HistoryFragment"
}