package com.nomyek.myapplication.base

import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView

open class BaseViewHolder<T : Any>(private val binding: ViewDataBinding) : RecyclerView.ViewHolder(binding.root) {

    private var _itemData: T? = null
    val itemData get() = _itemData

    open fun bind(itemData: T?) {
        this._itemData = itemData
    }

    open fun onItemClickListener( callBack : ()-> Unit){
        itemView.setOnClickListener {
            callBack.invoke()
        }
    }

    open fun onLongClickListener( callBack : ()-> Unit){
        itemView.setOnLongClickListener{
            callBack.invoke()
            true
        }
    }

}
