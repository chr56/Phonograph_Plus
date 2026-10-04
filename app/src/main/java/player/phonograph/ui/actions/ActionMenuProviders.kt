/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.ui.actions

import com.github.chr56.android.menu_dsl.attach
import com.github.chr56.android.menu_dsl.menuItem
import com.github.chr56.android.menu_dsl.submenu
import player.phonograph.foundation.concurrent.lifecycleScopeOrNewOne
import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Genre
import player.phonograph.model.Song
import player.phonograph.model.SongCollection
import player.phonograph.model.file.FileItem
import player.phonograph.model.menu.ActionEntry
import player.phonograph.model.playlist.Playlist
import player.phonograph.repo.loader.Genres
import player.phonograph.repo.loader.Songs
import player.phonograph.ui.resource.interpreter
import android.content.Context
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
object ActionMenuProviders {
    interface ActionMenuProvider<I> {

        fun prepareMenu(menuButtonView: View, item: I, bindingPosition: Int = -1) {
            PopupMenu(menuButtonView.context, menuButtonView).apply {
                inflateMenu(menu, menuButtonView.context, item, bindingPosition)
            }.show()
        }

        /**
         * inflate [menu] of this [item]
         */
        fun inflateMenu(menu: Menu, context: Context, item: I, position: Int)
    }

    fun inflateEntries(menu: Menu, context: Context, entries: List<ActionEntry>) {
        context.attach(menu) {
            for (entry in entries) {
                val title = entry.semantic.interpreter().text(context.resources)
                when (entry) {
                    is ActionEntry.ToolbarEntry   -> menuItem(title = title) {
                        when (entry.visibility) {
                            ActionEntry.ToolbarEntry.NEVER   -> showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
                            ActionEntry.ToolbarEntry.ALWAYS  -> showAsActionFlag = MenuItem.SHOW_AS_ACTION_ALWAYS
                            ActionEntry.ToolbarEntry.IF_ROOM -> showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
                        }
                        onClick {
                            context.lifecycleScopeOrNewOne().launch(Dispatchers.Main.immediate) {
                                entry.action()
                            }
                            true
                        }
                    }

                    is ActionEntry.GroupedEntries -> {
                        val subMenu = submenu(title) {}
                        inflateEntries(subMenu, context, entry.entries)
                    }
                }
            }
        }
    }

    class SongActionMenuProvider(
        private val showPlay: Boolean,
        private val index: Int = Int.MIN_VALUE,
    ) : ActionMenuProvider<Song> {
        override fun inflateMenu(menu: Menu, context: Context, song: Song, position: Int) {
            inflateEntries(menu, context, ActionMenus.Song(context, song, showPlay, index))
        }
    }

    sealed class CompositeActionMenuProvider<I> : ActionMenuProvider<I> {
        abstract suspend fun readSongs(context: Context, item: I): List<Song>

        override fun inflateMenu(menu: Menu, context: Context, item: I, position: Int) {
            inflateEntries(menu, context, ActionMenus.Composite(context, item, ::readSongs))
        }
    }

    object AlbumActionMenuProvider : CompositeActionMenuProvider<Album>() {
        override suspend fun readSongs(context: Context, album: Album): List<Song> = withContext(Dispatchers.IO) {
            Songs.album(context, album.id)
        }
    }


    object ArtistActionMenuProvider : CompositeActionMenuProvider<Artist>() {
        override suspend fun readSongs(context: Context, artist: Artist): List<Song> = withContext(Dispatchers.IO) {
            Songs.artist(context, artist.id)
        }
    }

    object GenreActionMenuProvider : CompositeActionMenuProvider<Genre>() {
        override suspend fun readSongs(context: Context, genre: Genre): List<Song> = withContext(Dispatchers.IO) {
            Genres.songs(context, genre.id)
        }
    }

    object SongCollectionActionMenuProvider : CompositeActionMenuProvider<SongCollection>() {
        override suspend fun readSongs(context: Context, collection: SongCollection): List<Song> = collection.songs
    }

    object PlaylistActionMenuProvider : ActionMenuProvider<Playlist> {
        override fun inflateMenu(menu: Menu, context: Context, playlist: Playlist, position: Int) {
            inflateEntries(menu, context, ActionMenus.Playlist(context, playlist))
        }
    }

    object FileItemActionMenuProvider : ActionMenuProvider<FileItem> {
        override fun inflateMenu(menu: Menu, context: Context, file: FileItem, position: Int) {
            inflateEntries(menu, context, ActionMenus.FileItem(context, file))
        }
    }

    object EmptyActionMenuProvider : ActionMenuProvider<Any> {
        override fun inflateMenu(menu: Menu, context: Context, item: Any, position: Int) = menu.clear()
    }
}