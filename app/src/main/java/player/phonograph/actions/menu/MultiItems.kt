/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.actions.menu

import android.content.Context
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.ColorInt
import com.github.chr56.android.menu_dsl.attach
import com.github.chr56.android.menu_dsl.menuItem
import player.phonograph.R
import player.phonograph.actions.actionAddToPlaylist
import player.phonograph.actions.actionDelete
import player.phonograph.actions.convertToSongs
import player.phonograph.actions.playQueue
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.util.ImageUtil.getTintedDrawable

fun multiItemsToolbar(
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
                    playQueue(context, convertToSongs(selections, context))
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
                    actionAddToPlaylist(convertToSongs(selections, context))
                }
            }
            menuItem(getString(R.string.action_delete_from_device)) {
                icon = getTintedDrawable(R.drawable.ic_delete_white_24dp, iconColor)
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
                onClick {
                    actionDelete(convertToSongs(selections, context))
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
