/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.components.explorer

import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import androidx.core.app.ComponentActivity
import mt.util.color.primaryTextColor
import mt.util.color.resolveColor
import player.phonograph.R
import player.phonograph.adapter.base.MultiSelectionCabController
import player.phonograph.databinding.ItemListBinding
import player.phonograph.model.file.FileEntity
import player.phonograph.util.ImageUtil.getTintedDrawable
import player.phonograph.util.PhonographColorUtil.nightMode
import java.text.SimpleDateFormat
import java.util.*

class FilesChooserAdapter(
    activity: ComponentActivity,
    dataset: MutableList<FileEntity>,
    private val callback: (FileEntity) -> Unit,
    cabController: MultiSelectionCabController?,
) : AbsFilesAdapter<AbsFilesAdapter.ViewHolder>(activity, dataset, cabController) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemListBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    inner class ViewHolder(binding: ItemListBinding) : AbsFilesAdapter.ViewHolder(binding) {
        override fun bind(item: FileEntity, position: Int) {
            with(binding) {
                title.text = item.name
                text.text = when (item) {
                    is FileEntity.File -> Formatter.formatFileSize(context, item.size)
                    is FileEntity.Folder -> toDate(item.dateModified)
                }
                shortSeparator.visibility = if (position == dataSet.size - 1) View.GONE else View.VISIBLE
                image.setImageDrawable(
                    image.context.getTintedDrawable(
                        if (item is FileEntity.File) R.drawable.ic_file_music_white_24dp
                        else R.drawable.ic_folder_white_24dp,
                        resolveColor(
                            context, R.attr.iconColor,
                            context.primaryTextColor(context.resources.nightMode)
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

    private val formatter = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault())
    private fun toDate(timeInMill: Long): String {
        return formatter.format(
            Calendar.getInstance().also {
                it.timeInMillis = timeInMill
            }.time
        )
    }
}
