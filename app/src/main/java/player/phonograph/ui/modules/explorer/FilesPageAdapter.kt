/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.explorer

import coil.size.ViewSizeResolver
import player.phonograph.R
import player.phonograph.coil.loadImage
import player.phonograph.databinding.ItemListBinding
import player.phonograph.mechanism.actions.ActionMenuProviders
import player.phonograph.model.file.FileEntity
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.ui.adapter.MultiSelectionController
import player.phonograph.util.observe
import player.phonograph.util.theme.getTintedDrawable
import player.phonograph.util.theme.themeIconColor
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView

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
                    is FileEntity.Folder -> folderDescriptionString(context.resources, item.songCount)
                }

                shortSeparator.visibility = if (position == dataSet.size - 1) View.GONE else View.VISIBLE

                setImage(image, item)

                menu.setOnClickListener {
                    ActionMenuProviders.FileEntityActionMenuProvider.prepareMenu(it, item, position)
                }
            }
            controller.registerClicking(itemView, bindingAdapterPosition) {
                callback(dataSet as List<FileEntity>, position)
                true
            }
            itemView.isActivated = controller.isSelected(item)
        }

        private fun folderDescriptionString(resources: Resources, songCount: Int): String {
            return if (songCount >= 0) {
                resources.getQuantityString(R.plurals.item_songs, songCount, songCount)
            } else {
                resources.getString(R.string.folder)
            }
        }

        private fun setImage(image: ImageView, item: FileEntity) {
            when (item) {
                is FileEntity.File   -> {
                    if (loadCover) {
                        loadImage(image.context)
                            .from(item)
                            .size(ViewSizeResolver(image))
                            .into(
                                onStart = { image.setImageDrawable(iconFile(image.context)) },
                                onSuccess = { image.setImageDrawable(it) },
                            )
                            .enqueue()
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

        private fun color(context: Context): Int =
            themeIconColor(context)
    }

    private var loadCover: Boolean = false

    init {
        observe(
            activity.lifecycle,
            Setting(activity)[Keys.showFileImages].flow,
            state = Lifecycle.State.STARTED
        ) { showFileImages ->
            if (loadCover != showFileImages) {
                loadCover = showFileImages
                @SuppressLint("NotifyDataSetChanged")
                notifyDataSetChanged()
            }
        }
    }
}
