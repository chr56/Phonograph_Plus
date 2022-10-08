/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.actions

import android.app.Activity
import android.content.Context
import android.media.MediaScannerConnection
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.github.chr56.android.menu_dsl.attach
import com.github.chr56.android.menu_dsl.menuItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import player.phonograph.R
import player.phonograph.mediastore.searchSongs
import player.phonograph.mediastore.MediaStoreUtil.linkedSong
import player.phonograph.misc.UpdateToastMediaScannerCompletionListener
import player.phonograph.model.Song
import player.phonograph.model.file.FileEntity
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.util.BlacklistUtil
import player.phonograph.util.preferences.FileConfig
import java.io.File

fun applyToPopupMenu(
    context: Context,
    menu: Menu,
    file: FileEntity,
) = context.run {
    attach(menu) {
        menuItem(title = getString(R.string.action_play)) { // id = R.id.action_play
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
            onClick {
                action(context, file, MusicPlayerRemote::playNow)
            }
        }
        menuItem(title = getString(R.string.action_play_next)) { // id = R.id.action_play_next
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
            onClick {
                action(context, file, MusicPlayerRemote::playNext)
            }
        }
        menuItem(title = getString(R.string.action_add_to_playing_queue)) { // id = R.id.action_add_to_current_playing
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
            onClick {
                action(context, file, MusicPlayerRemote::enqueue)
            }
        }
        menuItem(title = getString(R.string.action_add_to_playlist)) { // id = R.id.action_add_to_playlist
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
            onClick {
                action(context, file, ::actionAddToPlaylist)
            }
        }
        when (file) {
            is FileEntity.File -> {
                menuItem(title = getString(R.string.action_details)) { // id = R.id.action_details
                    showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
                    onClick {
                        fragmentActivity(context) { gotoDetail(it, file.linkedSong(context)) }
                        true
                    }
                }
                menuItem(title = getString(R.string.action_share)) { // id = R.id.action_share
                    showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
                    onClick { share(context, file.linkedSong(context)) }
                }
            }
            is FileEntity.Folder -> {
                menuItem(title = getString(R.string.action_scan)) {
                    showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
                    onClick {
                        scan(context, file)
                        true
                    }
                }
                menuItem(title = getString(R.string.action_set_as_start_directory)) {
                    showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
                    onClick {
                        setStartDirectory(context, file)
                    }
                }
                menuItem(title = getString(R.string.action_add_to_black_list)) { // id = R.id.action_add_to_black_list
                    showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
                    onClick {
                        BlacklistUtil.addToBlacklist(context, File(file.location.absolutePath))
                        true
                    }
                }

            }
        }
        menuItem(title = getString(R.string.action_delete_from_device)) { // id = R.id.action_delete_from_device
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
            onClick {
                action(context, file, ::actionDelete)
            }
        }
    }
}

private inline fun action(context: Context, fileItem: FileEntity, block: (List<Song>) -> Boolean): Boolean =
    when (fileItem) {
        is FileEntity.File -> block(listOf(fileItem.linkedSong(context)))
        is FileEntity.Folder -> block(
            searchSongs(context, fileItem.location)
        )
    }

private fun scan(context: Context, dir: FileEntity.Folder) =
    CoroutineScope(SupervisorJob()).launch(Dispatchers.IO) {
        val files = File(dir.location.absolutePath).listFiles() ?: return@launch
        val paths: Array<String> = Array(files.size) { files[it].path }

        withContext(Dispatchers.Main) {
            MediaScannerConnection.scanFile(
                context,
                paths,
                arrayOf("audio/*"),
                if (context is Activity)
                    UpdateToastMediaScannerCompletionListener(context, paths)
                else null
            )
        }
    }

private fun setStartDirectory(context: Context, dir: FileEntity.Folder): Boolean {
    val path = dir.location.absolutePath
    FileConfig.startDirectory = File(path)
    Toast.makeText(
        context,
        String.format(context.getString(R.string.new_start_directory), path),
        Toast.LENGTH_SHORT
    ).show()
    return true
}