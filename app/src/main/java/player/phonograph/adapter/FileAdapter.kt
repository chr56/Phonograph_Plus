/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView.SectionedAdapter
import player.phonograph.R
import player.phonograph.adapter.base.AbsMultiSelectAdapter
import player.phonograph.databinding.ItemListBinding
import player.phonograph.interfaces.CabHolder
import player.phonograph.model.FileEntity
import util.mddesign.util.Util

class FileAdapter(
    private val context: Context,
    dataset: MutableList<FileEntity>,
    private val callback: (FileEntity) -> Unit,
    cabHolder: CabHolder?,
) : AbsMultiSelectAdapter<FileAdapter.ViewHolder, FileEntity>(context, cabHolder, R.menu.menu_media_selection), SectionedAdapter {
    var dataSet: MutableList<FileEntity> = dataset
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getIdentifier(position: Int): FileEntity = dataSet[position]

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemListBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dataSet[position], position)
    }

    override fun getItemCount(): Int = dataSet.size

    override fun getSectionName(position: Int): String = dataSet[position].name.take(2)

    override fun onMultipleItemAction(menuItem: MenuItem, selection: List<FileEntity>) {
        // TODO
    }

    inner class ViewHolder(var binding: ItemListBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            item: FileEntity,
            position: Int,
        ) {
            with(binding) {
                title.text = item.name
                text.text = item.path.basePath

                shortSeparator.visibility = if (position == dataSet.size - 1) View.GONE else View.VISIBLE

                val iconColor = Util.resolveColor(context, R.attr.iconColor)
                image.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN)
                image.setImageResource(if (item.isFolder) R.drawable.ic_folder_white_24dp else R.drawable.ic_file_music_white_24dp)
            }
            itemView.setOnClickListener {
                if (isInQuickSelectMode)
                    toggleChecked(bindingAdapterPosition)
                else
                    callback(item)
            }
            itemView.setOnLongClickListener {
                toggleChecked(bindingAdapterPosition)
            }
            // todo menu
            binding.menu
        }
    }
}
