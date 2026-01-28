package com.nomyek.myapplication.ui.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.nomyek.myapplication.R
import com.nomyek.myapplication.data.entity.WallpaperEntity
import com.nomyek.myapplication.databinding.ItemWallpaperDetailBinding

class WallpaperDetailAdapter(private val onFavoriteClick: ((WallpaperEntity, Boolean) -> Unit)? = null) :
    ListAdapter<WallpaperEntity, WallpaperDetailAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemWallpaperDetailBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemWallpaperDetailBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(wallpaper: WallpaperEntity) {
            val imagePath = wallpaper.getDisplayUrl()
            val imageSource = parseImagePath(imagePath)
            updateFavoriteIcon(isFavorite = wallpaper.isFavorite)
            if (wallpaper.isAnimated) {
                loadAnimatedGif(imageSource)
            } else {
                loadStaticImage(imageSource)
            }
            // Handle favorite icon click
            binding.ivFavorite.setOnClickListener {
                val currentPosition = bindingAdapterPosition
                if (currentPosition != RecyclerView.NO_POSITION) {
                    val currentItem = getItem(currentPosition)
                    val newFavoriteState = !currentItem.isFavorite
                    onFavoriteClick?.invoke(currentItem, newFavoriteState)
                }
            }
        }

        private fun parseImagePath(path: String): Any {
            return when {
                path.startsWith("assets/") -> {
                    "file:///android_asset/${path.removePrefix("assets/")}"
                }

                else -> path
            }
        }

        private fun loadAnimatedGif(source: Any) {
            val context = binding.ivWallpaper.context
            // Check if context is an Activity and if it's destroyed
            if (context is android.app.Activity && (context.isFinishing || context.isDestroyed)) {
                return
            }
            
            try {
                Glide.with(context)
                    .asGif()
                    .load(source)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(binding.ivWallpaper)
            } catch (e: IllegalArgumentException) {
                // Activity is destroyed, ignore
            }
        }

        private fun loadStaticImage(source: Any) {
            val context = binding.ivWallpaper.context
            // Check if context is an Activity and if it's destroyed
            if (context is android.app.Activity && (context.isFinishing || context.isDestroyed)) {
                return
            }
            
            try {
                val requestOptions = RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .fitCenter()

                Glide.with(context)
                    .load(source)
                    .apply(requestOptions)
                    .into(binding.ivWallpaper)
            } catch (e: IllegalArgumentException) {
                // Activity is destroyed, ignore
            }
        }

        private fun updateFavoriteIcon(isFavorite: Boolean) {
            if (isFavorite) {
                binding.ivFavorite.setImageResource(R.drawable.ic_favorite_filled)
                binding.ivFavorite.setColorFilter(
                    ContextCompat.getColor(binding.root.context, R.color.white),
                    android.graphics.PorterDuff.Mode.SRC_IN
                )
            } else {
                binding.ivFavorite.setImageResource(R.drawable.ic_favorite_border)
                binding.ivFavorite.setColorFilter(
                    ContextCompat.getColor(binding.root.context, R.color.white),
                    android.graphics.PorterDuff.Mode.SRC_IN
                )
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<WallpaperEntity>() {
        override fun areItemsTheSame(oldItem: WallpaperEntity, newItem: WallpaperEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: WallpaperEntity,
            newItem: WallpaperEntity
        ): Boolean {
            return oldItem == newItem
        }
    }
}

