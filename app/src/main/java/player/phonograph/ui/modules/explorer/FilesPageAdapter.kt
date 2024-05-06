/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.explorer

import coil.size.ViewSizeResolver
import player.phonograph.R
import player.phonograph.actions.menu.fileEntityPopupMenu
import player.phonograph.coil.loadImage
import player.phonograph.databinding.ItemListBinding
import player.phonograph.model.file.FileEntity
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.ui.adapter.MultiSelectionController
import player.phonograph.util.theme.getTintedDrawable
import util.theme.internal.resolveColor
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FilesPageAdapter(
    activity: ComponentActivity,
    dataset: Collection<FileEntity>,
    private val callback: (List<FileEntity>, Int) -> Unit,
) : AbsFilesAdapter<FilesPageAdapter.ViewHolder>(activity, dataset, allowMultiSelection = true) {

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
                    is FileEntity.File   -> Formatter.formatFileSize(context, item.size)
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
            controller.registerClicking(itemView, bindingAdapterPosition) {
                callback(dataSet as List<FileEntity>, position)
                true
            }
            itemView.isActivated = controller.isSelected(item)
        }

        private fun setImage(image: ImageView, item: FileEntity) {
            when (item) {
                is FileEntity.File   -> {
                    if (loadCover) {
                        loadImage(image.context) {
                            data(item)
                            size(ViewSizeResolver(image))
                            target(
                                onStart = { image.setImageDrawable(iconFile(image.context)) },
                                onSuccess = { image.setImageDrawable(it) }
                            )
                        }
                    } else {
                        image.setImageDrawable(iconFile(image.context))
                    }
                }

                is FileEntity.Folder -> {
                    image.setImageDrawable(iconFolder(image.context))
                }
            }
        }

        private fun iconFile(context: Context): Drawable? =
            context.getTintedDrawable(R.drawable.ic_file_music_white_24dp, color(context))

        private fun iconFolder(context: Context): Drawable? =
            context.getTintedDrawable(R.drawable.ic_folder_white_24dp, color(context))

        private fun color(context: Context):Int =
            context.resolveColor(R.attr.iconColor, context.getColor(R.color.icon_lightdark))
    }

    private var loadCover: Boolean = false

    init {
        activity.lifecycleScope.launch(Dispatchers.IO) {
            Setting(activity)[Keys.showFileImages].flow.collect {
                @SuppressLint("NotifyDataSetChanged")
                if (loadCover != it) {
                    loadCover = it
                    launch(Dispatchers.Main) {
                        notifyDataSetChanged()
                    }
                }
            }
        }
    }
}
