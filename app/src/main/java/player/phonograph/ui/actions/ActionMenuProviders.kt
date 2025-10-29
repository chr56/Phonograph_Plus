/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.actions

import com.github.chr56.android.menu_dsl.attach
import com.github.chr56.android.menu_dsl.menuItem
import com.github.chr56.android.menu_dsl.submenu
import player.phonograph.R
import player.phonograph.mechanism.event.EventHub
import player.phonograph.mechanism.scanner.MediaStoreScanner
import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Genre
import player.phonograph.model.Song
import player.phonograph.model.SongCollection
import player.phonograph.model.file.FileItem
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.service.ShuffleMode
import player.phonograph.repo.loader.PinedPlaylists
import player.phonograph.repo.loader.Songs
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import player.phonograph.ui.modules.tag.TagBrowserActivity
import player.phonograph.util.concurrent.lifecycleScopeOrNewOne
import player.phonograph.util.fragmentActivity
import androidx.lifecycle.lifecycleScope
import android.content.Context
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File

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

    class SongActionMenuProvider(
        private val showPlay: Boolean,
        private val index: Int = Int.MIN_VALUE,
        private val transitionView: View? = null,
    ) : ActionMenuProvider<Song> {
        override fun inflateMenu(menu: Menu, context: Context, song: Song, position: Int) = context.run {
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
                        onClick { MusicPlayerRemote.queueManager.removeSongAt(index); true }
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
                submenu(context.getString(R.string.action_more)) {
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

    sealed class CompositeActionMenuProvider<I> : ActionMenuProvider<I> {
        abstract suspend fun readSongs(context: Context, item: I): List<Song>
        override fun inflateMenu(menu: Menu, context: Context, item: I, position: Int) = context.run {
            attach(menu) {
                menuItem {
                    title = getString(R.string.action_play)
                    onClick {
                        lifecycleScopeOrNewOne().launch {
                            val songs = readSongs(context, item)
                            songs.actionPlay(ShuffleMode.NONE, 0)
                        }
                        true
                    }
                }
                menuItem {
                    title = getString(R.string.action_play_next)
                    onClick {
                        lifecycleScopeOrNewOne().launch {
                            val songs = readSongs(context, item)
                            songs.actionPlayNext()
                        }
                        true
                    }
                }
                menuItem {
                    title = getString(R.string.action_add_to_playing_queue)
                    onClick {
                        lifecycleScopeOrNewOne().launch {
                            val songs = readSongs(context, item)
                            songs.actionEnqueue()
                        }
                        true
                    }
                }
                menuItem {
                    title = getString(R.string.action_add_to_playlist)
                    onClick {
                        fragmentActivity(context) {
                            lifecycleScopeOrNewOne().launch {
                                val songs = readSongs(context, item)
                                songs.actionAddToPlaylist(it)
                            }
                            true
                        }
                    }
                }
                submenu(context.getString(R.string.action_more)) {
                    menuItem(title = getString(R.string.action_delete_from_device)) {
                        onClick {
                            fragmentActivity(context) {
                                lifecycleScopeOrNewOne().launch {
                                    val songs = readSongs(context, item)
                                    songs.actionDelete(it)
                                }
                                true
                            }
                        }
                    }
                }
            }
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
            Songs.genres(context, genre.id)
        }
    }

    object SongCollectionActionMenuProvider : CompositeActionMenuProvider<SongCollection>() {
        override suspend fun readSongs(context: Context, collection: SongCollection): List<Song> = collection.songs
    }

    object PlaylistActionMenuProvider : ActionMenuProvider<Playlist> {
        override fun inflateMenu(menu: Menu, context: Context, playlist: Playlist, position: Int) = context.run {
            attach(menu) {
                menuItem {
                    title = getString(R.string.action_play)
                    onClick {
                        context.lifecycleScopeOrNewOne().launch {
                            playlist.actionPlay(context)
                        }
                        true
                    }
                }
                menuItem {
                    title = getString(R.string.action_play_next)
                    onClick {
                        context.lifecycleScopeOrNewOne().launch {
                            playlist.actionPlayNext(context)
                        }
                        true
                    }
                }
                menuItem {
                    title = getString(R.string.action_add_to_playing_queue)
                    onClick {
                        context.lifecycleScopeOrNewOne().launch {
                            playlist.actionAddToCurrentQueue(context)
                        }
                        true
                    }
                }
                menuItem {
                    title = getString(R.string.action_add_to_playlist)
                    onClick {
                        fragmentActivity(context) {
                            it.lifecycleScope.launch {
                                playlist.actionAddToPlaylist(it)
                            }
                            true
                        }
                    }
                }
                if (!playlist.isVirtual()) {
                    menuItem {
                        title = getString(R.string.action_rename)
                        onClick {
                            fragmentActivity(context) {
                                playlist.actionRenamePlaylist(it)
                                true
                            }
                        }
                    }
                    menuItem {
                        val pined = runBlocking { PinedPlaylists.isPined(context, playlist) }
                        title = getString(if (!pined) R.string.action_pin else R.string.action_unpin)
                        showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
                        onClick {
                            context.lifecycleScopeOrNewOne().launch(Dispatchers.IO) {
                                PinedPlaylists.toggleState(context, playlist)
                                EventHub.sendEvent(context, EventHub.EVENT_PLAYLISTS_CHANGED)
                            }
                            true
                        }
                    }
                }
                menuItem {
                    title = if (!playlist.isVirtual()) getString(R.string.action_delete)
                    else getString(R.string.action_clear)
                    onClick {
                        fragmentActivity(context) {
                            playlist.actionDeletePlaylist(it)
                            true
                        }
                    }
                }
                menuItem {
                    title = getString(R.string.action_save_playlist)
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

    object FileItemActionMenuProvider : ActionMenuProvider<FileItem> {
        override fun inflateMenu(menu: Menu, context: Context, file: FileItem, position: Int) = context.run {
            attach(menu) {
                menuItem(title = getString(R.string.action_play)) { // id = R.id.action_play
                    showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
                    onClick {
                        action(context, file, MusicPlayerRemote::playNow)
                    }
                }
                menuItem(title = getString(R.string.action_play_next)) { // id = R.id.action_play_next
                    showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
                    onClick {
                        action(context, file, MusicPlayerRemote::playNext)
                    }
                }
                menuItem(title = getString(R.string.action_add_to_playing_queue)) { // id = R.id.action_add_to_current_playing
                    showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
                    onClick {
                        action(context, file, MusicPlayerRemote::enqueue)
                    }
                }
                menuItem(title = getString(R.string.action_add_to_playlist)) { // id = R.id.action_add_to_playlist
                    showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
                    onClick {
                        action(context, file) { it.actionAddToPlaylist(context) } //todo
                    }
                }
                if (file.isFile) {
                    menuItem(title = getString(R.string.action_details)) { // id = R.id.action_details
                        showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
                        onClick {
                            fragmentActivity(context) {
                                lifecycleScopeOrNewOne().launch {
                                    filter(file, context)?.actionGotoDetail(it)
                                }
                                true
                            }
                        }
                    }
                    menuItem(title = getString(R.string.action_share)) { // id = R.id.action_share
                        showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
                        onClick {
                            lifecycleScopeOrNewOne().launch {
                                filter(file, context)?.actionShare(context)
                            }
                            true
                        }
                    }
                    menuItem(title = getString(R.string.action_tag_editor)) { //id = R.id.action_tag_editor
                        showAsActionFlag = MenuItem.SHOW_AS_ACTION_IF_ROOM
                        onClick {
                            TagBrowserActivity.launch(context, file.path)
                            true
                        }
                    }
                } else {
                    menuItem(title = getString(R.string.action_scan)) {
                        showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
                        onClick {
                            scan(context, file)
                            true
                        }
                    }
                    menuItem(title = getString(R.string.action_set_as_start_directory)) {
                        showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
                        onClick {
                            setStartDirectory(context, file)
                        }
                    }
                    menuItem(title = getString(R.string.action_add_to_black_list)) { // id = R.id.action_add_to_black_list
                        showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
                        onClick {
                            val file = File(file.path)
                            if (file.isDirectory) addToBlacklist(context, file.absolutePath)
                            else addToBlacklist(context, file.absolutePath.dropLastWhile { it != '/' }.dropLast(1))
                            true
                        }
                    }
                }
                menuItem(title = getString(R.string.action_delete_from_device)) { // id = R.id.action_delete_from_device
                    showAsActionFlag = MenuItem.SHOW_AS_ACTION_NEVER
                    onClick {
                        action(context, file) { it.actionDelete(context) } //todo
                    }
                }
            }
        }

        private suspend fun filter(file: FileItem, context: Context): Song? =
            if (file.content is FileItem.SongContent) {
                file.content.song
            } else if (file.mediaPath.mediastoreId > 0) {
                Songs.id(context, file.mediaPath.mediastoreId)
            } else {
                Songs.searchByPath(context, file.path, true).firstOrNull()
            }

        private inline fun action(
            context: Context,
            fileItem: FileItem,
            crossinline block: (List<Song>) -> Boolean,
        ): Boolean = runBlocking {
            block(fileItem.songs(context))
        }

        private fun scan(context: Context, dir: FileItem) {
            context.lifecycleScopeOrNewOne().launch(Dispatchers.IO) {
                val files = File(dir.path).listFiles() ?: return@launch
                val paths: Array<String> = Array(files.size) { files[it].path }

                MediaStoreScanner(context).scan(paths)
            }
        }

        private fun setStartDirectory(context: Context, dir: FileItem): Boolean {
            val path = dir.path
            Setting(context)[Keys.startDirectoryPath].data = path
            Toast.makeText(
                context, String.format(context.getString(R.string.msg_new_start_directory), path), Toast.LENGTH_SHORT
            ).show()
            return true
        }

    }

    object EmptyActionMenuProvider : ActionMenuProvider<Any> {
        override fun inflateMenu(menu: Menu, context: Context, item: Any, position: Int) = menu.clear()
    }
}