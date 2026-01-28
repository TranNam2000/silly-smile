package com.nomyek.myapplication.comon

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.content.res.ResourcesCompat
import com.nomyek.myapplication.R
import com.nomyek.myapplication.databinding.ViewTabSelectorBinding
import com.nomyek.myapplication.utils.click

class TabSelectorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: ViewTabSelectorBinding
    private var onTabSelectedListener: ((Int) -> Unit)? = null

    init {
        // Inflate layout with binding
        binding = ViewTabSelectorBinding.inflate(LayoutInflater.from(context), this, true)
        
        // Set default selected tab
        selectTab(0)
        
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.tabLeft.click {
            selectTab(0)
            onTabSelectedListener?.invoke(0)
        }

        binding.tabRight.click {
            selectTab(1)
            onTabSelectedListener?.invoke(1)
        }
    }

    fun selectTab(index: Int) {
        when (index) {
            0 -> {
                binding.tabLeft.isSelected = true
                binding.tabLeft.setTextAppearance(R.style.TabTextSelected)

                binding.tabRight.isSelected = false
                binding.tabRight.setTextAppearance(R.style.TabTextUnselected)
            }
            1 -> {
                binding.tabRight.isSelected = true
                binding.tabRight.setTextAppearance(R.style.TabTextSelected)

                binding.tabLeft.isSelected = false
                binding.tabLeft.setTextAppearance(R.style.TabTextUnselected)

            }
        }
    }

    fun setOnTabSelectedListener(listener: (Int) -> Unit) {
        onTabSelectedListener = listener
    }
}

