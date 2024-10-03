/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.mechanism.playlist

import lib.storage.launcher.ICreateFileStorageAccessible
import org.koin.core.context.GlobalContext
import player.phonograph.R
import player.phonograph.mechanism.IFavorite
import player.phonograph.mechanism.playlist.PlaylistProcessors.reader
import player.phonograph.mechanism.playlist.mediastore.createPlaylistViaMediastore
import player.phonograph.mechanism.playlist.mediastore.deletePlaylistsViaMediastore
import player.phonograph.mechanism.playlist.mediastore.duplicatePlaylistViaMediaStore
import player.phonograph.mechanism.playlist.saf.createPlaylistViaSAF
import player.phonograph.mechanism.playlist.saf.createPlaylistsViaSAF
import player.phonograph.mechanism.playlist.saf.writePlaylist
import player.phonograph.model.Song
import player.phonograph.model.playlist.FilePlaylistLocation
import player.phonograph.model.playlist.PLAYLIST_TYPE_FAVORITE
import player.phonograph.model.playlist.PLAYLIST_TYPE_HISTORY
import player.phonograph.model.playlist.PLAYLIST_TYPE_MY_TOP_TRACK
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.playlist.VirtualPlaylistLocation
import player.phonograph.repo.database.HistoryStore
import player.phonograph.repo.database.SongPlayCountStore
import player.phonograph.repo.mediastore.MediaStorePlaylists
import player.phonograph.util.coroutineToast
import player.phonograph.util.file.selectDocumentUris
import player.phonograph.util.text.currentDate
import player.phonograph.util.text.dateTimeSuffix
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PlaylistManager {

    suspend fun create(context: Context, songs: List<Song>, uri: Uri) {
        writePlaylist(context, uri, songs)
    }

    suspend fun create(context: Context, songs: List<Song>, name: String?): Long {
        @Suppress("NAME_SHADOWING")
        val name: String = if (name.isNullOrEmpty()) context.getString(R.string.new_playlist_title) else name
        if (!MediaStorePlaylists.checkExistence(context, name)) {
            val id = createPlaylistViaMediastore(context, name, songs)
            return id
        } else {
            return -2L
        }
    }

    suspend fun duplicate(context: Context, playlist: Playlist) {
        val name = playlist.name + dateTimeSuffix(currentDate())
        val songs = withContext(Dispatchers.IO) { reader(playlist).allSongs(context) }
        if (shouldUseSAF(context) && context is ICreateFileStorageAccessible) {
            createPlaylistViaSAF(context, playlistName = name, songs = songs)
        } else {
            val id = createPlaylistViaMediastore(context, name, songs)
            coroutineToast(context, if (id != -1L) R.string.success else R.string.failed)
        }
    }

    suspend fun duplicate(context: Context, playlists: List<Playlist>) {
        val names = playlists.map { it.name }
        val songBatches = withContext(Dispatchers.IO) { playlists.map { reader(it).allSongs(context) } }
        if (shouldUseSAF(context) && context is ICreateFileStorageAccessible) {
            createPlaylistsViaSAF(context, songBatches, names, defaultDirectory.absolutePath)
        } else {
            val result = duplicatePlaylistViaMediaStore(context, songBatches, names)
            coroutineToast(context, if (result) R.string.success else R.string.failed)
        }
    }

    suspend fun delete(context: Context, playlist: Playlist, options: Any? = null): Boolean {
        return when (val location = playlist.location) {
            is FilePlaylistLocation    -> {
                if (options == PlaylistProcessors.OPTION_DELETE_WITH_MEDIASTORE) {
                    return deletePlaylistsViaMediastore(context, longArrayOf(location.mediastoreId)).isEmpty()
                } else {
                    val uri = selectDocumentUris(context, listOf(location.path)).firstOrNull() ?: return false
                    return DocumentsContract.deleteDocument(context.contentResolver, uri)
                }
            }

            is VirtualPlaylistLocation -> when (location.type) {
                PLAYLIST_TYPE_HISTORY      -> HistoryStore.get().clear()
                PLAYLIST_TYPE_FAVORITE     -> GlobalContext.get().get<IFavorite>().clearAll(context)
                PLAYLIST_TYPE_MY_TOP_TRACK -> GlobalContext.get().get<SongPlayCountStore>().clear()
                else                       -> false
            }
        }
    }

    private val defaultDirectory get() = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
}