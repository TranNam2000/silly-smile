package com.nomyek.myapplication.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.jrm.base.BaseFragment
import com.nomyek.myapplication.R
import com.nomyek.myapplication.comon.BottomNavigationBar
import com.nomyek.myapplication.databinding.FragmentMainBinding
import com.nomyek.myapplication.ui.edit.editimage.EditImageFragment
import com.nomyek.myapplication.ui.favorite.FavoriteFragment
import com.nomyek.myapplication.ui.history.HistoryFragment
import com.nomyek.myapplication.ui.home.HomeFragment
import com.nomyek.myapplication.ui.settings.SettingFragment
import com.nomyek.myapplication.utils.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainFragment : BaseFragment<FragmentMainBinding>(FragmentMainBinding::inflate) {

    companion object {
        fun newInstance() = MainFragment()
    }

    private var currentFragment: Fragment? = null
    private val homeFragment: HomeFragment by lazy { HomeFragment() }
    private val favoriteFragment: FavoriteFragment by lazy { FavoriteFragment() }
    private val historyFragment: HistoryFragment by lazy { HistoryFragment() }
    private val settingFragment: SettingFragment by lazy { SettingFragment() }

    private val viewModel: MainViewModel by viewModels()

    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            openImagePicker()
        } else {
            showPermissionDeniedDialog()
        }
    }

    // Image picker launcher
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            handleSelectedImage(it)
        }
    }


    override fun initView() {
        setupBottomNavigation()
        loadFragment(homeFragment)
        observeViewModel()
    }

    override fun addEvent() {

    }

    private fun setupBottomNavigation() {
        val bottomNavBar = binding?.bottomNavBar
        bottomNavBar?.setOnNavItemSelectedListener { navId ->
            when (navId) {
                BottomNavigationBar.NAV_HOME -> {
                    loadFragment(homeFragment)
                }

                BottomNavigationBar.NAV_FAVORITE -> {
                    loadFragment(favoriteFragment)
                }

                BottomNavigationBar.NAV_ADD -> {
                    requestMediaPermissions()
                }

                BottomNavigationBar.NAV_HISTORY -> {
                    loadFragment(historyFragment)
                }

                BottomNavigationBar.NAV_SETTING -> {
                    loadFragment(settingFragment)
                }
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        if (currentFragment == fragment) {
            return
        }

        val fragmentManager = childFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()

        currentFragment?.let {
            fragmentTransaction.hide(it)
        }
        if (fragment.isAdded) {
            fragmentTransaction.show(fragment)
        } else {
            fragmentTransaction.add(R.id.fragment_container, fragment)
        }

        fragmentTransaction.commit()
        currentFragment = fragment
    }


    private fun requestMediaPermissions() {
        // Check if permissions are already granted
        if (hasMediaPermissions()) {
            openImagePicker()
            return
        }

        if (shouldShowRequestPermissionRationale()) {
            showPermissionRationaleDialog()
        } else {

            launchPermissionRequest()
        }
    }

    private fun hasMediaPermissions(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                        ) == PackageManager.PERMISSION_GRANTED
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED
            }

            else -> {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
    }

    private fun shouldShowRequestPermissionRationale(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                shouldShowRequestPermissionRationale(Manifest.permission.READ_MEDIA_IMAGES) ||
                        shouldShowRequestPermissionRationale(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                shouldShowRequestPermissionRationale(Manifest.permission.READ_MEDIA_IMAGES)
            }

            else -> {
                shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    private fun showPermissionRationaleDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.permission_required))
            .setMessage(getString(R.string.permission_message))
            .setPositiveButton(getString(R.string.apply)) { _, _ ->
                launchPermissionRequest()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun launchPermissionRequest() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                requestPermissions.launch(
                    arrayOf(
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                    )
                )
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                requestPermissions.launch(
                    arrayOf(
                        Manifest.permission.READ_MEDIA_IMAGES
                    )
                )
            }

            else -> {
                requestPermissions.launch(
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                )
            }
        }
    }

    private fun openImagePicker() {
        imagePickerLauncher.launch("image/*")
    }

    private fun showPermissionDeniedDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.permission_required))
            .setMessage(getString(R.string.permission_message))
            .setPositiveButton(getString(R.string.settings)) { _, _ ->
                openAppSettings()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun openAppSettings() {
        val intent =
            android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", requireContext().packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    private fun observeViewModel() {


        lifecycleScope.launch {
            viewModel.imageProcessResult.collect { result ->
                result?.let {
                    if (it.isSuccess) {
                        val cachedFile = it.getOrNull()

                        cachedFile?.let { file ->
                            navigateToEditImage(file.absolutePath)
                        } ?: run {
                            showToast(getString(R.string.err_path))
                        }
                    } else {
                        val error = it.exceptionOrNull()
                        context?.let { ctx ->
                            showToast(
                                getString(
                                    R.string.error_processing_image,
                                    error?.message ?: "Unknown"
                                )
                            )
                        }
                    }
                    viewModel.clearImageProcessResult()
                }
            }
        }
    }

    private fun handleSelectedImage(uri: Uri) {
        android.util.Log.d("MainFragment", "Image selected: $uri")
        viewModel.processSelectedImage(uri)
    }

    private fun navigateToEditImage(imagePath: String) {
        val editFragment = EditImageFragment.newInstanceFromUri(
            imagePath
        )
        (activity as? MainActivity)?.navigate(
            fragment = editFragment,
            addToBackStack = true
        )
    }


}
