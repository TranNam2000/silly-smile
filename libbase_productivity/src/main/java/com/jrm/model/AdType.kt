package com.jrm.model

import com.google.gson.annotations.SerializedName

/**
 * Enum representing different types of ad placements in the application.
 * Each placement type defines how and where the ad should be displayed.
 *
 * @property value The JSON value used in ad configuration
 * @property requiresNativeConfig Whether this placement type requires native configuration
 * @property description Human-readable description of the ad type
 */
enum class AdPlacementType(
    val value: String,
    val requiresNativeConfig: Boolean,
    val description: String
) {
    /**
     * Native ad displayed as a view component embedded in the UI.
     * Requires layout configuration for proper rendering.
     */
    @SerializedName("native_view")
    NATIVE_VIEW(
        value = "native_view",
        requiresNativeConfig = true,
        description = "Native ad displayed as an embedded view component"
    ),

    /**
     * Native ad displayed as a full-screen native (FSN) format.
     * Typically shown with a delay and custom full-screen layout.
     */
    @SerializedName("native_fsn")
    NATIVE_FSN(
        value = "native_fsn",
        requiresNativeConfig = true,
        description = "Full-screen native ad with custom layout and delay"
    ),

    /**
     * Full-screen ad that can be either interstitial or native FSN.
     * May include native configuration for fallback native display.
     */
    @SerializedName("full_screen_ad")
    FULL_SCREEN_AD(
        value = "full_screen_ad",
        requiresNativeConfig = false,
        description = "Full-screen interstitial or native ad"
    ),

    /**
     * Standard interstitial ad displayed between app transitions.
     * Does not require native configuration.
     */
    @SerializedName("interstitial")
    INTERSTITIAL(
        value = "interstitial",
        requiresNativeConfig = false,
        description = "Standard interstitial ad shown between screens"
    ),

    /**
     * Rewarded ad that gives users an incentive for watching.
     * Does not require native configuration.
     */
    @SerializedName("rewarded_ad")
    REWARDED_AD(
        value = "rewarded_ad",
        requiresNativeConfig = false,
        description = "Rewarded ad with user incentives"
    ),

    /**
     * Banner ad displayed in a fixed position.
     * May use native rendering for better integration.
     */
    @SerializedName("banner")
    BANNER(
        value = "banner",
        requiresNativeConfig = false,
        description = "Banner ad in fixed position"
    );

    companion object {
        /**
         * Get AdPlacementType from string value.
         * @param value The string value from JSON config
         * @return Corresponding AdPlacementType or null if not found
         */
        fun fromValue(value: String): AdPlacementType? {
            return entries.find { it.value == value }
        }

        /**
         * Get all placement types that require native configuration.
         * @return List of placement types requiring native config
         */
        fun getNativeTypes(): List<AdPlacementType> {
            return entries.filter { it.requiresNativeConfig }
        }

        /**
         * Get all full-screen placement types.
         * @return List of full-screen placement types
         */
        fun getFullScreenTypes(): List<AdPlacementType> {
            return listOf(NATIVE_FSN, FULL_SCREEN_AD, INTERSTITIAL, REWARDED_AD)
        }
    }

    /**
     * Check if this placement type is a native ad variant.
     */
    fun isNativeType(): Boolean = this in listOf(NATIVE_VIEW, NATIVE_FSN)

    /**
     * Check if this placement type is a full-screen format.
     */
    fun isFullScreen(): Boolean = this in Companion.getFullScreenTypes()
}

/**
 * Enum representing the actual ad unit types used in ad items.
 * These correspond to the actual ad network formats (AdMob, Meta, etc.)
 *
 * @property value The JSON value used in ad item configuration
 * @property description Human-readable description
 */
enum class AdItemType(
    val value: String,
    val description: String
) {
    /**
     * Native ad unit from ad network.
     */
    @SerializedName("native")
    NATIVE(
        value = "native",
        description = "Native ad from ad network"
    ),

    /**
     * Interstitial ad unit from ad network.
     */
    @SerializedName("interstitial")
    INTERSTITIAL(
        value = "interstitial",
        description = "Interstitial ad from ad network"
    ),

    /**
     * Rewarded ad unit from ad network.
     */
    @SerializedName("rewarded")
    REWARDED(
        value = "rewarded",
        description = "Rewarded ad from ad network"
    ),

    /**
     * Banner ad unit from ad network.
     */
    @SerializedName("banner")
    BANNER(
        value = "banner",
        description = "Banner ad from ad network"
    ),

    /**
     * App open ad unit (shown when app opens/resumes).
     */
    @SerializedName("app_open")
    APP_OPEN(
        value = "app_open",
        description = "App open ad shown on app launch"
    );

    companion object {
        /**
         * Get AdItemType from string value.
         * @param value The string value from JSON config
         * @return Corresponding AdItemType or null if not found
         */
        fun fromValue(value: String): AdItemType? {
            return entries.find { it.value == value }
        }
    }
}

/**
 * Extension function to validate if an AdPlacementType is compatible with an AdItemType.
 * @param itemType The ad item type to check compatibility with
 * @return true if compatible, false otherwise
 */
fun AdPlacementType.isCompatibleWith(itemType: AdItemType): Boolean {
    return when (this) {
        AdPlacementType.NATIVE_VIEW, AdPlacementType.NATIVE_FSN -> 
            itemType == AdItemType.NATIVE
        
        AdPlacementType.INTERSTITIAL -> 
            itemType == AdItemType.INTERSTITIAL
        
        AdPlacementType.FULL_SCREEN_AD -> 
            itemType in listOf(AdItemType.INTERSTITIAL, AdItemType.NATIVE)
        
        AdPlacementType.REWARDED_AD -> 
            itemType == AdItemType.REWARDED
        
        AdPlacementType.BANNER -> 
            itemType in listOf(AdItemType.BANNER, AdItemType.NATIVE)
    }
}
