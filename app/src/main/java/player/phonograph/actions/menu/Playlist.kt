/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.actions.menu

import com.github.chr56.android.menu_dsl.attach
import com.github.chr56.android.menu_dsl.menuItem
import player.phonograph.R
import player.phonograph.actions.actionAddToCurrentQueue
import player.phonograph.actions.actionAddToPlaylist
import player.phonograph.actions.actionDeletePlaylist
import player.phonograph.actions.actionPlay
import player.phonograph.actions.actionPlayNext
import player.phonograph.actions.actionRenamePlaylist
import player.phonograph.actions.actionSavePlaylist
import player.phonograph.actions.fragmentActivity
import player.phonograph.model.playlist.FilePlaylist
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.playlist.ResettablePlaylist
import player.phonograph.repo.database.FavoritesStore
import player.phonograph.util.lifecycleScopeOrNewOne
import android.content.Context
import android.view.Menu
import android.view.MenuItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun playlistPopupMenu(menu: Menu, context: Context, playlist: Playlist) = context.run {
    attach(menu) {
        menuItem {
            title = getString(R.string.action_play)
            onClick { playlist.actionPlay(context) }
        }
        menuItem {
            title = getString(R.string.action_play_next)
            onClick { playlist.actionPlayNext(context) }
        }
        menuItem {
            title = getString(R.string.action_add_to_playing_queue)
            onClick { playlist.actionAddToCurrentQueue(context) }
        }
        menuItem {
            title = getString(R.string.add_playlist_title)
            onClick {
                fragmentActivity(context) {
                    playlist.actionAddToPlaylist(it)
                    true
                }
            }
        }
        if (playlist is FilePlaylist) {
            menuItem {
                title = getString(R.string.rename_action)
                onClick {
                    fragmentActivity(context) {
                        playlist.actionRenamePlaylist(it)
                        true
                    }
                }
            }
            menuItem {
                val pined = FavoritesStore.get().containsPlaylist(playlist)
                title =
                    getString(if (!pined) R.string.action_pin else R.string.action_unpin)
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
                onClick {
                    context.lifecycleScopeOrNewOne().launch(Dispatchers.IO) {
                        val ins = FavoritesStore.get()
                        if (pined) ins.removePlaylist(playlist) else ins.addPlaylist(playlist)
                    }
                    true
                }
            }
        }
        if (playlist is ResettablePlaylist) {
            menuItem {
                title =
                    if (playlist is FilePlaylist) getString(R.string.delete_action)
                    else getString(R.string.clear_action)
                onClick {
                    fragmentActivity(context) {
                        playlist.actionDeletePlaylist(it)
                        true
                    }
                }
            }
        }
        menuItem {
            title = getString(R.string.save_playlist_title)
            onClick {
                fragmentActivity(context) {
                    playlist.actionSavePlaylist(it)
                    true
                }
            }
        }
    }
}

