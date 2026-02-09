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
        val placement = config?.adPlacements?.get(placeName) ?: return emptyList()

        val orderedAds = placement.orderedPriorityAds
        if (orderedAds.isEmpty()) {

            return emptyList()
        }

        // Load id_registry
        val idRegistry = RemoteConfigManager.Companion.instance?.idRegistry ?: return emptyList()

        val totalUnitIds = idRegistry.apps.firstOrNull()?.unitIds?.size ?: 0

        // Split by semicolon and get ad item IDs
        val adItemIds = orderedAds.split(";").map { it.trim() }.filter { it.isNotEmpty() }

        // Build UnitIdConfig list with actual ad IDs from registry
        val unitIdConfigList = adItemIds.mapIndexedNotNull { index, adItemId ->


            val adItem = config.adItem[adItemId] ?: return@mapIndexedNotNull null

            val unitName = adItem.unit
            if (unitName.isEmpty()) {
                return@mapIndexedNotNull null
            }

            // Find the UnitIdConfig from id_registry by app_id and ad_name
            val registryUnitId = idRegistry.apps
                .filter { it.appId == "quran.muslim.prayapp" }
                .flatMap { it.unitIds }
                .find { it.adName == unitName }

            if (registryUnitId == null) {

                return@mapIndexedNotNull null
            }
            registryUnitId
        }

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