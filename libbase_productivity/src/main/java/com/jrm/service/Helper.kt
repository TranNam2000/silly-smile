package com.jrm.service

import com.ads.nomyek_admob.utils.AdsNativeMultiPreload
import com.jrm.model.AdConfigModel
import com.jrm.service.NativeService.TAG
import com.jrm.utils.Logger
import com.jrm.model.UnitIdConfig
import com.jrm.utils.remote_config.RemoteConfigManager

object Helper {
    /**
     * Build List<UnitIdConfig> from AdConfigModel placement.
     * This directly returns UnitIdConfig list ready for WaterfallManager.preload()
     *
     * @param config AdConfigModel
     * @param placeName Placement ID (e.g., "splash_ad_view")
     * @return List<UnitIdConfig> with actual IDs from id_registry
     */
    fun buildUnitIdConfigList(
        config: AdConfigModel?,
        placeName: String,
    ): List<UnitIdConfig> {
        Logger.d("===== buildUnitIdConfigList START =====")
        Logger.d("placementId: $placeName")

        val placement = config?.adPlacements?.get(placeName)
        if (placement == null) {
            Logger.e( "Placement not found: $placeName")
            return emptyList()
        }

        val orderedAds = placement.orderedPriorityAds
        if (orderedAds.isEmpty()) {
            Logger.e( "ordered_priority_ads is empty for $placeName")
            return emptyList()
        }

        Logger.d("ordered_priority_ads: $orderedAds")

        // Load id_registry
        val idRegistry = RemoteConfigManager.Companion.instance?.idRegistry
        if (idRegistry == null) {
            Logger.e( "Failed to load id_registry")
            return emptyList()
        }

        val totalUnitIds = idRegistry.apps.firstOrNull()?.unitIds?.size ?: 0
        Logger.d("id_registry loaded: $totalUnitIds unit IDs available")

        // Split by semicolon and get ad item IDs
        val adItemIds = orderedAds.split(";").map { it.trim() }.filter { it.isNotEmpty() }
        Logger.d("Split into ${adItemIds.size} ad items: $adItemIds")

        // Build UnitIdConfig list with actual ad IDs from registry
        val unitIdConfigList = adItemIds.mapIndexedNotNull { index, adItemId ->
            Logger.d("--- Processing item ${index + 1}/${adItemIds.size}: $adItemId ---")

            val adItem = config.adItem[adItemId]
            if (adItem == null) {
                Logger.w( "  ❌ ad_item not found for: $adItemId")
                return@mapIndexedNotNull null
            }

            Logger.d("  ✓ ad_item found - type: ${adItem.type}, unit: ${adItem.unit}")

            val unitName = adItem.unit
            if (unitName.isEmpty()) {
                Logger.w( "  ❌ unit is empty")
                return@mapIndexedNotNull null
            }

            // Find the UnitIdConfig from id_registry by app_id and ad_name
            val registryUnitId = idRegistry.apps
                .filter { it.appId == "quran.muslim.prayapp" }
                .flatMap { it.unitIds }
                .find { it.adName == unitName }

            if (registryUnitId == null) {
                Logger.w( "  ❌ Ad ID not found in registry for: $unitName")
                return@mapIndexedNotNull null
            }

            Logger.d(
                "  ✓ Registry lookup: $unitName → unitId=${registryUnitId.unitId}, unitIdTest=${registryUnitId.unitIdTest}, network=${registryUnitId.network}, format=${registryUnitId.format}"
            )

            registryUnitId
        }

        Logger.d("===== FINAL RESULT =====")
        Logger.d("placementId: $placeName")
        Logger.d("UnitIdConfig items: ${unitIdConfigList.size}")
        unitIdConfigList.forEachIndexed { idx, unitId ->
            Logger.d(
                "  [$idx] format=${unitId.format}, adName=${unitId.adName}, network=${unitId.network}"
            )
        }
        Logger.d("========================")

        return unitIdConfigList
    }

    /**
     * Converts [List] of [UnitIdConfig] to [List] of [AdsNativeMultiPreload.AdIdModel].
     * Uses test ad IDs when [com.jrm.BuildConfig.DEBUG] is true.
     *
     * @param unitIdConfigList List from [buildUnitIdConfigList]
     * @return List ready for [AdsNativeMultiPreload.preloadMultipleNativeAds]
     */
    fun convertToAdIdModelList(
        unitIdConfigList: List<UnitIdConfig>
    ): List<AdsNativeMultiPreload.AdIdModel> {
        return unitIdConfigList.map { unitIdConfig ->
            AdsNativeMultiPreload.AdIdModel().apply {
                adId = if (com.jrm.BuildConfig.DEBUG) {
                    unitIdConfig.unitIdTest
                } else {
                    unitIdConfig.unitId
                }
                adName = unitIdConfig.adName
            }
        }
    }
}