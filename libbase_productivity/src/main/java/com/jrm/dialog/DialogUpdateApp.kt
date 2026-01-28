package com.jrm.dialog

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import com.jrm.databinding.LayoutDialogUpdateAppBinding
import com.jrm.utils.remote_config.RemoteConfigManager

class DialogUpdateApp(private val context: Context, private val isRequired: Boolean = false) : Dialog(context) {
    private lateinit var binding: LayoutDialogUpdateAppBinding
    
    companion object {
        fun checkAndShowUpdateDialog(context: Context) {
            val remoteConfig = RemoteConfigManager.instance
            remoteConfig?.let {
                when (it.upgradePopup) {
                    "show_require" -> {
                        // Show required update dialog - user must update
                        val dialog = DialogUpdateApp(context, isRequired = true)
                        dialog.show()
                    }
                    "show_optional" -> {
                        // Show optional update dialog - user can skip
                        val dialog = DialogUpdateApp(context, isRequired = false)
                        dialog.show()
                    }
                    "hide" -> {
                        // Don't show dialog
                    }
                }
            }
        }
    }
    
    init {
        setupDialog()
    }
    
    private fun setupDialog() {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setCancelable(!isRequired)
        
        binding = LayoutDialogUpdateAppBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)
        
        setupViews()
    }
    
    private fun setupViews() {
        if (isRequired) {
            // Required update - show only update button
            binding.buttonsLayout.visibility = View.GONE
            binding.btnUpdateSingle.visibility = View.VISIBLE
            
            binding.btnUpdateSingle.setOnClickListener {
                openPlayStore()
            }
        } else {
            // Optional update - show both buttons
            binding.buttonsLayout.visibility = View.VISIBLE
            binding.btnUpdateSingle.visibility = View.GONE
            
            binding.btnLater.setOnClickListener {
                dismiss()
            }
            
            binding.btnUpdate.setOnClickListener {
                openPlayStore()
                dismiss()
            }
        }
    }
    
    private fun openPlayStore() {
        try {
            val packageName = context.packageName
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            // If Play Store app is not available, open in browser
            val packageName = context.packageName
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}

