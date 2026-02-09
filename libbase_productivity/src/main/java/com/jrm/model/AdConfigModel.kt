package com.jrm.model

import com.google.gson.annotations.SerializedName
import com.jrm.utils.BaseUtils.getSessionNumber

/**
 * Root model for ad_config JSON (e.g. ad_config_quran_android_1.json).
 * Supports session-based configs: session 1 (first open) vs session 2+ (returning user).
 */
data class AdConfigModel(
    @SerializedName("schema_version") val schemaVersion: Int,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("version") val version: String,
    @SerializedName("session_configs") val sessionConfigs: List<SessionConfig>,
    @SerializedName("ad_item") val adItem: Map<String, AdItem>
) {
    /**
     * Resolves config for a given session number.
     * Session range uses inclusive [from, to]; to == -1 means unbounded (e.g. session 2+).
     */
    fun getSessionConfig(sessionNumber: Int): SessionConfig? {
        return sessionConfigs.find { config ->
            val from = config.sessionRange.from
            val to = config.sessionRange.to
            if (to == -1) sessionNumber >= from else sessionNumber in from..to
        }
    }

    /** Session number used for backward-compat; default 1 (first open). */
    var currentSession: Int = getSessionNumber()


    /** Backward-compat: configs for current session; fallback to session 1 if no match. */
    val configs: Configs?
        get() = getSessionConfig(currentSession)?.configs ?: getSessionConfig(1)?.configs

    /** Backward-compat: screen_obd for current session; fallback to session 1 if no match. */
    val screenObd: ScreenObd?
        get() = getSessionConfig(currentSession)?.screenObd ?: getSessionConfig(1)?.screenObd

    /** Backward-compat: ad_placements for current session; fallback to session 1 if no match. */
    val adPlacements: Map<String, AdPlacement>?
        get() = getSessionConfig(currentSession)?.adPlacements ?: getSessionConfig(1)?.adPlacements
}

data class SessionRange(
    @SerializedName("from") val from: Int,
    @SerializedName("to") val to: Int
)

data class SessionConfig(
    @SerializedName("session_range") val sessionRange: SessionRange,
    @SerializedName("configs") val configs: Configs,
    @SerializedName("screen_obd") val screenObd: ScreenObd,
    @SerializedName("ad_placements") val adPlacements: Map<String, AdPlacement>
)

data class Configs(
    @SerializedName("disable_obd_ad") val disableObdAd: Boolean,
    @SerializedName("disable_all_ad") val disableAllAd: Boolean,
    @SerializedName("interstitial_rule") val interstitialRule: String,
    @SerializedName("time_interstitial_cooldown") val timeInterstitialCooldown: Long,
    @SerializedName("number_finish_obd") val numberFinishObd: Int,
    @SerializedName("time_out_full_screen_ad") val timeOutFullScreenAd: Long,
    @SerializedName("time_out_reward") val timeOutReward: Long
)

data class ScreenObd(
    @SerializedName("splash") val splash: SplashConfig,
    @SerializedName("paywall") val paywall: PaywallConfig,
    @SerializedName("paywall_limited") val paywallLimited: PaywallConfig,
    @SerializedName("language") val language: LanguageConfig,
    @SerializedName("onboarding") val onboarding: OnboardingConfig
)

data class SplashConfig(
    @SerializedName("enable") val enable: Boolean,
    @SerializedName("min_time_splash") val minTimeSplash: Long,
    @SerializedName("time_out_splash") val timeOutSplash: Long,
    @SerializedName("enable_btn_continue") val enableBtnContinue: Boolean
)

data class PaywallConfig(
    @SerializedName("enable") val enable: Boolean,
    @SerializedName("show_after") val showAfter: String? = null,
    @SerializedName("show_session_finish_obd") val showSessionFinishObd: Boolean
)

data class LanguageConfig(
    @SerializedName("is_two_screen") val isTwoScreen: Boolean,
    @SerializedName("enable") val enable: Boolean
)

data class OnboardingConfig(
    @SerializedName("number_screen") val numberScreen: Int,
    @SerializedName("enable") val enable: Boolean
)

data class AdPlacement(
    @SerializedName("type") val type: String,
    @SerializedName("enable") val enable: Boolean,
    @SerializedName("ordered_priority_ads") val orderedPriorityAds: String,
    @SerializedName("session_cap") val sessionCap: Int,
    @SerializedName("max_attempt") val maxAttempt: Int,
    @SerializedName("native_config") val nativeConfig: PlacementNativeConfig? = null
)

data class PlacementNativeConfig(
    @SerializedName("layout_res") val layoutRes: String? = null,
    @SerializedName("meta_layout_res") val metaLayoutRes: String? = null,
    @SerializedName("delay_time") val delayTime: Long? = null,
    @SerializedName("reload_time") val reloadTime: Long? = null
)

data class AdItem(
    @SerializedName("type") val type: String,
    @SerializedName("unit") val unit: String
)
