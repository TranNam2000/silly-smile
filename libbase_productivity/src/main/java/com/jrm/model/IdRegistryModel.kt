package com.jrm.model

import com.google.gson.annotations.SerializedName

data class IdRegistryModel(
    @SerializedName("schema_version") val schemaVersion: Int,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("apps") val apps: List<AppAdConfig>
)

data class AppAdConfig(
    @SerializedName("app_id") val appId: String,
    @SerializedName("app_name") val appName: String,
    @SerializedName("os_type") val osType: String,
    @SerializedName("unit_ids") val unitIds: List<UnitIdConfig>
)

data class UnitIdConfig(
    @SerializedName("ad_name") val adName: String,
    @SerializedName("format") val format: String,
    @SerializedName("network") val network: String,
    @SerializedName("unit_id") val unitId: String,
    @SerializedName("unit_id_test") val unitIdTest: String
)
