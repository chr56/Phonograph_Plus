/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.mechanism.playlist

import org.koin.core.context.GlobalContext
import player.phonograph.mechanism.IFavorite
import player.phonograph.mechanism.playlist.mediastore.createPlaylistViaMediastore
import player.phonograph.mechanism.playlist.mediastore.deletePlaylistsViaMediastore
import player.phonograph.mechanism.playlist.saf.writePlaylist
import player.phonograph.model.Song
import player.phonograph.model.playlist.DatabasePlaylistLocation
import player.phonograph.model.playlist.FilePlaylistLocation
import player.phonograph.model.playlist.PLAYLIST_TYPE_FAVORITE
import player.phonograph.model.playlist.PLAYLIST_TYPE_HISTORY
import player.phonograph.model.playlist.PLAYLIST_TYPE_MY_TOP_TRACK
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.playlist.VirtualPlaylistLocation
import player.phonograph.repo.database.HistoryStore
import player.phonograph.repo.database.SongPlayCountStore
import player.phonograph.repo.mediastore.MediaStorePlaylists
import player.phonograph.util.file.selectDocumentUris
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract

object PlaylistManager {

    fun create(songs: List<Song>): Creator = Creator(songs)

    class Creator(val songs: List<Song>) {
        suspend fun fromUri(context: Context, uri: Uri): Boolean =
            writePlaylist(context, uri, songs)

        suspend fun fromMediaStore(context: Context, name: String): Long {
            if (MediaStorePlaylists.named(context, name) == null) {
                val id = createPlaylistViaMediastore(context, name, songs)
                return id
            } else {
                return -2L
            }
        }

        suspend fun intoDatabase(context: Context, name: String): Boolean = false
    }

    fun delete(playlist: Playlist, preferSaf: Boolean): Deleter = when (playlist.location) {
        is FilePlaylistLocation     -> Deleter.FilePlaylistDeleter(playlist, preferSaf)
        is DatabasePlaylistLocation -> Deleter.DatabasePlaylistDeleter(playlist, preferSaf)
        is VirtualPlaylistLocation  -> Deleter.VirtualPlaylistDeleter(playlist, preferSaf)
    }

    sealed class Deleter(val playlist: Playlist, protected val preferSaf: Boolean) {
        abstract suspend fun delete(context: Context): Boolean

        class FilePlaylistDeleter(playlist: Playlist, preferSaf: Boolean) : Deleter(playlist, preferSaf) {
            override suspend fun delete(context: Context): Boolean {
                val location = playlist.location
                if (location !is FilePlaylistLocation) return false
                if (!preferSaf) {
                    return deletePlaylistsViaMediastore(context, longArrayOf(location.mediastoreId)).isEmpty()
                } else {
                    val uri = selectDocumentUris(context, listOf(location.path)).firstOrNull() ?: return false
                    return DocumentsContract.deleteDocument(context.contentResolver, uri)
                }
            }
        }

        class VirtualPlaylistDeleter(playlist: Playlist, preferSaf: Boolean) : Deleter(playlist, preferSaf) {
            override suspend fun delete(context: Context): Boolean {
                val location = playlist.location
                if (location !is VirtualPlaylistLocation) return false
                return when (location.type) {
                    PLAYLIST_TYPE_HISTORY      -> HistoryStore.get().clear()
                    PLAYLIST_TYPE_FAVORITE     -> GlobalContext.get().get<IFavorite>().clearAll(context)
                    PLAYLIST_TYPE_MY_TOP_TRACK -> GlobalContext.get().get<SongPlayCountStore>().clear()
                    else                       -> false
                }
            }
        }

        class DatabasePlaylistDeleter(playlist: Playlist, preferSaf: Boolean) : Deleter(playlist, preferSaf) {
            override suspend fun delete(context: Context): Boolean {
                val location = playlist.location
                if (location !is DatabasePlaylistLocation) return false
                return false
            }

        }
    }

}