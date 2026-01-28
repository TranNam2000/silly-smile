package com.nomyek.myapplication.comon

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.nomyek.myapplication.R
import com.nomyek.myapplication.utils.click

class BottomNavigationBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var selectedNavItem: View? = null
    private val selectedColor = ContextCompat.getColor(context, R.color.nav_item_selected)
    private val unselectedColor = ContextCompat.getColor(context, R.color.nav_item_text)

    private var onNavItemSelectedListener: ((Int) -> Unit)? = null

    companion object {
        const val NAV_HOME = 0
        const val NAV_FAVORITE = 1
        const val NAV_ADD = 2
        const val NAV_HISTORY = 3
        const val NAV_SETTING = 4
    }

    init {
        LayoutInflater.from(context).inflate(R.layout.bottom_navigation_bar, this, true)
        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        val navHome = findViewById<View>(R.id.nav_home)
        val navFavorite = findViewById<View>(R.id.nav_favorite)
        val navHistory = findViewById<View>(R.id.nav_history)
        val navSetting = findViewById<View>(R.id.nav_setting)
        val navAdd = findViewById<View>(R.id.nav_add)

        // Set Home as default selected
        setSelectedNavItem(navHome, NAV_HOME)

        navHome.click {
            setSelectedNavItem(navHome, NAV_HOME)
        }

        navFavorite.click {
            setSelectedNavItem(navFavorite, NAV_FAVORITE)
        }

        navHistory.click {
            setSelectedNavItem(navHistory, NAV_HISTORY)
        }

        navSetting.click {
            setSelectedNavItem(navSetting, NAV_SETTING)
        }

        navAdd.click {
            onNavItemSelectedListener?.invoke(NAV_ADD)
        }
    }

    private fun setSelectedNavItem(item: View, navId: Int) {
        // Deselect previous item
        selectedNavItem?.let { previousItem ->
            updateNavItemState(previousItem, false)
        }

        // Select new item
        updateNavItemState(item, true)
        selectedNavItem = item

        // Notify listener
        onNavItemSelectedListener?.invoke(navId)
    }

    private fun updateNavItemState(item: View, isSelected: Boolean) {
        val iconId = when (item.id) {
            R.id.nav_home -> R.id.nav_home_icon
            R.id.nav_favorite -> R.id.nav_favorite_icon
            R.id.nav_history -> R.id.nav_history_icon
            R.id.nav_setting -> R.id.nav_setting_icon
            else -> null
        }

        iconId?.let {
            val icon = item.findViewById<ImageView>(it)
            icon?.setColorFilter(if (isSelected) selectedColor else unselectedColor)
        }

        // Update text color
        val textId = when (item.id) {
            R.id.nav_home -> R.id.nav_home_text
            R.id.nav_favorite -> R.id.nav_favorite_text
            R.id.nav_history -> R.id.nav_history_text
            R.id.nav_setting -> R.id.nav_setting_text
            else -> null
        }

        textId?.let {
            val text = item.findViewById<TextView>(it)
            text?.setTextColor(if (isSelected) selectedColor else unselectedColor)
        }
    }

    fun setOnNavItemSelectedListener(listener: (Int) -> Unit) {
        onNavItemSelectedListener = listener
    }


}