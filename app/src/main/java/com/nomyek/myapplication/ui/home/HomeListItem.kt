package com.nomyek.myapplication.ui.home

import com.nomyek.myapplication.data.entity.WallpaperEntity

sealed class HomeListItem {
    data class WallpaperItem(val wallpaper: WallpaperEntity) : HomeListItem()
    object NativeAdItem : HomeListItem()
}
