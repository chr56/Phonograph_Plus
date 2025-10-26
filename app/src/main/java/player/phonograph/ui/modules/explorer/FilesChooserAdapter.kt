/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.explorer

import player.phonograph.R
import player.phonograph.databinding.ItemListBinding
import player.phonograph.model.file.FileItem
import player.phonograph.ui.adapter.MultiSelectionController
import player.phonograph.util.text.dateTimeText
import player.phonograph.util.theme.getTintedDrawable
import player.phonograph.util.theme.themeIconColor
import androidx.activity.ComponentActivity
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup

class FilesChooserAdapter(
    activity: ComponentActivity,
    dataset: Collection<FileItem>,
    private val callback: (FileItem) -> Unit,
) : AbsFilesAdapter<AbsFilesAdapter.ViewHolder>(activity, dataset, allowMultiSelection = false) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemListBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    inner class ViewHolder(binding: ItemListBinding) : AbsFilesAdapter.ViewHolder(binding) {
        override fun bind(item: FileItem, position: Int, controller: MultiSelectionController<FileItem>) {
            val context = binding.root.context
            with(binding) {
                title.text = item.name
                text.text = when {
                    item.isFile -> Formatter.formatFileSize(context, item.size)
                    else        -> dateTimeText(item.dateModified / 1000)
                }
                shortSeparator.visibility = if (position == dataSet.size - 1) GONE else VISIBLE
                image.setImageDrawable(
                    image.getTintedDrawable(
                        if (item.isFile) R.drawable.ic_file_music_white_24dp
                        else R.drawable.ic_folder_white_24dp,
                        themeIconColor(context)
                    )
                )
                binding.menu.visibility = GONE
                itemView.setOnClickListener {
                    callback(item)
                }
            }
        }
    }

}
