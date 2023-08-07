/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.adapter

import player.phonograph.databinding.ItemGridBinding
import player.phonograph.databinding.ItemGridCardHorizontalBinding
import player.phonograph.databinding.ItemListBinding
import player.phonograph.databinding.ItemListNoImageBinding
import player.phonograph.databinding.ItemListSingleRowBinding
import player.phonograph.ui.views.IconImageView
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

    protected open val image: ImageView? get() = null
    protected open val imageText: TextView? get() = null
    protected open val title: TextView? get() = null
    protected open val text: TextView? get() = null
    protected open val menu: View? get() = null
    protected open val separator: View? get() = null
    protected open val shortSeparator: View? get() = null
    protected open val dragView: View? get() = null
    protected open val paletteColorContainer: View? get() = null
}

class ListViewHolder(context: Context) : BindingViewHolder<ItemListBinding>(
    ItemListBinding.inflate(LayoutInflater.from(context))
) {
    override val image: ImageView get() = binding.image
    override val imageText: TextView get() = binding.imageText
    override val title: TextView get() = binding.title
    override val text: TextView get() = binding.text
    override val menu: IconImageView get() = binding.menu
    override val separator: View get() = binding.separator
    override val shortSeparator: View get() = binding.shortSeparator
    override val dragView: IconImageView get() = binding.dragView
    // override val paletteColorContainer : View get() = binding.paletteColorContainer
}

class SingleRowListViewHolder(context: Context) : BindingViewHolder<ItemListSingleRowBinding>(
    ItemListSingleRowBinding.inflate(LayoutInflater.from(context))
) {
    override val image: ImageView get() = binding.image
    override val imageText: TextView get() = binding.imageText
    override val title: TextView get() = binding.title
    // override val text: TextView get() = binding.text
    override val menu: IconImageView get() = binding.menu
    override val separator: View get() = binding.separator
    override val shortSeparator: View get() = binding.shortSeparator
    override val dragView: IconImageView get() = binding.dragView
    // override val paletteColorContainer : View get() = binding.paletteColorContainer
}

class NoImageListViewHolder(context: Context) : BindingViewHolder<ItemListNoImageBinding>(
    ItemListNoImageBinding.inflate(LayoutInflater.from(context))
) {
    // override val image: ImageView get() = binding.image
    // override val imageText: TextView get() = binding.imageText
    override val title: TextView get() = binding.title
    override val text: TextView get() = binding.text
    override val menu: IconImageView get() = binding.menu
    override val separator: View get() = binding.separator
    override val shortSeparator: View get() = binding.shortSeparator
    override val dragView: IconImageView get() = binding.dragView
    // override val paletteColorContainer : View get() = binding.paletteColorContainer
}

class GridViewHolder(context: Context) : BindingViewHolder<ItemGridBinding>(
    ItemGridBinding.inflate(LayoutInflater.from(context))
) {
    override val image: ImageView get() = binding.image
    // override val imageText: TextView get() = binding.imageText
    override val title: TextView get() = binding.title
    override val text: TextView get() = binding.text
    // override val menu: IconImageView get() = binding.menu
    // override val separator: View get() = binding.separator
    // override val shortSeparator: View get() = binding.shortSeparator
    // override val dragView: IconImageView get() = binding.dragView
    override val paletteColorContainer: View get() = binding.paletteColorContainer
}

class GridCardHorizontalViewHolder(context: Context) : BindingViewHolder<ItemGridCardHorizontalBinding>(
    ItemGridCardHorizontalBinding.inflate(LayoutInflater.from(context))
) {
    override val image: ImageView get() = binding.image
    // override val imageText: TextView get() = binding.imageText
    override val title: TextView get() = binding.title
    override val text: TextView get() = binding.text
    // override val menu: IconImageView get() = binding.menu
    // override val separator: View get() = binding.separator
    // override val shortSeparator: View get() = binding.shortSeparator
    // override val dragView: IconImageView get() = binding.dragView
    // override val paletteColorContainer : View get() = binding.paletteColorContainer
}
