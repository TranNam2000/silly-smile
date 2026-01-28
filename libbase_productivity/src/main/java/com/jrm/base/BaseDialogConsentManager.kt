package com.jrm.base
import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform
import com.jrm.utils.purchase.IAPHelper
import java.text.SimpleDateFormat
import java.util.Date

abstract class BaseDialogConsentManager {
    protected var isDialogLoading = false
    protected var consentInformation: ConsentInformation? = null
    private var consentForm: ConsentForm? = null
    private var dialog: ProgressDialog? = null
    private var retryTimes = 0
    fun showDialog(
        activity: Activity,
        showProgress: Boolean,
        onDismissedListener: ConsentForm.OnConsentFormDismissedListener
    ) {
        if (isDialogLoading) {
            if (showProgress) {
                dialog = ProgressDialog(activity)
                dialog!!.setMessage("Loading ...")
                dialog!!.setCancelable(false)
                dialog!!.show()
            }
            Thread {
                while (isDialogLoading) {
                    try {
                        Thread.sleep(30)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                }
                activity.runOnUiThread {
                    if (dialog != null) {
                        dialog!!.dismiss()
                    }
                    showDialog(activity, showProgress, onDismissedListener)
                }
            }.start()
        } else {
            if (consentForm != null) {
                showConsentDialog(consentForm!!, activity, onDismissedListener)
                consentForm = null
            } else {
                onDismissedListener.onConsentFormDismissed(null)
            }
            loadDialogForm(activity, null)
        }
    }

    fun loadDialogForm(
        activity: Activity,
        loadFailListener: UserMessagingPlatform.OnConsentFormLoadFailureListener?
    ) {
        if (isDialogLoading) {
            return
        }
        isDialogLoading = true
        consentInformation = getConsentInformation(activity)
        
        val startTime = System.currentTimeMillis()
        Log.d("ConsentDialog", "requestConsentInfoUpdate started at: ${
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(
                Date(startTime)
            )}")
        
        consentInformation!!.requestConsentInfoUpdate(
            activity,
            getParams(activity),
            {
                val endTime = System.currentTimeMillis()
                val duration = endTime - startTime
                Log.d("ConsentDialog", "requestConsentInfoUpdate completed successfully at: ${
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(
                        Date(endTime)
                    )}")
                Log.d("ConsentDialog", "requestConsentInfoUpdate duration: ${duration}ms")
                
                UserMessagingPlatform.loadConsentForm(
                    activity.application,
                    { consentForm: ConsentForm? ->
                        this.consentForm = consentForm
                        isDialogLoading = false
                        Log.d("ConsentDialog", "Consent form loaded successfully, ready to show popup")
                    }) { formError: FormError ->
                    if (loadFailListener != null) {
                        Log.d("LoadConsentForm:", formError.message)
                        retryTimes++
                        if (retryTimes == 3) {
                            retryTimes = 0
                            loadFailListener.onConsentFormLoadFailure(formError)
                        }
                    }
                    isDialogLoading = false
                    loadDialogForm(activity, loadFailListener)
                }
            }
        ) { requestConsentError: FormError ->
            // Consent gathering failed.
            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime
            Log.d("ConsentDialog", "requestConsentInfoUpdate failed at: ${
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(
                    Date(endTime)
                )}")
            Log.d("ConsentDialog", "requestConsentInfoUpdate failed duration: ${duration}ms")
            Log.e("ConsentDialog", "Error: ${requestConsentError.errorCode} - ${requestConsentError.message}")
            
            retryTimes++
            if (retryTimes == 3) {
                retryTimes = 0
                onLoadFail(requestConsentError, loadFailListener)
            }
            isDialogLoading = false
            loadDialogForm(activity, loadFailListener)
        }
    }

    protected fun onLoadFail(
        requestConsentError: FormError,
        listener: UserMessagingPlatform.OnConsentFormLoadFailureListener?
    ) {
        Log.w(
            "ConsentTag", String.format(
                "%s: %s",
                requestConsentError.errorCode,
                requestConsentError.message
            )
        )
        listener?.onConsentFormLoadFailure(null)
    }

    private fun showConsentDialog(
        consentForm: ConsentForm,
        activity: Activity,
        dismissedListener: ConsentForm.OnConsentFormDismissedListener?
    ) {
        BaseEventLogger.logCustomEvent("show_gdpr")
        val isConsentedPrev = canRequestAds(activity)
        val showTime = System.currentTimeMillis()
        Log.d("ConsentDialog", "Consent popup shown at: ${
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(
                Date(showTime)
            )}")
        
        try {
            consentForm.show(activity) { formError: FormError? ->
                val dismissTime = System.currentTimeMillis()
                val displayDuration = dismissTime - showTime
                Log.d("ConsentDialog", "Consent popup dismissed at: ${
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(
                        Date(dismissTime)
                    )}")
                Log.d("ConsentDialog", "Consent popup display duration: ${displayDuration}ms")
                
                if (formError != null) {
                    Log.e("ConsentDialog", "Consent form dismissed with error: ${formError.errorCode} - ${formError.message}")
                } else {
                    Log.d("ConsentDialog", "Consent form dismissed successfully")
                }
                
//                if (canRequestAds(activity)) {
//                    AppOpenManager.getInstance().enableAppResume()
//                } else {
//                    AppOpenManager.getInstance().disableAppResume()
//                }
                trackingAdsType(activity, isConsentedPrev)
                dismissedListener?.onConsentFormDismissed(formError)
            }
        } catch (e: Exception) {
            Log.e("ConsentDialog", "Exception showing consent form", e)
            e.printStackTrace()
        }
    }

    private fun trackingAdsType(activity: Activity, isConsentedPrev: Boolean) {
        if (canShowAds(activity)) {
            if (!isConsentedPrev && !isFirstTracking(activity)) {
                BaseEventLogger.logCustomEvent("accept_show_ads")
            }
            if (canShowPersonalizedAds(activity)) {
                BaseEventLogger.logCustomEvent("show_person_ads")
                return
            }
            if (canShowNonPersonalAds(activity)) {
                BaseEventLogger.logCustomEvent("show_non_person_ads")
                return
            }
        } else {
            if (isConsentedPrev) {
                BaseEventLogger.logCustomEvent("revoke_show_ads")
            }
            BaseEventLogger.logCustomEvent("not_show_ads")
        }
    }

    private fun isFirstTracking(activity: Activity): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
        val isFirst = prefs.getBoolean(IS_FIRST_TRACKING, true)
        if (isFirst) {
            val editor = prefs.edit()
            editor.putBoolean(IS_FIRST_TRACKING, false)
            editor.apply()
        }
        return isFirst
    }

    protected fun consentRequired(activity: Activity?): Boolean {
        val prefs =PreferenceManager.getDefaultSharedPreferences(activity)
        return prefs.getBoolean(IS_REQUIRED_CONSENT, false) && !IAPHelper.isPremium()
    }

    protected fun setRequiredConsentFirstOpen(activity: Activity) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
        val isFirst = prefs.getBoolean(IS_FIRST_REQUEST_CONSENT, true)
        if (isFirst) {
            val editor = prefs.edit()
            editor.putBoolean(IS_FIRST_REQUEST_CONSENT, false)
            editor.apply()
            editor.putBoolean(
                IS_REQUIRED_CONSENT,
                getConsentInformation(activity).consentStatus == ConsentInformation.ConsentStatus.REQUIRED
            )
            editor.apply()
        }
    }

    fun requestConsentInfor(
        activity: Activity,
        successListener: ConsentInformation.OnConsentInfoUpdateSuccessListener?,
        failureListener: ConsentInformation.OnConsentInfoUpdateFailureListener?
    ) {
        consentInformation = UserMessagingPlatform.getConsentInformation(activity.application)
        consentInformation?.requestConsentInfoUpdate(
            activity,
            getParams(activity),
            ConsentInformation.OnConsentInfoUpdateSuccessListener { successListener?.onConsentInfoUpdateSuccess() },
            ConsentInformation.OnConsentInfoUpdateFailureListener { requestConsentError: FormError ->
                // Consent gathering failed.
                failureListener?.onConsentInfoUpdateFailure(requestConsentError)
                Log.w(
                    "ConsentTag", String.format(
                        "%s: %s",
                        requestConsentError.errorCode,
                        requestConsentError.message
                    )
                )
            })
    }

    protected fun getParams(activity: Activity?): ConsentRequestParameters {
        return if (IS_SHOW_TEST_CONSENT) {
            val debugSettings =
                ConsentDebugSettings.Builder(activity)
                    .addTestDeviceHashedId("495ADA41523A0ADC48950EB9BA26AA77")
                    .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                    .build()
            ConsentRequestParameters.Builder()
                .setTagForUnderAgeOfConsent(false)
                .setConsentDebugSettings(debugSettings)
                .build()
        } else {
            ConsentRequestParameters.Builder()
                .setTagForUnderAgeOfConsent(false)
                .build()
        }
    }

    protected fun getConsentInformation(activity: Activity): ConsentInformation {
        return UserMessagingPlatform.getConsentInformation(activity.application)
    }

    fun canRequestAds(activity: Activity): Boolean {
        requestConsentInfor(activity, null, null)
        return canShowAds(activity)
    }

    private fun canShowNonPersonalAds(activity: Activity): Boolean {
        return !canShowPersonalizedAds(activity) && canShowAds(activity)
    }

    private fun canShowAds(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val purposeConsent = prefs.getString(KEY_PURPOSE_CONSENTS_STRING, "")
        val vendorConsent = prefs.getString(KEY_VENDOR_CONSENTS_STRING, "")
        val vendorLI = prefs.getString(KEY_VENDOR_LEGI_INSTEREST_STRING, "")
        val purposeLI = prefs.getString(KEY_PUR_LEGI_INTEREST_STRING, "")
        val googleId = 755
        val hasGoogleVendorConsent = hasAttribute(vendorConsent, googleId)
        val hasGoogleVendorLI = hasAttribute(vendorLI, googleId)

        // Minimum required for at least non-personalized ads
        return (hasConsentFor(intArrayOf(1), purposeConsent, hasGoogleVendorConsent)
                && hasConsentOrLegitimateInterestFor(
            intArrayOf(2, 7, 9, 10),
            purposeConsent,
            purposeLI,
            hasGoogleVendorConsent,
            hasGoogleVendorLI
        ))
    }

    fun canShowPersonalizedAds(context: Context?): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val purposeConsent = prefs.getString(KEY_PURPOSE_CONSENTS_STRING, "")
        val vendorConsent = prefs.getString(KEY_VENDOR_CONSENTS_STRING, "")
        val vendorLI = prefs.getString(KEY_VENDOR_LEGI_INSTEREST_STRING, "")
        val purposeLI = prefs.getString(KEY_PUR_LEGI_INTEREST_STRING, "")
        val googleId = 755
        val hasGoogleVendorConsent = hasAttribute(vendorConsent, googleId)
        val hasGoogleVendorLI = hasAttribute(vendorLI, googleId)
        return (hasConsentFor(intArrayOf(1, 3, 4), purposeConsent, hasGoogleVendorConsent)
                && hasConsentOrLegitimateInterestFor(
            intArrayOf(2, 7, 9, 10),
            purposeConsent,
            purposeLI,
            hasGoogleVendorConsent,
            hasGoogleVendorLI
        ))
    }

    private fun hasAttribute(input: String?, index: Int): Boolean {
        return input!!.length >= index && input[index - 1] == '1'
    }

    private fun hasConsentFor(
        purposes: IntArray,
        purposeConsent: String?,
        hasVendorConsent: Boolean
    ): Boolean {
        for (p in purposes) {
            if (!hasAttribute(purposeConsent, p)) {
                return false
            }
        }
        return hasVendorConsent
    }

    private fun hasConsentOrLegitimateInterestFor(
        purposes: IntArray,
        purposeConsent: String?,
        purposeLI: String?,
        hasVendorConsent: Boolean,
        hasVendorLI: Boolean
    ): Boolean {
        for (p in purposes) {
            if (hasAttribute(purposeLI, p) && hasVendorLI || hasAttribute(
                    purposeConsent,
                    p
                ) && hasVendorConsent
            ) {
                return true
            }
        }
        return false
    }

    fun resetDevice(activity: Activity) {
        val consentInformation = UserMessagingPlatform.getConsentInformation(activity.application)
        consentInformation.reset()
    }

    companion object {
        private const val IS_SHOW_TEST_CONSENT = false
        private const val IS_REQUIRED_CONSENT = "IS_REQUIRED_CONSENT"
        private const val IS_FIRST_REQUEST_CONSENT = "IS_FIRST_REQUEST_CONSENT"
        private const val KEY_PURPOSE_CONSENTS_STRING = "IABTCF_PurposeConsents"
        private const val KEY_VENDOR_CONSENTS_STRING = "IABTCF_VendorConsents"
        private const val KEY_VENDOR_LEGI_INSTEREST_STRING = "IABTCF_VendorLegitimateInterests"
        private const val KEY_PUR_LEGI_INTEREST_STRING = "IABTCF_PurposeLegitimateInterests"
        private const val IS_FIRST_TRACKING = "IS_FIRST_TRACKING"
    }
}