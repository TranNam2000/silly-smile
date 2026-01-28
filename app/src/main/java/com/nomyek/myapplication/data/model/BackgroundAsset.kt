package com.nomyek.myapplication.data.model

import com.google.gson.annotations.SerializedName

/**
 * Model cho background asset từ background_asset.json
 */
data class BackgroundAsset(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("preview")
    val preview: String,
    
    @SerializedName("category")
    val category: String,
    
    @SerializedName("background")
    val background: String
)

