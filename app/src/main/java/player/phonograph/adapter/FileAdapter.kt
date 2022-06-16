/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.adapter

import android.annotation.SuppressLint
import android.graphics.PorterDuff
import android.media.MediaScannerConnection
import android.view.*
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView.SectionedAdapter
import java.io.File
import kotlinx.coroutines.*
import player.phonograph.R
import player.phonograph.adapter.base.MultiSelectAdapter
import player.phonograph.databinding.ItemListBinding
import player.phonograph.helper.menu.SongMenuHelper
import player.phonograph.helper.menu.SongsMenuHelper
import player.phonograph.interfaces.MultiSelectionCabProvider
import player.phonograph.mediastore.MediaStoreUtil
import player.phonograph.misc.UpdateToastMediaScannerCompletionListener
import player.phonograph.model.FileEntity
import player.phonograph.settings.Setting
import player.phonograph.util.BlacklistUtil
import util.mddesign.util.Util

class FileAdapter(
    activity: AppCompatActivity,
    dataset: MutableList<FileEntity>,
    private val callback: (FileEntity) -> Unit,
    cabProvider: MultiSelectionCabProvider?,
) : MultiSelectAdapter<FileAdapter.ViewHolder, FileEntity>(activity, cabProvider), SectionedAdapter {
    var dataSet: MutableList<FileEntity> = dataset
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItem(datasetPosition: Int): FileEntity = dataSet[datasetPosition]

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemListBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dataSet[position], position)
    }

    override fun getItemCount(): Int = dataSet.size

    override fun getSectionName(position: Int): String = dataSet[position].name.take(2)

    override var multiSelectMenuRes: Int = R.menu.menu_item_file_entities
    override fun onMultipleItemAction(menuItem: MenuItem, selection: List<FileEntity>) {
        val songs = selection.mapNotNull { (it as? FileEntity.File)?.linkedSong }
        SongsMenuHelper.handleMenuClick(context as AppCompatActivity, songs, menuItem.itemId)
    }

    inner class ViewHolder(var binding: ItemListBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            item: FileEntity,
            position: Int,
        ) {
            with(binding) {
                title.text = item.name
                text.text = item.location.basePath

                shortSeparator.visibility = if (position == dataSet.size - 1) View.GONE else View.VISIBLE

                val iconColor = Util.resolveColor(context, R.attr.iconColor)
                image.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN)
                image.setImageResource(if (item is FileEntity.File) R.drawable.ic_file_music_white_24dp else R.drawable.ic_folder_white_24dp)
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
                if (isInQuickSelectMode)
                    toggleChecked(bindingAdapterPosition)
                else
                    callback(item)
            }
            itemView.setOnLongClickListener {
                toggleChecked(bindingAdapterPosition)
            }
            itemView.isActivated = isChecked(item)
        }

        private fun onMenuClick(item: MenuItem, fileItem: FileEntity): Boolean {
            context as AppCompatActivity
            return when (fileItem) {
                is FileEntity.File -> {
                    val song = fileItem.linkedSong
                    SongMenuHelper.handleMenuClick(context, song, item.itemId)
                }
                is FileEntity.Folder -> {
                    val path = fileItem.location.absolutePath
                    return when (val itemId = item.itemId) {
                        R.id.action_play_next, R.id.action_add_to_current_playing, R.id.action_add_to_playlist, R.id.action_delete_from_device -> {
                            CoroutineScope(SupervisorJob()).launch(Dispatchers.IO) {
                                val songs = MediaStoreUtil.searchSongFiles(context, fileItem.location)
                                    ?.mapNotNull { if (it is FileEntity.File) it.linkedSong else null } ?: return@launch
                                withContext(Dispatchers.Main) { SongsMenuHelper.handleMenuClick(context, songs, itemId) }
                            }
                            true
                        }
                        R.id.action_set_as_start_directory -> {
                            Setting.instance().startDirectory = File(path)
                            Toast.makeText(context, String.format(context.getString(R.string.new_start_directory), path), Toast.LENGTH_SHORT).show()
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

    override fun updateItemCheckStatusForAll() = notifyDataSetChanged()
    override fun updateItemCheckStatus(datasetPosition: Int) = notifyItemChanged(datasetPosition)
}
