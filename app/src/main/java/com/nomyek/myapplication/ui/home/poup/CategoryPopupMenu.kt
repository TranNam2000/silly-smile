package com.nomyek.myapplication.ui.home.poup

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import com.nomyek.myapplication.R

data class Category(
    val id: CategoryId,
    val name: String,
)

enum class CategoryId(val resId: Int) {
    ALL(R.string.category_all),
    MARVEL(R.string.category_marvel),
    NATURE(R.string.category_nature),
    CAR(R.string.category_car),
    CARTOON(R.string.category_cartoon),
    CUTE(R.string.category_cute),
    BRAINROT(R.string.category_brainrot),
    ANIME(R.string.category_anime)
}

class CategoryPopupMenu(
    private val context: Context,
    private val selectedCategoryId: CategoryId = CategoryId.ALL,
    private val onCategorySelected: (Category) -> Unit
) {

    private val categories = CategoryId.entries.map { categoryId ->
        Category(
            id = categoryId,
            name = context.getString(categoryId.resId)
        )
    }

    private var popupWindow: PopupWindow? = null

    fun show(anchorView: View) {
        val inflater = LayoutInflater.from(context)
        val popupView = inflater.inflate(R.layout.popup_category_menu, null)

        val categoryContainer = popupView.findViewById<LinearLayout>(R.id.category_container)

        // Add category items
        categories.forEach { category ->
            val itemView = inflater.inflate(R.layout.item_category_popup, categoryContainer, false)
            val nameView = itemView.findViewById<TextView>(R.id.tv_category_name)
            val checkIcon = itemView.findViewById<ImageView>(R.id.iv_category_icon)

            nameView.text = category.name

            if (category.id == selectedCategoryId) {
                checkIcon.visibility = View.VISIBLE
            } else {
                checkIcon.visibility = View.INVISIBLE
            }

            itemView.setOnClickListener {
                onCategorySelected(category)
                dismiss()
            }

            categoryContainer.addView(itemView)
        }

        // Create popup window
        popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            elevation = 10f

            val location = IntArray(2)
            anchorView.getLocationOnScreen(location)

            val yOffset = anchorView.height + 8

            showAsDropDown(anchorView, 0, yOffset, Gravity.END)
        }
    }

    fun dismiss() {
        popupWindow?.dismiss()
        popupWindow = null
    }
}

