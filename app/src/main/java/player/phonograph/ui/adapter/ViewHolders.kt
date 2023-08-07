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

abstract class BindingViewHolder<VB : ViewBinding>
protected constructor(val binding: VB) : RecyclerView.ViewHolder(binding.root) {
    protected val context: Context = itemView.context
}

class ListViewHolder(context: Context) : BindingViewHolder<ItemListBinding>(
    ItemListBinding.inflate(LayoutInflater.from(context))
) {}

class SingleRowListViewHolder(context: Context) : BindingViewHolder<ItemListSingleRowBinding>(
    ItemListSingleRowBinding.inflate(LayoutInflater.from(context))
) {}

class NoImageListViewHolder(context: Context) : BindingViewHolder<ItemListNoImageBinding>(
    ItemListNoImageBinding.inflate(LayoutInflater.from(context))
) {}

class GridViewHolder(context: Context) : BindingViewHolder<ItemGridBinding>(
    ItemGridBinding.inflate(LayoutInflater.from(context))
) {}

class GridCardHorizontalViewHolder(context: Context) : BindingViewHolder<ItemGridCardHorizontalBinding>(
    ItemGridCardHorizontalBinding.inflate(LayoutInflater.from(context))
) {}
