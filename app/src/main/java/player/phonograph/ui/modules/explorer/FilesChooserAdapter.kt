/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.explorer

import player.phonograph.R
import player.phonograph.databinding.ItemListBinding
import player.phonograph.model.file.FileEntity
import player.phonograph.ui.adapter.MultiSelectionController
import player.phonograph.util.text.dateTimeText
import player.phonograph.util.theme.getTintedDrawable
import player.phonograph.util.theme.nightMode
import util.theme.color.primaryTextColor
import util.theme.internal.resolveColor
import androidx.activity.ComponentActivity
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup

class FilesChooserAdapter(
    activity: ComponentActivity,
    dataset: Collection<FileEntity>,
    private val callback: (FileEntity) -> Unit,
) : AbsFilesAdapter<AbsFilesAdapter.ViewHolder>(activity, dataset, allowMultiSelection = false) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemListBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    inner class ViewHolder(binding: ItemListBinding) : AbsFilesAdapter.ViewHolder(binding) {
        override fun bind(item: FileEntity, position: Int, controller: MultiSelectionController<FileEntity>) {
            val context = binding.root.context
            with(binding) {
                title.text = item.name
                text.text = when (item) {
                    is FileEntity.File   -> Formatter.formatFileSize(context, item.size)
                    is FileEntity.Folder -> dateTimeText(item.dateModified / 1000)
                }
                shortSeparator.visibility = if (position == dataSet.size - 1) GONE else VISIBLE
                image.setImageDrawable(
                    image.context.getTintedDrawable(
                        if (item is FileEntity.File) R.drawable.ic_file_music_white_24dp
                        else R.drawable.ic_folder_white_24dp,
                        context.resolveColor(
                            R.attr.iconColor,
                            context.primaryTextColor(context.nightMode)
                        )
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
