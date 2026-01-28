package com.jrm.onboarding.consent_dialog

import android.app.Activity
import android.util.Log
import com.ads.nomyek_admob.event.YNMAirBridge
import com.applovin.sdk.AppLovinPrivacySettings
import com.bytedance.sdk.openadsdk.api.PAGConstant
import com.google.ads.mediation.inmobi.InMobiConsent
import com.google.ads.mediation.pangle.PangleMediationAdapter
import com.google.android.ump.ConsentInformation
import com.google.android.ump.UserMessagingPlatform
import com.inmobi.sdk.InMobiSdk
import com.mbridge.msdk.MBridgeConstans
import com.mbridge.msdk.out.MBridgeSDKFactory
import com.moloco.sdk.publisher.privacy.MolocoPrivacy
import com.jrm.base.BaseDialogConsentManager
import com.jrm.utils.SharedPref
import com.jrm.utils.purchase.IAPHelper
import org.json.JSONException
import org.json.JSONObject

class ConsentDialogManager : BaseDialogConsentManager() {
    private var rejectSplashCount: Long = 0
    private var buttonClickCount: Long = 0
    fun isPrivacyOptionsRequired(): Boolean {
        return (consentInformation!!.getPrivacyOptionsRequirementStatus()
                == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED)
    }
    enum class ConsentDialogState {
        ACCEPTED,
        REJECTED,
        NOT_REQUIRED
    }

    interface ConsentDialogListener {
        fun onConsentFormDismissed(state: ConsentDialogState)
    }

    fun showDialogConsentMonkey(activity: Activity, listener: ConsentDialogListener) {
        IAPHelper.isPremium()
        if (IAPHelper.isPremium()) {
            listener.onConsentFormDismissed(ConsentDialogState.NOT_REQUIRED)
            return
        }
        //            // Create a ConsentRequestParameters object.
        consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        consentInformation!!.requestConsentInfoUpdate(
            activity,
            getParams(activity),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                    activity
                ) { loadAndShowError ->
                    if (loadAndShowError != null) {
                        // Consent gathering failed.
                        Log.w("TAG", "${loadAndShowError.errorCode}: ${loadAndShowError.message}")
                    }

                    if (isPrivacyOptionsRequired()) {
                        SharedPref.isShowPolicychange = true
                    }

                    // Consent has been gathered.
                    if (consentInformation!!.canRequestAds()) {
                        // mediation consent api EU
                        PangleMediationAdapter.setGDPRConsent(PAGConstant.PAGGDPRConsentType.PAG_GDPR_CONSENT_TYPE_CONSENT)
                        PangleMediationAdapter.setPAConsent(PAGConstant.PAGPAConsentType.PAG_PA_CONSENT_TYPE_CONSENT)
                        AppLovinPrivacySettings.setHasUserConsent(true, activity)
                        val mBridgeSDK = MBridgeSDKFactory.getMBridgeSDK()
                        mBridgeSDK.setConsentStatus(activity, MBridgeConstans.IS_SWITCH_ON)

                        // mediation consent api US
                        mBridgeSDK.setDoNotTrackStatus(activity, false)
                        listener.onConsentFormDismissed(ConsentDialogState.ACCEPTED)
                        YNMAirBridge.getInstance().logCustomEvent("consent", "accept")
                        val consentObject = JSONObject()
                        try {
                            consentObject.put(InMobiSdk.IM_GDPR_CONSENT_AVAILABLE, true)
                            consentObject.put("gdpr", "1")
                        } catch (exception: JSONException) {
                            exception.printStackTrace()
                        }
                        InMobiConsent.updateGDPRConsent(consentObject)
                        val privacySettings = MolocoPrivacy.PrivacySettings(isUserConsent = true)
                        MolocoPrivacy.setPrivacy(privacySettings)
                    } else {
                        AppLovinPrivacySettings.setHasUserConsent(false, activity)
                        listener.onConsentFormDismissed(ConsentDialogState.REJECTED)
                        YNMAirBridge.getInstance().logCustomEvent("consent", "reject")
                    }
                }
            }
        ) { requestConsentError ->
            // Consent gathering failed.
            Log.w("TAG", "${requestConsentError.errorCode}: ${requestConsentError.message}")
            YNMAirBridge.getInstance().logCustomEvent("consent", "error")
            listener.onConsentFormDismissed(ConsentDialogState.REJECTED)
        }

    }

    companion object {
        private var INSTANCE: ConsentDialogManager? = null
        val instance: ConsentDialogManager?
            get() {
                if (INSTANCE == null) {
                    INSTANCE = ConsentDialogManager()
                }
                return INSTANCE
            }
    }
}