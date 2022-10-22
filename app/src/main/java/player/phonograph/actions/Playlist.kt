/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.actions

import android.content.Context
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.ColorInt
import androidx.fragment.app.FragmentActivity
import com.github.chr56.android.menu_dsl.attach
import com.github.chr56.android.menu_dsl.menuItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import player.phonograph.R
import player.phonograph.dialogs.AddToPlaylistDialog
import player.phonograph.dialogs.ClearPlaylistDialog
import player.phonograph.dialogs.RenamePlaylistDialog
import player.phonograph.misc.SAFCallbackHandlerActivity
import player.phonograph.model.playlist.FilePlaylist
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.playlist.PlaylistType
import player.phonograph.model.playlist.ResettablePlaylist
import player.phonograph.model.playlist.SmartPlaylist
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.service.queue.ShuffleMode
import player.phonograph.util.ImageUtil.getTintedDrawable
import util.phonograph.m3u.PlaylistsManager

fun injectPlaylistDetail(menu: Menu, context: Context, playlist: Playlist, @ColorInt iconColor: Int) = context.run {
    attach(menu) {
        menuItem {
            title = getString(R.string.action_shuffle_playlist)
            icon = getTintedDrawable(R.drawable.ic_shuffle_white_24dp, iconColor)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_ALWAYS
            onClick {
                playlist.shuffleAndPlay(context)
                true
            }
        }
        menuItem {
            title = getString(R.string.action_play)
            icon = getTintedDrawable(R.drawable.ic_play_arrow_white_24dp, iconColor)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_ALWAYS
            onClick { playlist.play(context) }
        }

        menuItem {
            title = getString(R.string.refresh)
            icon = getTintedDrawable(R.drawable.ic_refresh_white_24dp, iconColor)
            itemId = R.id.action_refresh
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
        }

        menuItem {
            title = getString(R.string.action_play_next)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
            onClick { playlist.playNext(context) }
        }

        menuItem {
            title = getString(R.string.action_add_to_playing_queue)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
            onClick { playlist.addToCurrentQueue(context) }
        }
        menuItem {
            title = getString(R.string.action_add_to_playlist)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
            onClick {
                val activity = context as? FragmentActivity
                if (activity != null) {
                    playlist.addToPlaylist(activity)
                    true
                } else {
                    false
                }
            }
        }

        // File Playlist
        if (playlist !is SmartPlaylist) {
            menuItem {
                title = getString(R.string.edit)
                itemId = R.id.action_edit_playlist
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
            }
            menuItem {
                title = getString(R.string.rename_action)
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
                onClick {
                    val activity = context as? FragmentActivity
                    if (activity != null) {
                        playlist.renamePlaylist(activity)
                        true
                    } else {
                        false
                    }
                }
            }
        }

        // Resettable
        if (playlist is ResettablePlaylist) {
            menuItem {
                title = getString(
                    if (playlist is FilePlaylist) R.string.delete_action else R.string.clear_action
                )
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
                onClick {
                    val activity = context as? FragmentActivity
                    if (activity != null) {
                        playlist.deletePlaylist(activity)
                        true
                    } else {
                        false
                    }
                }
            }
        }

        menuItem {
            title = getString(R.string.save_playlist_title)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
            onClick {
                val activity = context as? FragmentActivity
                if (activity != null) {
                    playlist.savePlaylist(activity)
                    true
                } else {
                    false
                }
            }
        }

        // shortcut
        if (playlist.type == PlaylistType.LAST_ADDED) {
            menuItem {
                itemId = R.id.action_setting_last_added_interval
                title = getString(R.string.pref_title_last_added_interval)
                icon = getTintedDrawable(R.drawable.ic_timer_white_24dp, iconColor)
            }
        }
    }
}

fun injectPlaylistAdapter(menu: Menu, context: Context, playlist: Playlist) = context.run {
    attach(menu) {
        menuItem {
            title = getString(R.string.action_play)
            onClick { playlist.play(context) }
        }
        menuItem {
            title = getString(R.string.action_play_next)
            onClick { playlist.playNext(context) }
        }
        menuItem {
            title = getString(R.string.action_add_to_playing_queue)
            onClick { playlist.addToCurrentQueue(context) }
        }
        menuItem {
            title = getString(R.string.add_playlist_title)
            onClick {
                val activity = context as? FragmentActivity
                if (activity != null) {
                    playlist.addToPlaylist(activity)
                    true
                } else {
                    false
                }
            }
        }
        if (playlist is FilePlaylist) {
            menuItem {
                title = getString(R.string.rename_action)
                onClick {
                    val activity = context as? FragmentActivity
                    if (activity != null) {
                        playlist.renamePlaylist(activity)
                        true
                    } else {
                        false
                    }
                }
            }
        }
        if (playlist is ResettablePlaylist) {
            menuItem {
                title =
                    if (playlist is FilePlaylist) getString(R.string.delete_action)
                    else getString(R.string.clear_action)
                onClick {
                    val activity = context as? FragmentActivity
                    if (activity != null) {
                        playlist.deletePlaylist(activity)
                        true
                    } else {
                        false
                    }
                }
            }
        }
        menuItem {
            title = getString(R.string.save_playlist_title)
            onClick {
                val activity = context as? FragmentActivity
                if (activity != null) {
                    playlist.savePlaylist(activity)
                    true
                } else {
                    false
                }
            }
        }
    }
}

@JvmName("applyToToolbarPlaylist")
fun applyToPlaylistsToolbar(
    menu: Menu,
    context: Context,
    playlists: List<Playlist>,
    @ColorInt iconColor: Int,
    selectAllCallback: (() -> Boolean)?,
): Boolean = with(context) {
    attach(menu) {
        val songs = playlists.flatMap { it.getSongs(context) }
        menuItem(getString(R.string.action_play)) {
            icon = getTintedDrawable(R.drawable.ic_play_arrow_white_24dp, iconColor)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
            onClick {
                MusicPlayerRemote.playNow(songs)
                true
            }
        }
        menuItem(getString(R.string.action_play_next)) {
            icon = getTintedDrawable(R.drawable.ic_redo_white_24dp, iconColor)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
            onClick {
                MusicPlayerRemote.playNext(songs)
                true
            }
        }
        menuItem(getString(R.string.action_add_to_playing_queue)) {
            icon = getTintedDrawable(R.drawable.ic_library_add_white_24dp, iconColor)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
            onClick {
                MusicPlayerRemote.enqueue(songs)
                true
            }
        }
        menuItem(getString(R.string.action_add_to_playlist)) {
            icon = getTintedDrawable(R.drawable.ic_playlist_add_white_24dp, iconColor)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
            onClick {
                actionAddToPlaylist(songs)
            }
        }
        menuItem(getString(R.string.action_delete_from_device)) {
            icon = getTintedDrawable(R.drawable.ic_delete_white_24dp, iconColor)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
            onClick {
                playlists.deletePlaylists(context)
            }
        }
        menuItem(getString(R.string.save_playlists_title)) {
            icon = getTintedDrawable(R.drawable.ic_save_white_24dp, iconColor)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
            onClick {
                playlists.savePlaylists(context)
                true
            }
        }
        selectAllCallback?.let { callback ->
            menuItem(getString(R.string.select_all_title)) {
                icon = getTintedDrawable(R.drawable.ic_select_all_white_24dp, iconColor)
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
                onClick {
                    callback()
                }
            }
        }
    }
    true
}

fun Playlist.play(context: Context): Boolean =
    playQueue(context, getSongs(context))

fun Playlist.shuffleAndPlay(context: Context) =
    MusicPlayerRemote.playQueueCautiously(getSongs(context), 0, true, ShuffleMode.SHUFFLE)

fun Playlist.playNext(context: Context): Boolean =
    MusicPlayerRemote.playNext(ArrayList(getSongs(context)))

fun Playlist.addToCurrentQueue(context: Context): Boolean =
    MusicPlayerRemote.enqueue(ArrayList(getSongs(context)))

fun Playlist.addToPlaylist(activity: FragmentActivity) {
    AddToPlaylistDialog.create(getSongs(activity))
        .show(activity.supportFragmentManager, "ADD_PLAYLIST")
}

fun Playlist.renamePlaylist(activity: FragmentActivity) {
    RenamePlaylistDialog.create(this.id)
        .show(activity.supportFragmentManager, "RENAME_PLAYLIST")
}

fun Playlist.deletePlaylist(activity: FragmentActivity) {
    ClearPlaylistDialog.create(listOf(this))
        .show(activity.supportFragmentManager, "CLEAR_PLAYLIST")
}

fun List<Playlist>.deletePlaylists(activity: Context): Boolean =
    if (activity is FragmentActivity) {
        ClearPlaylistDialog.create(this)
            .show(activity.supportFragmentManager, "CLEAR_PLAYLIST")
        true
    } else {
        false
    }

fun Playlist.savePlaylist(activity: FragmentActivity) {
    CoroutineScope(Dispatchers.Default).launch {
        PlaylistsManager(activity, activity as? SAFCallbackHandlerActivity)
            .duplicatePlaylistViaSaf(this@savePlaylist)
    }
}

fun List<Playlist>.savePlaylists(activity: Context) {
    CoroutineScope(Dispatchers.Default).launch {
        PlaylistsManager(activity, activity as? SAFCallbackHandlerActivity)
            .duplicatePlaylistsViaSaf(this@savePlaylists)
    }
}
