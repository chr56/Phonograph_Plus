/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.actions.menu

import com.github.chr56.android.menu_dsl.attach
import com.github.chr56.android.menu_dsl.menuItem
import player.phonograph.R
import player.phonograph.actions.*
import player.phonograph.model.playlist.FilePlaylist
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.playlist.PlaylistType
import player.phonograph.model.playlist.ResettablePlaylist
import player.phonograph.model.playlist.SmartPlaylist
import player.phonograph.util.ImageUtil.getTintedDrawable
import androidx.annotation.ColorInt
import androidx.fragment.app.FragmentActivity
import android.content.Context
import android.view.Menu
import android.view.MenuItem

fun playlistToolbar(menu: Menu, context: Context, playlist: Playlist, @ColorInt iconColor: Int) =
    context.run {
        attach(menu) {
            menuItem {
                title = getString(R.string.action_shuffle_playlist)
                icon = getTintedDrawable(R.drawable.ic_shuffle_white_24dp, iconColor)
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_ALWAYS
                onClick { playlist.actionShuffleAndPlay(context) }
            }
            menuItem {
                title = getString(R.string.action_play)
                icon = getTintedDrawable(R.drawable.ic_play_arrow_white_24dp, iconColor)
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_ALWAYS
                onClick { playlist.actionPlay(context) }
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
                onClick { playlist.actionPlayNext(context) }
            }

            menuItem {
                title = getString(R.string.action_add_to_playing_queue)
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
                onClick { playlist.actionAddToCurrentQueue(context) }
            }
            menuItem {
                title = getString(R.string.action_add_to_playlist)
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
                onClick {
                    fragmentActivity(context) {
                        playlist.actionAddToPlaylist(it)
                        true
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
                        fragmentActivity(context) {
                            playlist.actionRenamePlaylist(it)
                            true
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
                        fragmentActivity(context) {
                            playlist.actionDeletePlaylist(it)
                            true
                        }
                    }
                }
            }

            menuItem {
                title = getString(R.string.save_playlist_title)
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
                onClick {
                    fragmentActivity(context) {
                        playlist.actionSavePlaylist(it)
                        true
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
        }
        if (playlist is ResettablePlaylist) {
            menuItem {
                title =
                    if (playlist is FilePlaylist) getString(R.string.delete_action)
                    else getString(R.string.clear_action)
                onClick {
                    fragmentActivity(context){
                        playlist.actionDeletePlaylist(it)
                        true
                    }
                }
            }
        }
        menuItem {
            title = getString(R.string.save_playlist_title)
            onClick {
                fragmentActivity(context){
                    playlist.actionSavePlaylist(it)
                    true
                }
            }
        }
    }
}

