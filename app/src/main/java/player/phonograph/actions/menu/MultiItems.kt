/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.actions.menu

import com.github.chr56.android.menu_dsl.attach
import com.github.chr56.android.menu_dsl.menuItem
import player.phonograph.R
import player.phonograph.actions.actionAddToPlaylist
import player.phonograph.actions.actionDelete
import player.phonograph.actions.actionDeletePlaylists
import player.phonograph.actions.actionEnqueue
import player.phonograph.actions.actionPlay
import player.phonograph.actions.actionPlayNext
import player.phonograph.actions.actionSavePlaylists
import player.phonograph.actions.convertToSongs
import player.phonograph.model.playlist.Playlist
import player.phonograph.service.queue.ShuffleMode.NONE
import player.phonograph.service.queue.ShuffleMode.SHUFFLE
import player.phonograph.ui.adapter.MultiSelectionController
import player.phonograph.ui.compose.tag.BatchTagEditorActivity
import player.phonograph.ui.compose.tag.TagEditorActivity
import player.phonograph.util.theme.getTintedDrawable
import android.content.Context
import android.graphics.Color
import android.view.Menu
import android.view.MenuItem
import java.util.Random

fun <I> multiItemsToolbar(
    menu: Menu,
    context: Context,
    controller: MultiSelectionController<I>,
): Boolean =
    with(context) {
        val iconColor = Color.WHITE //todo

        attach(menu) {
            menuItem(getString(R.string.action_play)) {
                icon = getTintedDrawable(R.drawable.ic_play_arrow_white_24dp, iconColor)
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
                onClick {
                    convertToSongs(controller.selected, context).actionPlay(NONE, 0)
                }
            }
            menuItem(getString(R.string.action_play_next)) {
                icon = getTintedDrawable(R.drawable.ic_redo_white_24dp, iconColor)
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
                onClick {
                    convertToSongs(controller.selected, context).actionPlayNext()
                }
            }
            menuItem(title = getString(R.string.action_shuffle_all)) {
                icon = getTintedDrawable(R.drawable.ic_shuffle_white_24dp, iconColor)
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
                onClick {
                    convertToSongs(controller.selected, context).actionPlay(SHUFFLE, Random().nextInt(controller.selected.size))
                }
            }
            menuItem(getString(R.string.action_add_to_playing_queue)) {
                icon = getTintedDrawable(R.drawable.ic_library_add_white_24dp, iconColor)
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
                onClick {
                    convertToSongs(controller.selected, context).actionEnqueue()
                    true
                }
            }
            menuItem(getString(R.string.action_add_to_playlist)) {
                icon = getTintedDrawable(R.drawable.ic_playlist_add_white_24dp, iconColor)
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
                onClick {
                    convertToSongs(controller.selected, context).actionAddToPlaylist(context)
                }
            }

            menuItem(title = getString(R.string.action_tag_editor)) { //id = R.id.action_tag_editor
                icon = getTintedDrawable(R.drawable.ic_library_music_white_24dp, iconColor)
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
                onClick {
                    val songs = convertToSongs(controller.selected, context)
                    if (songs.size > 1)
                        BatchTagEditorActivity.launch(context, ArrayList(songs))
                    else
                        TagEditorActivity.launch(context, songs.first().id)
                    true
                }
            }

            val playlists: List<Playlist> = controller.selected.filterIsInstance<Playlist>()

            menuItem(getString(R.string.action_delete_from_device)) {
                icon = getTintedDrawable(R.drawable.ic_delete_white_24dp, iconColor)
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
                onClick {
                    // check playlist to avoid accidentally deleting song but playlist
                    if (playlists.isEmpty()) {
                        convertToSongs(controller.selected, context).actionDelete(context)
                    } else {
                        // todo
                        playlists.actionDeletePlaylists(context)
                    }
                }
            }

            if (playlists.isNotEmpty()) {
                menuItem(getString(R.string.save_playlists_title)) {
                    icon = getTintedDrawable(R.drawable.ic_save_white_24dp, iconColor)
                    showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
                    onClick {
                        playlists.actionSavePlaylists(context)
                        true
                    }
                }
            }

            menuItem(getString(R.string.select_all_title)) {
                icon = getTintedDrawable(R.drawable.ic_select_all_white_24dp, iconColor)
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
                onClick {
                    controller.selectAll()
                    true
                }
            }

            menuItem(getString(R.string.invert_selection)) {
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
                onClick {
                    controller.invertSelected()
                    true
                }
            }

            menuItem(getString(R.string.unselect_all_title)) {
                showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
                onClick {
                    controller.unselectedAll()
                    true
                }
            }

        }
        true
    }
