/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.components.explorer

import android.graphics.PorterDuff
import android.media.MediaScannerConnection
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ComponentActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mt.util.color.resolveColor
import player.phonograph.R
import player.phonograph.adapter.base.MultiSelectionCabController
import player.phonograph.coil.loadImage
import player.phonograph.databinding.ItemListBinding
import player.phonograph.mediastore.MediaStoreUtil
import player.phonograph.mediastore.MediaStoreUtil.linkedSong
import lib.phonograph.misc.UpdateToastMediaScannerCompletionListener
import player.phonograph.model.file.FileEntity
import player.phonograph.util.preferences.FileConfig
import player.phonograph.settings.Setting
import player.phonograph.util.BlacklistUtil
import player.phonograph.util.ImageUtil.getTintedDrawable
import player.phonograph.util.menu.onMultiSongMenuItemClick
import player.phonograph.util.menu.onSongMenuItemClick
import java.io.File

class FilesPageAdapter(
    activity: ComponentActivity,
    dataset: MutableList<FileEntity>,
    private val callback: (FileEntity) -> Unit,
    cabController: MultiSelectionCabController?,
) : AbsFilesAdapter<FilesPageAdapter.ViewHolder>(activity, dataset, cabController) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemListBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    inner class ViewHolder(binding: ItemListBinding) : AbsFilesAdapter.ViewHolder(binding) {
        override fun bind(
            item: FileEntity,
            position: Int,
        ) {
            with(binding) {
                title.text = item.name
                text.text = when (item) {
                    is FileEntity.File -> Formatter.formatFileSize(context, item.size)
                    is FileEntity.Folder -> context.resources.getQuantityString(
                        R.plurals.x_songs,
                        item.songCount,
                        item.songCount
                    )
                }

                shortSeparator.visibility = if (position == dataSet.size - 1) View.GONE else View.VISIBLE

                setImage(image, item)
            }
            binding.menu.setOnClickListener {
                PopupMenu(context, binding.menu)
                    .apply {
                        inflate(
                            if (item is FileEntity.File) R.menu.menu_item_file_entity else R.menu.menu_item_directory_entity
                        )
                        setOnMenuItemClickListener { onMenuClick(it, item) }
                        show()
                    }
            }

            itemView.setOnClickListener {
                if (isInQuickSelectMode) {
                    toggleChecked(bindingAdapterPosition)
                } else {
                    callback(item)
                }
            }
            itemView.setOnLongClickListener {
                toggleChecked(bindingAdapterPosition)
            }
            itemView.isActivated = isChecked(item)
        }

        private fun setImage(image: ImageView, item: FileEntity) {
            if (item is FileEntity.File) {
                if (loadCover) {
                    loadImage(image.context) {
                        data(item.linkedSong(context))
                        size(image.maxWidth, image.maxHeight)
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

        private fun onMenuClick(item: MenuItem, fileItem: FileEntity): Boolean {
            context as AppCompatActivity
            return when (fileItem) {
                is FileEntity.File -> {
                    val song = fileItem.linkedSong(context)
                    onSongMenuItemClick(context, song, item.itemId)
                }
                is FileEntity.Folder -> {
                    val path = fileItem.location.absolutePath
                    return when (val itemId = item.itemId) {
                        R.id.action_play_next, R.id.action_add_to_current_playing, R.id.action_add_to_playlist, R.id.action_delete_from_device -> {
                            CoroutineScope(SupervisorJob()).launch(Dispatchers.IO) {
                                val songs =
                                    MediaStoreUtil.searchSongFiles(context, fileItem.location)
                                        ?.mapNotNull { if (it is FileEntity.File) it.linkedSong(context) else null } ?: return@launch
                                withContext(Dispatchers.Main) {
                                    onMultiSongMenuItemClick(context, songs, itemId)
                                }
                            }
                            true
                        }
                        R.id.action_set_as_start_directory -> {
                            FileConfig.startDirectory = File(path)
                            Toast.makeText(
                                context,
                                String.format(context.getString(R.string.new_start_directory), path),
                                Toast.LENGTH_SHORT
                            ).show()
                            true
                        }
                        R.id.action_scan -> {
                            CoroutineScope(SupervisorJob()).launch(Dispatchers.IO) {
                                val files = File(path).listFiles() ?: return@launch
                                val paths: Array<String> = Array(files.size) { files[it].path }

                                withContext(Dispatchers.Main) {
                                    MediaScannerConnection.scanFile(
                                        context,
                                        paths,
                                        arrayOf("audio/*"),
                                        UpdateToastMediaScannerCompletionListener(context, paths)
                                    )
                                }
                            }
                            true
                        }
                        R.id.action_add_to_black_list -> {
                            BlacklistUtil.addToBlacklist(context, File(path))
                            true
                        }
                        else -> false
                    }
                }
            }
        }
    }

    override var multiSelectMenuRes: Int = R.menu.menu_item_file_entities
    override fun onMultipleItemAction(menuItem: MenuItem, selection: List<FileEntity>) {
        val songs = selection.mapNotNull { (it as? FileEntity.File)?.linkedSong(context) }
        onMultiSongMenuItemClick(context as AppCompatActivity, songs, menuItem.itemId)
    }

    var loadCover: Boolean = Setting.instance.showFileImages

}
