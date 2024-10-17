/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.mechanism.playlist

import org.koin.core.context.GlobalContext
import player.phonograph.R
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

    suspend fun create(context: Context, songs: List<Song>, uri: Uri): Boolean =
        writePlaylist(context, uri, songs)

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

            is DatabasePlaylistLocation -> false

            is VirtualPlaylistLocation -> when (location.type) {
                PLAYLIST_TYPE_HISTORY      -> HistoryStore.get().clear()
                PLAYLIST_TYPE_FAVORITE     -> GlobalContext.get().get<IFavorite>().clearAll(context)
                PLAYLIST_TYPE_MY_TOP_TRACK -> GlobalContext.get().get<SongPlayCountStore>().clear()
                else                       -> false
            }
        }
    }

}