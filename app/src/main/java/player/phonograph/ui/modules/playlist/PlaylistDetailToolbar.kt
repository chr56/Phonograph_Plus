/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.modules.playlist

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
import player.phonograph.actions.actionShuffleAndPlay
import player.phonograph.actions.fragmentActivity
import player.phonograph.model.UIMode
import player.phonograph.model.playlist.FilePlaylist
import player.phonograph.model.playlist.PlaylistType
import player.phonograph.model.playlist.ResettablePlaylist
import player.phonograph.model.playlist.SmartPlaylist
import player.phonograph.ui.dialogs.LastAddedPlaylistIntervalDialog
import player.phonograph.ui.modules.tag.MultiTagBrowserActivity
import player.phonograph.util.theme.getTintedDrawable
import androidx.annotation.ColorInt
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import android.content.Context
import android.view.Menu
import android.view.MenuItem
import kotlinx.coroutines.runBlocking

fun playlistDetailToolbar(
    menu: Menu,
    context: Context,
    model: PlaylistDetailViewModel,
    @ColorInt iconColor: Int,
) =
    context.run {
        val playlist = model.playlist.value
        attach(menu) {
            menuItem {
                title = getString(R.string.action_play)
                icon = getTintedDrawable(R.drawable.ic_play_arrow_white_24dp, iconColor)
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_ALWAYS
                onClick { playlist.actionPlay(context) }
            }
            menuItem {
                title = getString(R.string.action_shuffle_playlist)
                icon = getTintedDrawable(R.drawable.ic_shuffle_white_24dp, iconColor)
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_ALWAYS
                onClick { playlist.actionShuffleAndPlay(context) }
            }
            menuItem {
                title = getString(R.string.action_search)
                icon = getTintedDrawable(R.drawable.ic_search_white_24dp, iconColor)
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
                onClick {
                    if (model.currentMode.value != UIMode.Search) {
                        model.updateCurrentMode(UIMode.Search)
                    } else { // exit
                        model.updateCurrentMode(UIMode.Common)
                    }
                    true
                }
            }
            menuItem {
                title = getString(R.string.action_play_next)
                icon = getTintedDrawable(R.drawable.ic_redo_white_24dp, iconColor)
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
                onClick { playlist.actionPlayNext(context) }
            }

            menuItem {
                title = getString(R.string.refresh)
                icon = getTintedDrawable(R.drawable.ic_refresh_white_24dp, iconColor)
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
                onClick {
                    model.refreshPlaylist(context)
                    true
                }
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
                    onClick {
                        if (playlist is FilePlaylist) {
                            model.updateCurrentMode(UIMode.Editor)
                            true
                        } else {
                            false
                        }
                    }
                }
                menuItem {
                    title = getString(R.string.rename_action)
                    showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
                    onClick {
                        fragmentActivity(context) {
                            (playlist as FilePlaylist).actionRenamePlaylist(it)
                            true
                        }
                    }
                }
            }

            menuItem(title = getString(R.string.action_tag_editor)) { //id = R.id.action_tag_editor
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
                onClick {
                    runBlocking {
                        MultiTagBrowserActivity.launch(context, ArrayList(playlist.getSongs(context).map { it.data }))
                    }
                    true
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
                    onClick {
                        fragmentActivity(context) { activity ->
                            val dialog = LastAddedPlaylistIntervalDialog()
                            dialog.show(activity.supportFragmentManager, "LAST_ADDED")
                            dialog.lifecycle.addObserver(object : DefaultLifecycleObserver {
                                override fun onDestroy(owner: LifecycleOwner) {
                                    model.refreshPlaylist(activity)
                                }
                            })
                            true
                        }
                        true
                    }
                }
            }
        }
    }