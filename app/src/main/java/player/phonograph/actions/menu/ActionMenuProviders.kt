/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.actions.menu

import com.github.chr56.android.menu_dsl.attach
import com.github.chr56.android.menu_dsl.menuItem
import com.github.chr56.android.menu_dsl.submenu
import player.phonograph.R
import player.phonograph.actions.*
import player.phonograph.model.Song
import player.phonograph.model.playlist.FilePlaylistLocation
import player.phonograph.model.playlist.Playlist
import player.phonograph.repo.database.FavoritesStore
import player.phonograph.util.lifecycleScopeOrNewOne
import android.content.Context
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
object ActionMenuProviders {
    interface ActionMenuProvider<I> {

        fun prepareMenu(menuButtonView: View, item: I) {
            PopupMenu(menuButtonView.context, menuButtonView).apply {
                inflateMenu(menu, menuButtonView.context, item)
            }.show()
        }

        /**
         * inflate [menu] of this [item]
         */
        fun inflateMenu(menu: Menu, context: Context, item: I)
    }

    class SongActionMenuProvider(
        private val showPlay: Boolean,
        private val index: Int = Int.MIN_VALUE,
        private val transitionView: View? = null,
    ) : ActionMenuProvider<Song> {
        override fun inflateMenu(menu: Menu, context: Context, song: Song) = context.run {
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
    }

    object PlaylistActionMenuProvider : ActionMenuProvider<Playlist> {
        override fun inflateMenu(menu: Menu, context: Context, playlist: Playlist) = context.run {
            attach(menu) {
                val location = playlist.location
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
                if (!playlist.isVirtual()) {
                    menuItem {
                        title = getString(R.string.rename_action)
                        onClick {
                            fragmentActivity(context) {
                                playlist.actionRenamePlaylist(it)
                                true
                            }
                        }
                    }
                    if (location is FilePlaylistLocation) menuItem {
                        val pined = FavoritesStore.get().containsPlaylist(playlist.id, location.path)
                        title =
                            getString(if (!pined) R.string.action_pin else R.string.action_unpin)
                        showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
                        onClick {
                            context.lifecycleScopeOrNewOne().launch(Dispatchers.IO) {
                                val ins = FavoritesStore.get()
                                if (pined) ins.removePlaylist(playlist)
                                else ins.addPlaylist(playlist)
                            }
                            true
                        }
                    }
                }
                menuItem {
                    title =
                        if (!playlist.isVirtual()) getString(R.string.delete_action)
                        else getString(R.string.clear_action)
                    onClick {
                        fragmentActivity(context) {
                            playlist.actionDeletePlaylist(it)
                            true
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
    }

    object EmptyActionMenuProvider : ActionMenuProvider<Any> {
        override fun inflateMenu(menu: Menu, context: Context, item: Any) = menu.clear()
    }
}