package com.nomyek.myapplication.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.jrm.base.BaseFragment
import com.jrm.onboarding.language.Language2Activity
import com.jrm.utils.BaseExtension
import com.nomyek.myapplication.databinding.FragmentSettingBinding
import com.nomyek.myapplication.utils.click
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingFragment : BaseFragment<FragmentSettingBinding>(FragmentSettingBinding::inflate) {

    override fun initView() {
    }


    override fun addEvent() {
        binding?.apply {
            layoutPremiumBanner.click {
                Toast.makeText(requireContext(), "Unlock Premium", Toast.LENGTH_SHORT).show()
            }

            btnGetNow.click {
                Toast.makeText(requireContext(), "Get Premium Now", Toast.LENGTH_SHORT).show()
            }

            itemLanguage.click {
                Toast.makeText(requireContext(), "Select Language", Toast.LENGTH_SHORT).show()
                BaseExtension.showActivity(requireContext(), Language2Activity::class.java, null)
            }

            itemFeedback.click {
                sendFeedbackEmail()
            }

            itemRateApp.click {
                openPlayStore()
            }

            // Theme and Proxy
            itemTheme.click {
                Toast.makeText(requireContext(), "Theme Settings", Toast.LENGTH_SHORT).show()
            }

            itemPrivacy.click {
                openUrl("https://sites.google.com/view/castai")
            }
        }
    }

    private fun openUrl(strUrl: String?) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(strUrl))
        intent.addCategory(Intent.CATEGORY_BROWSABLE)
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.d("", e.toString())
        }
    } // You'll need to create a DebouncedOnClickListener class that extends OnClickListener

    // and implements the debouncing logic.
    private fun sendFeedbackEmail() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf("support@example.com"))
            putExtra(Intent.EXTRA_SUBJECT, "Feedback for ${requireContext().packageName}")
        }
        
        try {
            startActivity(Intent.createChooser(intent, "Send Feedback"))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "No email app found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openPlayStore() {
        val packageName = requireContext().packageName
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
        } catch (e: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
        }
    }

    override fun getScreenName(): String = "SettingFragment"
}