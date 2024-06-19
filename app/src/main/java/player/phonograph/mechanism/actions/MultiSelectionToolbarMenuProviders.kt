/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.mechanism.actions

import com.github.chr56.android.menu_dsl.attach
import com.github.chr56.android.menu_dsl.menuItem
import player.phonograph.R
import player.phonograph.mechanism.playlist.PlaylistProcessors
import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Genre
import player.phonograph.model.Song
import player.phonograph.model.SongCollection
import player.phonograph.model.file.FileEntity
import player.phonograph.model.playlist.Playlist
import player.phonograph.repo.loader.Songs
import player.phonograph.service.queue.ShuffleMode
import player.phonograph.ui.adapter.MultiSelectionController
import player.phonograph.ui.modules.tag.MultiTagBrowserActivity
import player.phonograph.ui.modules.tag.TagBrowserActivity
import player.phonograph.util.lifecycleScopeOrNewOne
import player.phonograph.util.theme.getTintedDrawable
import android.content.Context
import android.view.Menu
import android.view.MenuItem
import kotlinx.coroutines.launch
import java.util.Random

object MultiSelectionToolbarMenuProviders {
    fun <I> inflate(
        menu: Menu,
        context: Context,
        controller: MultiSelectionController<I>,
    ): Boolean =
        with(context) {
            attach(menu) {
                menuItem(getString(R.string.action_play)) {
                    icon = getTintedDrawable(R.drawable.ic_play_arrow_white_24dp, controller.textColor)
                    showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
                    onClick {
                        context.lifecycleScopeOrNewOne().launch {
                            convertToSongs(controller.selected, context).actionPlay(ShuffleMode.NONE, 0)
                        }
                        true
                    }
                }
                menuItem(getString(R.string.action_play_next)) {
                    icon = getTintedDrawable(R.drawable.ic_redo_white_24dp, controller.textColor)
                    showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
                    onClick {
                        context.lifecycleScopeOrNewOne().launch {
                            convertToSongs(controller.selected, context).actionPlayNext()
                        }
                        true
                    }
                }
                menuItem(title = getString(R.string.action_shuffle_all)) {
                    icon = getTintedDrawable(R.drawable.ic_shuffle_white_24dp, controller.textColor)
                    showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
                    onClick {
                        context.lifecycleScopeOrNewOne().launch {
                            convertToSongs(controller.selected, context)
                                .actionPlay(ShuffleMode.SHUFFLE, Random().nextInt(controller.selected.size))
                        }
                        true
                    }
                }
                menuItem(getString(R.string.action_add_to_playing_queue)) {
                    icon = getTintedDrawable(R.drawable.ic_library_add_white_24dp, controller.textColor)
                    showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
                    onClick {
                        context.lifecycleScopeOrNewOne().launch {
                            convertToSongs(controller.selected, context).actionEnqueue()
                        }
                        true
                    }
                }
                menuItem(getString(R.string.action_add_to_playlist)) {
                    icon = getTintedDrawable(R.drawable.ic_playlist_add_white_24dp, controller.textColor)
                    showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
                    onClick {
                        context.lifecycleScopeOrNewOne().launch {
                            convertToSongs(controller.selected, context).actionAddToPlaylist(context)
                        }
                        true
                    }
                }

                menuItem(title = getString(R.string.action_tag_editor)) {
                    icon = getTintedDrawable(R.drawable.ic_library_music_white_24dp, controller.textColor)
                    showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
                    onClick {
                        context.lifecycleScopeOrNewOne().launch {
                            val songs = convertToSongs(controller.selected, context)
                            if (songs.size > 1)
                                MultiTagBrowserActivity.launch(context, ArrayList(songs.map { it.data }))
                            else
                                TagBrowserActivity.launch(context, songs.first().data)
                        }
                        true
                    }
                }

                val playlists: List<Playlist> = controller.selected.filterIsInstance<Playlist>()

                menuItem(getString(R.string.action_delete_from_device)) {
                    icon = getTintedDrawable(R.drawable.ic_delete_white_24dp, controller.textColor)
                    showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
                    onClick {
                        context.lifecycleScopeOrNewOne().launch {
                            // check playlist to avoid accidentally deleting song but playlist
                            if (playlists.isEmpty()) {
                                convertToSongs(controller.selected, context).actionDelete(context)
                            } else {
                                // todo
                                playlists.actionDeletePlaylists(context)
                            }
                        }
                        true
                    }
                }

                if (playlists.isNotEmpty()) {
                    menuItem(getString(R.string.save_playlists_title)) {
                        icon = getTintedDrawable(R.drawable.ic_save_white_24dp, controller.textColor)
                        showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
                        onClick {
                            playlists.actionSavePlaylists(context)
                            true
                        }
                    }
                }

                menuItem(getString(R.string.select_all_title)) {
                    icon = getTintedDrawable(R.drawable.ic_select_all_white_24dp, controller.textColor)
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
            } // attach
            true
        }

    private suspend fun convertToSongs(selections: Iterable<*>, context: Context): List<Song> = selections.flatMap {
        when (it) {
            is Song            -> listOf(it)
            is Album           -> Songs.album(context, it.id)
            is Artist          -> Songs.artist(context, it.id)
            is Genre           -> Songs.genres(context, it.id)
            is Playlist        -> PlaylistProcessors.reader(it).allSongs(context)
            is SongCollection  -> it.songs
            is FileEntity.File -> listOf(Songs.searchByFileEntity(context, it))
            // is FileEntity.Folder -> TODO()
            else               -> emptyList()
        }
    }
}