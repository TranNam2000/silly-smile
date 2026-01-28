package com.nomyek.myapplication.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ads.nomyek_admob.ads_components.YNMAds
import com.ads.nomyek_admob.ads_components.YNMAdsCallbacks
import com.ads.nomyek_admob.ads_components.ads_native.YNMNativeAdView
import com.ads.nomyek_admob.event.YNMAirBridge
import com.ads.nomyek_admob.utils.AdsNativeMultiPreload
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.android.gms.ads.nativead.NativeAd
import com.jrm.ads.WaterfallNativeAdManager
import com.jrm.utils.AdsHelper
import com.jrm.utils.BaseConstants
import com.jrm.utils.remote_config.RemoteConfigManager
import com.nomyek.myapplication.R
import com.nomyek.myapplication.data.entity.WallpaperEntity
import com.nomyek.myapplication.databinding.ItemWallpaperBinding
import com.nomyek.myapplication.databinding.ItemNativeAdBinding
import com.nomyek.myapplication.utils.GifUtils.parseImagePath

class WallpaperAdapter(
    private val activity: android.app.Activity? = null,
    private val activityName: String = "",
    private val onItemClick: ((WallpaperEntity) -> Unit)? = null,
    private val onFavoriteClick: ((WallpaperEntity, Boolean) -> Unit)? = null,
    private val isShowFavorite: Boolean = true
) : ListAdapter<HomeListItem, RecyclerView.ViewHolder>(HomeListItemDiffCallback()) {

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is HomeListItem.WallpaperItem -> VIEW_TYPE_WALLPAPER
            is HomeListItem.NativeAdItem -> VIEW_TYPE_NATIVE_AD
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_WALLPAPER -> {
                val binding = ItemWallpaperBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                WallpaperViewHolder(binding)
            }
            VIEW_TYPE_NATIVE_AD -> {
                val binding = ItemNativeAdBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                NativeAdViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is WallpaperViewHolder -> {
                val item = getItem(position) as HomeListItem.WallpaperItem
                val isLastItem = position == itemCount - 1
                holder.bind(item.wallpaper, isLastItem)
            }
            is NativeAdViewHolder -> {
                holder.bind()
            }
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            if (holder is WallpaperViewHolder && payloads.contains(PAYLOAD_FAVORITE_CHANGED)) {
                val item = getItem(position) as HomeListItem.WallpaperItem
                holder.updateFavoriteIcon(item.wallpaper.isFavorite)
                val isLastItem = position == itemCount - 1
                holder.updateBottomMargin(isLastItem)
            }
        }
    }

    // Native Ad ViewHolder
    inner class NativeAdViewHolder(
        private val binding: ItemNativeAdBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind() {
            // Native ad view will be loaded by the ad manager
            if (AdsHelper.isDisableAllAd()) {
                binding.nativeAdView.visibility = android.view.View.GONE
                return
            }
            binding.nativeAdView.visibility = android.view.View.VISIBLE
            val listAdId = RemoteConfigManager.instance!!.getListAdIdNativeFromRemote(BaseConstants.NATIVE_HOME,
                RemoteConfigManager.instance!!.homeNativeIds)
            // Only load ads if activity is provided
            activity?.let { act ->
                AdsNativeMultiPreload.preloadMultipleNativeAds(
                    act,
                    YNMAirBridge.AppData(activityName, BaseConstants.NATIVE_HOME),
                    listAdId,
                    BaseConstants.NATIVE_HOME,
                    object : YNMAdsCallbacks() {
                        override fun onNativeAdLoaded(nativeAd: NativeAd) {
                            super.onNativeAdLoaded(nativeAd)
                            // Show the native ad in the native ad view if available
                            binding.nativeAdView?.let { adView ->
                                AdsNativeMultiPreload.showPreloadedNativeAd(
                                    act,
                                    adView,
                                    BaseConstants.NATIVE_HOME,
                                    com.jrm.R.layout.custom_native_admob_medium,
                                    com.jrm.R.layout.custom_native_admob_medium
                                )
                            }
                        }
                    }
                )

            }
        }
    }

    // Wallpaper ViewHolder
    inner class WallpaperViewHolder(
        val binding: ItemWallpaperBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(wallpaper: WallpaperEntity, isLastItem: Boolean = false) {
            binding.ivFavorite.isVisible = isShowFavorite
            loadWallpaper(wallpaper)
            // Set favorite icon
            updateFavoriteIcon(wallpaper.isFavorite)

            // Add extra bottom margin for last item
            updateBottomMargin(isLastItem)

            // Handle card click
            binding.cardWallpaper.setOnClickListener {
                onItemClick?.invoke(wallpaper)
            }

            // Handle favorite icon click
            binding.ivFavorite.setOnClickListener {
                val currentPosition = bindingAdapterPosition
                if (currentPosition != RecyclerView.NO_POSITION) {
                    val currentItem = getItem(currentPosition)
                    if (currentItem is HomeListItem.WallpaperItem) {
                        val newFavoriteState = !currentItem.wallpaper.isFavorite
                        onFavoriteClick?.invoke(currentItem.wallpaper, newFavoriteState)
                    }
                }
            }
        }

        fun updateBottomMargin(isLastItem: Boolean) {
            val layoutParams = binding.root.layoutParams as ViewGroup.MarginLayoutParams
            val extraBottomMargin = if (isLastItem) {
                binding.root.context.resources.getDimensionPixelSize(R.dimen.space)
            } else {
                0
            }
            layoutParams.bottomMargin = extraBottomMargin
            binding.root.layoutParams = layoutParams
        }

        private fun loadWallpaper(wallpaper: WallpaperEntity) {
            val imagePath = wallpaper.getDisplayUrl()


            binding.ivWallpaper.setImageDrawable(null)

            val imageSource = parseImagePath(imagePath)

            if (wallpaper.isAnimated) {
                loadAnimatedGif(imageSource)
            } else {
                loadStaticImage(imageSource)
            }
        }



        /**
         * Load Animated GIF với Shimmer effect
         * @param source: String URL, Resource URI, or Resource ID
         */
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

        /**
         * Load Static Image with optimization
         * @param source: String URL, Resource URI, or Resource ID
         */
        private fun loadStaticImage(source: Any) {
            val context = binding.ivWallpaper.context
            // Check if context is an Activity and if it's destroyed
            if (context is android.app.Activity && (context.isFinishing || context.isDestroyed)) {
                return
            }
            
            try {
                val requestOptions = RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()

                Glide.with(context)
                    .load(source)
                    .apply(requestOptions)
                    .into(binding.ivWallpaper)
            } catch (e: IllegalArgumentException) {
                // Activity is destroyed, ignore
            }
        }

        fun updateFavoriteIcon(isFavorite: Boolean) {
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

    class HomeListItemDiffCallback : DiffUtil.ItemCallback<HomeListItem>() {
        override fun areItemsTheSame(oldItem: HomeListItem, newItem: HomeListItem): Boolean {
            return when {
                oldItem is HomeListItem.WallpaperItem && newItem is HomeListItem.WallpaperItem -> {
                    oldItem.wallpaper.id == newItem.wallpaper.id
                }
                oldItem is HomeListItem.NativeAdItem && newItem is HomeListItem.NativeAdItem -> {
                    true
                }
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: HomeListItem, newItem: HomeListItem): Boolean {
            return when {
                oldItem is HomeListItem.WallpaperItem && newItem is HomeListItem.WallpaperItem -> {
                    oldItem.wallpaper == newItem.wallpaper
                }
                oldItem is HomeListItem.NativeAdItem && newItem is HomeListItem.NativeAdItem -> {
                    true
                }
                else -> false
            }
        }

        override fun getChangePayload(oldItem: HomeListItem, newItem: HomeListItem): Any? {
            if (oldItem is HomeListItem.WallpaperItem && newItem is HomeListItem.WallpaperItem) {
                val oldWallpaper = oldItem.wallpaper
                val newWallpaper = newItem.wallpaper
                if (oldWallpaper.id == newWallpaper.id &&
                    oldWallpaper.isFavorite != newWallpaper.isFavorite &&
                    oldWallpaper.copy(isFavorite = newWallpaper.isFavorite) == newWallpaper
                ) {
                    return PAYLOAD_FAVORITE_CHANGED
                }
            }
            return null
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        super.onViewDetachedFromWindow(holder)
    }

    companion object {
        private const val PAYLOAD_FAVORITE_CHANGED = "favorite_changed"
        private const val VIEW_TYPE_WALLPAPER = 0
        private const val VIEW_TYPE_NATIVE_AD = 1
    }
}
