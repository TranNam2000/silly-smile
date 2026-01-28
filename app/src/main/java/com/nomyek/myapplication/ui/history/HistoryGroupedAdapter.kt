package com.nomyek.myapplication.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nomyek.myapplication.data.entity.HistoryEntity
import com.nomyek.myapplication.databinding.ItemHistoryDateSectionBinding
import com.nomyek.myapplication.ui.home.WallpaperAdapter

class HistoryGroupedAdapter(
    private val onItemClick: ((HistoryEntity) -> Unit)? = null
) : ListAdapter<HistoryDateSection, HistoryGroupedAdapter.DateSectionViewHolder>(HistoryDateSectionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DateSectionViewHolder {
        val binding = ItemHistoryDateSectionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DateSectionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DateSectionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DateSectionViewHolder(
        private val binding: ItemHistoryDateSectionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val wallpaperAdapter = WallpaperAdapter(
            activity = null, // History screen doesn't need ads
            activityName = "",
            onItemClick = { wallpaper ->
                val historyEntity = currentDateSection?.wallpapers?.find {
                    "history_${it.id}" == wallpaper.id 
                }
                historyEntity?.let { onItemClick?.invoke(it) }
            },
            isShowFavorite = false
        )

        private var currentDateSection: HistoryDateSection? = null

        init {
            binding.rvWallpapers.apply {
                layoutManager = GridLayoutManager(binding.root.context, 2)
                adapter = wallpaperAdapter
            }
        }

        fun bind(dateSection: HistoryDateSection) {
            currentDateSection = dateSection
            binding.tvDate.text = dateSection.formattedDate
            
            val wallpapers = dateSection.wallpapers.map { historyEntity ->
                com.nomyek.myapplication.data.entity.WallpaperEntity(
                    id = "history_${historyEntity.id}",
                    title = historyEntity.title,
                    category = historyEntity.category,
                    imageUrl = historyEntity.originalPath,
                    previewUrl = historyEntity.originalPath,
                    sourceType = "HISTORY",
                    isAnimated = historyEntity.isAnimated,
                    localPath = historyEntity.originalPath,
                    timeOpen = historyEntity.createdAt
                )
            }
            
            // Convert WallpaperEntity list to HomeListItem list
            val homeListItems = wallpapers.map { 
                com.nomyek.myapplication.ui.home.HomeListItem.WallpaperItem(it) 
            }
            wallpaperAdapter.submitList(homeListItems)
        }
    }

    class HistoryDateSectionDiffCallback : DiffUtil.ItemCallback<HistoryDateSection>() {
        override fun areItemsTheSame(oldItem: HistoryDateSection, newItem: HistoryDateSection): Boolean {
            return oldItem.date == newItem.date
        }

        override fun areContentsTheSame(oldItem: HistoryDateSection, newItem: HistoryDateSection): Boolean {
            return oldItem == newItem
        }
    }
}