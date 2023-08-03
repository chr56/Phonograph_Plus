/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.fragments.explorer

import coil.size.ViewSizeResolver
import mt.util.color.resolveColor
import player.phonograph.R
import player.phonograph.actions.menu.fileEntityPopupMenu
import player.phonograph.adapter.base.MultiSelectionController
import player.phonograph.coil.loadImage
import player.phonograph.databinding.ItemListBinding
import player.phonograph.model.file.FileEntity
import player.phonograph.model.file.linkedSong
import player.phonograph.settings.Setting
import player.phonograph.util.theme.getTintedDrawable
import androidx.activity.ComponentActivity
import android.graphics.PorterDuff
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu

class FilesPageAdapter(
    activity: ComponentActivity,
    dataset: MutableList<FileEntity>,
    private val callback: (List<FileEntity>, Int) -> Unit,
) : AbsFilesAdapter<FilesPageAdapter.ViewHolder>(activity, dataset) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemListBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    inner class ViewHolder(binding: ItemListBinding) : AbsFilesAdapter.ViewHolder(binding) {
        override fun bind(
            item: FileEntity,
            position: Int,
            controller: MultiSelectionController<FileEntity>,
        ) {
            val context = binding.root.context
            with(binding) {
                title.text = item.name
                text.text = when (item) {
                    is FileEntity.File -> Formatter.formatFileSize(context, item.size)
                    is FileEntity.Folder -> context.resources.getQuantityString(
                        R.plurals.item_songs, item.songCount, item.songCount
                    )
                }

                shortSeparator.visibility = if (position == dataSet.size - 1) View.GONE else View.VISIBLE

                setImage(image, item)

                menu.setOnClickListener {
                    PopupMenu(context, binding.menu).apply {
                        fileEntityPopupMenu(context, this.menu, item)
                    }.show()
                }
            }

            itemView.setOnClickListener {
                if (controller.isInQuickSelectMode) {
                    controller.toggle(bindingAdapterPosition)
                } else {
                    callback(dataSet as List<FileEntity>, position)
                }
            }
            itemView.setOnLongClickListener {
                controller.toggle(bindingAdapterPosition)
            }
            itemView.isActivated = controller.isSelected(item)
        }

        private fun setImage(image: ImageView, item: FileEntity) {
            val context = image.context
            if (item is FileEntity.File) {
                if (loadCover) {
                    loadImage(image.context) {
                        data(item.linkedSong(context))
                        size(ViewSizeResolver(image))
                        target(
                            onStart = {
                                image.setImageDrawable(
                                    image.context.getTintedDrawable(
                                        R.drawable.ic_file_music_white_24dp,
                                        resolveColor(context, R.attr.iconColor)
                                    )
                                )
                            },
                            onSuccess = { image.setImageDrawable(it) }
                        )
                    }
                } else {
                    image.setImageDrawable(
                        image.context.getTintedDrawable(
                            R.drawable.ic_file_music_white_24dp,
                            resolveColor(context, R.attr.iconColor)
                        )
                    )
                }
            } else {
                image.setImageResource(
                    R.drawable.ic_folder_white_24dp
                )
                val iconColor = resolveColor(context, R.attr.iconColor)
                image.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN)
            }
        }
    }

    override val allowMultiSelection: Boolean get() = true

    var loadCover: Boolean = Setting.instance.showFileImages

}
