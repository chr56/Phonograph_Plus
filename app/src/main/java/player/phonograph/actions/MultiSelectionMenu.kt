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
import player.phonograph.R
import player.phonograph.dialogs.AddToPlaylistDialog
import player.phonograph.mediastore.GenreLoader
import player.phonograph.mediastore.MediaStoreUtil.linkedSong
import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Genre
import player.phonograph.model.Song
import player.phonograph.model.file.FileEntity
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.ui.dialogs.DeleteSongsDialog
import player.phonograph.util.ImageUtil.getTintedDrawable

fun applyToToolbar(
    menu: Menu,
    context: Context,
    selections: List<Any>,
    @ColorInt iconColor: Int,
    selectAllCallback: (() -> Boolean)?,
): Boolean =
    with(context) {
        attach(menu) {
            menuItem(getString(R.string.action_play)) {
                icon = getTintedDrawable(R.drawable.ic_play_arrow_white_24dp, iconColor)
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
                onClick {
                    MusicPlayerRemote.playNow(convertToSongs(selections, context))
                    true
                }
            }
            menuItem(getString(R.string.action_play_next)) {
                icon = getTintedDrawable(R.drawable.ic_redo_white_24dp, iconColor)
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
                onClick {
                    MusicPlayerRemote.playNext(convertToSongs(selections, context))
                    true
                }
            }
            menuItem(getString(R.string.action_add_to_playing_queue)) {
                icon = getTintedDrawable(R.drawable.ic_library_add_white_24dp, iconColor)
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
                onClick {
                    MusicPlayerRemote.enqueue(convertToSongs(selections, context))
                    true
                }
            }
            menuItem(getString(R.string.action_add_to_playlist)) {
                icon = getTintedDrawable(R.drawable.ic_playlist_add_white_24dp, iconColor)
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
                onClick {
                    actionAddToPlaylist(selections)
                }
            }
            menuItem(getString(R.string.action_delete_from_device)) {
                icon = getTintedDrawable(R.drawable.ic_delete_white_24dp, iconColor)
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
                onClick {
                    actionDelete(selections)
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


private fun convertToSongs(selections: List<Any>, context: Context): List<Song> = selections.flatMap {
    when (it) {
        is Song -> listOf(it)
        is Album -> it.songs
        is Artist -> it.songs
        is Genre -> GenreLoader.getSongs(context, it.id)
        is FileEntity.File -> listOf(it.linkedSong(context))
        // is FileEntity.Folder -> TODO()
        else -> emptyList()
    }
}

private fun Context.actionAddToPlaylist(selections: List<Any>) =
    if ((this is FragmentActivity)) {
        AddToPlaylistDialog.create(convertToSongs(selections, this))
            .show(supportFragmentManager, "ADD_PLAYLIST")
        true
    } else {
        false
    }

private fun Context.actionDelete(selections: List<Any>) =
    if ((this is FragmentActivity)) {
        DeleteSongsDialog.create(ArrayList(convertToSongs(selections, this)))
            .show(supportFragmentManager, "ADD_DELETE")
        true
    } else {
        false
    }
