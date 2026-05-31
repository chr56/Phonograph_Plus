/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.ui.modules.playlist

import com.github.chr56.android.menu_dsl.attach
import com.github.chr56.android.menu_dsl.menuItem
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import player.phonograph.R
import player.phonograph.mechanism.playlist.PlaylistSongsActions
import player.phonograph.model.playlist.PLAYLIST_TYPE_FAVORITE
import player.phonograph.model.playlist.PLAYLIST_TYPE_LAST_ADDED
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.playlist.VirtualPlaylistLocation
import player.phonograph.model.ui.UIMode
import player.phonograph.repo.loader.FavoriteTracks
import player.phonograph.ui.actions.actionAddToCurrentQueue
import player.phonograph.ui.actions.actionAddToPlaylist
import player.phonograph.ui.actions.actionDeletePlaylist
import player.phonograph.ui.actions.actionPlay
import player.phonograph.ui.actions.actionPlayNext
import player.phonograph.ui.actions.actionRenamePlaylist
import player.phonograph.ui.actions.actionSavePlaylist
import player.phonograph.ui.actions.actionShuffleAndPlay
import player.phonograph.ui.modules.setting.dialog.LastAddedPlaylistIntervalDialog
import player.phonograph.ui.modules.tag.MultiTagBrowserActivity
import player.phonograph.util.fragmentActivity
import player.phonograph.util.theme.getTintedDrawable
import player.phonograph.util.theme.tintButtons
import androidx.activity.ComponentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import android.view.Menu
import android.view.MenuItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun inflatePlaylistDetailMenu(
    menu: Menu,
    context: ComponentActivity,
    item: Playlist,
    iconColor: Int,
    onAction: (PlaylistAction) -> Boolean,
): Boolean = with(context) {
    attach(menu) {
        menuItem {
            title = getString(R.string.action_play)
            icon = getTintedDrawable(R.drawable.ic_play_arrow_white_24dp, iconColor)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_ALWAYS
            onClick {
                context.lifecycleScope.launch {
                    item.actionPlay(context)
                }
                true
            }
        }
        menuItem {
            title = getString(R.string.action_shuffle_playlist)
            icon = getTintedDrawable(R.drawable.ic_shuffle_white_24dp, iconColor)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_ALWAYS
            onClick {
                context.lifecycleScope.launch {
                    item.actionShuffleAndPlay(context)
                }
                true
            }
        }
        menuItem {
            title = getString(R.string.action_play_next)
            icon = getTintedDrawable(R.drawable.ic_redo_white_24dp, iconColor)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
            onClick {
                context.lifecycleScope.launch {
                    item.actionPlayNext(context)
                }
                true
            }
        }
        menuItem {
            title = getString(R.string.action_add_to_playing_queue)
            icon = getTintedDrawable(R.drawable.ic_library_add_white_24dp, iconColor)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
            onClick {
                context.lifecycleScope.launch {
                    item.actionAddToCurrentQueue(context)
                }
                true
            }
        }
        menuItem {
            title = getString(R.string.action_add_to_playlist)
            icon = getTintedDrawable(R.drawable.ic_playlist_add_white_24dp, iconColor)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
            onClick {
                fragmentActivity(context) {
                    context.lifecycleScope.launch {
                        item.actionAddToPlaylist(it)
                    }
                    true
                }
            }
        }
        if (!item.isVirtual()) {
            menuItem {
                title = getString(R.string.action_rename)
                icon = getTintedDrawable(R.drawable.ic_edit_white_24dp, iconColor)
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
                onClick {
                    fragmentActivity(context) {
                        item.actionRenamePlaylist(it)
                        true
                    }
                }
            }
        }
        menuItem {
            title = getString(
                if (!item.isVirtual()) R.string.action_delete else R.string.action_clear
            )
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
            onClick {
                fragmentActivity(context) {
                    item.actionDeletePlaylist(it)
                    true
                }
            }
        }
        menuItem(title = getString(R.string.action_tag_editor)) { //id = R.id.action_tag_editor
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
            onClick {
                context.lifecycleScope.launch(Dispatchers.IO) {
                    val paths = PlaylistSongsActions.reader(item).allSongs(context).map { it.data }
                    MultiTagBrowserActivity.launch(context, ArrayList(paths))
                }
                true
            }
        }
        menuItem {
            title = getString(R.string.action_save_playlist)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
            onClick {
                fragmentActivity(context) {
                    it.lifecycleScope.launch {
                        item.actionSavePlaylist(it)
                    }
                    true
                }
            }
        }

        menuItem {
            title = getString(R.string.action_search)
            icon = getTintedDrawable(R.drawable.ic_search_white_24dp, iconColor)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
            onClick {
                onAction(UpdateMode(UIMode.Search))
            }
        }
        menuItem {
            title = getString(R.string.action_refresh)
            icon = getTintedDrawable(R.drawable.ic_refresh_white_24dp, iconColor)
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
            onClick {
                onAction(Refresh(fetch = true))
                true
            }
        }

        if (!item.isVirtual()) menuItem {
            title = getString(R.string.action_edit)
            itemId = R.id.action_edit_playlist
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
            onClick {
                onAction(UpdateMode(UIMode.Editor))
                true
            }
        } else {
            val location = item.location
            if (location is VirtualPlaylistLocation) {
                when (location.type) {
                    PLAYLIST_TYPE_LAST_ADDED -> {
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
                                            onAction(Refresh(fetch = true))
                                        }
                                    })
                                    true
                                }
                                true
                            }
                        }
                    }

                    PLAYLIST_TYPE_FAVORITE   -> {
                        menuItem {
                            title = getString(R.string.action_clean)
                            icon = getTintedDrawable(R.drawable.ic_cleaning_bucket_24dp, iconColor)
                            onClick {
                                fragmentActivity(context) { activity ->
                                    MaterialAlertDialogBuilder(context)
                                        .setTitle(R.string.action_clean)
                                        .setMessage(R.string.action_clean_missing_items)
                                        .setPositiveButton(getString(android.R.string.ok)) { dialog, _ ->
                                            context.lifecycleScope.launch {
                                                FavoriteTracks.cleanMissing(context)
                                            }
                                            dialog.dismiss()
                                        }
                                        .setNegativeButton(getString(android.R.string.cancel)) { dialog, _ ->
                                            dialog.dismiss()
                                        }
                                        .create()
                                        .tintButtons()
                                        .show()
                                    true
                                }
                                true
                            }
                        }
                    }


                    else                     -> {}
                }

            }
        }
    }
    true
}