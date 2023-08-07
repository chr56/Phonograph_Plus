/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.adapter

import player.phonograph.databinding.ItemGridBinding
import player.phonograph.databinding.ItemGridCardHorizontalBinding
import player.phonograph.databinding.ItemListBinding
import player.phonograph.databinding.ItemListNoImageBinding
import player.phonograph.databinding.ItemListSingleRowBinding
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView

abstract class BindingViewHolder<VB : ViewBinding>
protected constructor(val binding: VB) : RecyclerView.ViewHolder(binding.root) {
    protected val context: Context = itemView.context

    protected open var image: ImageView? = null
    protected open var imageText: TextView? = null
    protected open var title: TextView? = null
    protected open var text: TextView? = null
    protected open var menu: View? = null
    protected open var separator: View? = null
    protected open var shortSeparator: View? = null
    protected open var dragView: View? = null
    protected open var paletteColorContainer: View? = null
}

class ListViewHolder(context: Context) : BindingViewHolder<ItemListBinding>(
    ItemListBinding.inflate(LayoutInflater.from(context))
) {
    init {
        image = binding.image
        imageText = binding.imageText
        title = binding.title
        text = binding.text
        menu = binding.menu
        separator = binding.separator
        shortSeparator = binding.shortSeparator
        dragView = binding.dragView
        // paletteColorContainer = binding.paletteColorContainer
    }
}

class SingleRowListViewHolder(context: Context) : BindingViewHolder<ItemListSingleRowBinding>(
    ItemListSingleRowBinding.inflate(LayoutInflater.from(context))
) {
    init {
        image = binding.image
        imageText = binding.imageText
        title = binding.title
        // text = binding.text
        menu = binding.menu
        separator = binding.separator
        shortSeparator = binding.shortSeparator
        dragView = binding.dragView
        // paletteColorContainer = binding.paletteColorContainer
    }
}

class NoImageListViewHolder(context: Context) : BindingViewHolder<ItemListNoImageBinding>(
    ItemListNoImageBinding.inflate(LayoutInflater.from(context))
) {
    init {
        // image = binding.image
        // imageText = binding.imageText
        title = binding.title
        text = binding.text
        menu = binding.menu
        separator = binding.separator
        shortSeparator = binding.shortSeparator
        dragView = binding.dragView
        // paletteColorContainer = binding.paletteColorContainer
    }
}

class GridViewHolder(context: Context) : BindingViewHolder<ItemGridBinding>(
    ItemGridBinding.inflate(LayoutInflater.from(context))
) {
    init {
        image = binding.image
        // imageText = binding.imageText
        title = binding.title
        text = binding.text
        // menu = binding.menu
        // separator = binding.separator
        // shortSeparator = binding.shortSeparator
        // dragView = binding.dragView
        paletteColorContainer = binding.paletteColorContainer
    }
}

class GridCardHorizontalViewHolder(context: Context) : BindingViewHolder<ItemGridCardHorizontalBinding>(
    ItemGridCardHorizontalBinding.inflate(LayoutInflater.from(context))
) {
    init {
        image = binding.image
        // imageText = binding.imageText
        title = binding.title
        text = binding.text
        // menu = binding.menu
        // separator = binding.separator
        // shortSeparator = binding.shortSeparator
        // dragView = binding.dragView
        // paletteColorContainer = binding.paletteColorContainer
    }

}
