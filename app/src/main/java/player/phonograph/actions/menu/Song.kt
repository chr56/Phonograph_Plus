/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.actions.menu

import com.github.chr56.android.menu_dsl.attach
import com.github.chr56.android.menu_dsl.menuItem
import com.github.chr56.android.menu_dsl.submenu
import player.phonograph.R
import player.phonograph.actions.*
import player.phonograph.model.Song
import player.phonograph.ui.compose.tag.TagEditorActivity
import android.content.Context
import android.view.Menu
import android.view.MenuItem
import android.view.View

fun songPopupMenu(
    context: Context,
    menu: Menu,
    song: Song,
    showPlay: Boolean,
    index: Int,
    transitionView: View?,
) = context.run {
    attach(menu) {
        if (showPlay) menuItem(title = getString(R.string.action_play)) { // id = R.id.action_play_
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
            onClick { song.actionPlay() } // todo
        }
        menuItem(title = getString(R.string.action_play_next)) { // id = R.id.action_play_next
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
            onClick { song.actionPlayNext() }
        }
        if (index >= 0) {
            menuItem(title = getString(R.string.action_remove_from_playing_queue)) {
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
                onClick { actionRemoveFromQueue(index) }
            }
        } else {
            menuItem(title = getString(R.string.action_add_to_playing_queue)) { // id = R.id.action_add_to_current_playing
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
                onClick { song.actionEnqueue() }
            }
        }
        menuItem(title = getString(R.string.action_add_to_playlist)) { // id = R.id.action_add_to_playlist
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
            onClick { listOf(song).actionAddToPlaylist(context) }
        }
        menuItem(title = getString(R.string.action_go_to_album)) { // id = R.id.action_go_to_album
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
            onClick { song.actionGotoAlbum(context, transitionView) }
        }
        menuItem(title = getString(R.string.action_go_to_artist)) { // id = R.id.action_go_to_artist
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
            onClick { song.actionGotoArtist(context, transitionView) }
        }
        menuItem(title = getString(R.string.action_details)) { // id = R.id.action_details
            showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
            onClick {
                fragmentActivity(context) { song.actionGotoDetail(it) }
            }
        }
        submenu(context.getString(R.string.more_actions)) {
            menuItem(title = getString(R.string.action_share)) { // id = R.id.action_share
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
                onClick { song.actionShare(context) }
            }
            menuItem(title = getString(R.string.action_tag_editor)) { // id = R.id.action_tag_editor
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
                onClick { song.actionTagEditor(context) }
            }
            menuItem(title = getString(R.string.action_tag_editor)) { // id = R.id.action_tag_editor
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
                onClick {
                    TagEditorActivity.launch(context, song.id)
                    true
                }
            }
            menuItem(title = getString(R.string.action_set_as_ringtone)) { // id = R.id.action_set_as_ringtone
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
                onClick { song.actionSetAsRingtone(context) }
            }
            menuItem(title = getString(R.string.action_add_to_black_list)) { // id = R.id.action_add_to_black_list
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
                onClick { song.actionAddToBlacklist(context) }
            }
            menuItem(title = getString(R.string.action_delete_from_device)) { // id = R.id.action_delete_from_device
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
                onClick { listOf(song).actionDelete(context) }
            }
        }
    }
}

