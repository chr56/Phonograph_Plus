/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.mechanism.playlist

import player.phonograph.mechanism.playlist.m3u.SAFPlaylistUtil
import player.phonograph.model.Song
import player.phonograph.model.playlist.DatabasePlaylistLocation
import player.phonograph.model.playlist.FilePlaylistLocation
import player.phonograph.model.playlist.PLAYLIST_TYPE_FAVORITE
import player.phonograph.model.playlist.PLAYLIST_TYPE_HISTORY
import player.phonograph.model.playlist.PLAYLIST_TYPE_MY_TOP_TRACK
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.playlist.PlaylistCreator
import player.phonograph.model.playlist.PlaylistCreator.Companion.RESULT_ERROR
import player.phonograph.model.playlist.PlaylistDeleter
import player.phonograph.model.playlist.VirtualPlaylistLocation
import player.phonograph.repo.database.store.HistoryStore
import player.phonograph.repo.database.store.SongPlayCountStore
import player.phonograph.repo.loader.FavoriteSongs
import player.phonograph.repo.mediastore.MediaStorePlaylists
import player.phonograph.repo.mediastore.MediaStorePlaylistsActions
import player.phonograph.repo.room.MusicDatabase
import player.phonograph.repo.room.domain.RoomPlaylistsActions
import player.phonograph.util.file.selectDocumentUris
import player.phonograph.util.mediastoreUriPlaylist
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PlaylistManager {

    fun create(songs: List<Song>): PlaylistCreator = PlaylistCreatorImpl(songs)

    private class PlaylistCreatorImpl(val songs: List<Song>) : PlaylistCreator {

        override suspend fun fromUri(context: Context, uri: Uri): Boolean =
            SAFPlaylistUtil.writePlaylist(context, uri, songs)

        override suspend fun fromMediaStore(context: Context, name: String): Long {
            if (MediaStorePlaylists.named(context, name) == null) {
                val uri = MediaStorePlaylistsActions.create(context, name, songs)
                return if (uri != null) {
                    uri.lastPathSegment?.toLong() ?: RESULT_ERROR
                } else {
                    RESULT_ERROR
                }
            } else {
                return PlaylistCreator.RESULT_EXISTED
            }
        }

        @Suppress("unused")
        override suspend fun intoDatabase(context: Context, name: String): Boolean = withContext(Dispatchers.IO) {
            RoomPlaylistsActions.create(MusicDatabase.koinInstance, name, songs)
        }
    }

    fun delete(playlist: Playlist, preferSaf: Boolean): PlaylistDeleter = when (playlist.location) {
        is FilePlaylistLocation     -> FilePlaylistDeleter(playlist, preferSaf)
        is DatabasePlaylistLocation -> DatabasePlaylistDeleter(playlist, preferSaf)
        is VirtualPlaylistLocation  -> VirtualPlaylistDeleter(playlist, preferSaf)
    }

    class FilePlaylistDeleter(playlist: Playlist, preferSaf: Boolean) : PlaylistDeleter(playlist, preferSaf) {
        override suspend fun delete(context: Context): Boolean {
            val location = playlist.location
            if (location !is FilePlaylistLocation) return false
            if (!preferSaf) {
                val uri = mediastoreUriPlaylist(location.storageVolume, location.mediastoreId)
                return MediaStorePlaylistsActions.delete(context, uri)
            } else {
                val uri = selectDocumentUris(context, listOf(location.path)).firstOrNull() ?: return false
                return DocumentsContract.deleteDocument(context.contentResolver, uri)
            }
        }
    }

    class VirtualPlaylistDeleter(playlist: Playlist, preferSaf: Boolean) : PlaylistDeleter(playlist, preferSaf) {
        override suspend fun delete(context: Context): Boolean {
            val location = playlist.location
            if (location !is VirtualPlaylistLocation) return false
            return when (location.type) {
                PLAYLIST_TYPE_HISTORY      -> HistoryStore.get().clear()
                PLAYLIST_TYPE_MY_TOP_TRACK -> SongPlayCountStore.get().clear()
                PLAYLIST_TYPE_FAVORITE     -> FavoriteSongs.clearAll(context)
                else                       -> false
            }
        }
    }

    class DatabasePlaylistDeleter(playlist: Playlist, preferSaf: Boolean) : PlaylistDeleter(playlist, preferSaf) {
        override suspend fun delete(context: Context): Boolean {
            val location = playlist.location
            if (location !is DatabasePlaylistLocation) return false
            return RoomPlaylistsActions.delete(MusicDatabase.koinInstance, location.databaseId)
        }

    }

    class BatchDeleteSession(playlists: List<Playlist>) {

        var currentPhrase: Int = 1
            private set

        val playlists: MutableList<Playlist> = playlists.toMutableList()

        val failed: MutableList<Playlist> = mutableListOf<Playlist>()

        val filePlaylists: MutableList<Playlist> = mutableListOf<Playlist>()

        val linkedUris: MutableList<Pair<Playlist, Uri?>> = mutableListOf<Pair<Playlist, Uri?>>()

        var useSAF: Boolean = false

        suspend fun execute(context: Context): Int {
            when (currentPhrase) {
                1 -> phase1(context)
                2 -> phase2(context)
                3 -> phase3(context)
                4 -> phase4(context)
            }
            return currentPhrase
        }


        /**
         * ## PHASE 1
         *
         * Delete playlist excepts file playlists:
         *
         * - pick out file playlists
         * - delete others
         * - obtain first batch of playlist failed to be delete
         *
         * then move to **phase 2**
         */
        suspend fun phase1(context: Context) {
            for (playlist in playlists) {
                val result = when (val location = playlist.location) {
                    is FilePlaylistLocation     -> filePlaylists.add(playlist)
                    is DatabasePlaylistLocation -> deleteDatabase(context, location)
                    is VirtualPlaylistLocation  -> deleteVirtual(context, location)
                }
                if (!result) failed.add(playlist)
            }
            currentPhrase = 2
        }

        /**
         * ## PHASE 2
         *
         * Delete file playlists basing on config:
         *
         * - `MediaStore`: delete file playlists and obtain second batch of failures (move to **phase 4**)
         * - `SAF`: Launch DocumentUI and pick a folder, and generate uris for further user action (move to **phase 3**)
         */
        suspend fun phase2(context: Context) {
            if (!useSAF) {
                val results = deleteUsingMediastore(context, filePlaylists)
                failed.addAll(results)
                currentPhrase = 4
            } else {
                val linked = filePlaylists.zip(selectDocumentUris(context, filePlaylists))
                linkedUris.addAll(linked)
                currentPhrase = 3
            }

        }

        /**
         * ## PHASE 3
         *
         * (**`SAF` only**) delete uris from phase 2
         *
         * then move to **phase 4**
         * */
        fun phase3(context: Context) {
            for ((playlist, uri) in linkedUris) {
                if (uri != null) {
                    if (!DocumentsContract.deleteDocument(context.contentResolver, uri)) {
                        failed.add(playlist) // failed to delete
                    }
                } else {
                    failed.add(playlist) // missing uri
                }
            }
            currentPhrase = 4
        }

        /**
         * ## PHASE 4
         *
         * Complete
         *
         * - reset states
         * - reverse to beginning
         */
        fun phase4(context: Context) {

            playlists.clear()
            playlists.addAll(failed)

            filePlaylists.clear()
            linkedUris.clear()

            currentPhrase = 1
        }

        private suspend fun deleteVirtual(context: Context, location: VirtualPlaylistLocation): Boolean =
            when (location.type) {
                PLAYLIST_TYPE_HISTORY      -> HistoryStore.get().clear()
                PLAYLIST_TYPE_MY_TOP_TRACK -> SongPlayCountStore.get().clear()
                PLAYLIST_TYPE_FAVORITE     -> FavoriteSongs.clearAll(context)
                else                       -> false
            }

        private suspend fun deleteDatabase(context: Context, location: DatabasePlaylistLocation): Boolean =
            withContext(Dispatchers.IO) {
                RoomPlaylistsActions.delete(MusicDatabase.koinInstance, location.databaseId)
            }

        private suspend fun deleteUsingMediastore(context: Context, filePlaylists: List<Playlist>): List<Playlist> {
            val failed = mutableListOf<Playlist>()
            for (playlist in filePlaylists) {
                val location = (playlist.location as FilePlaylistLocation)
                val uri = mediastoreUriPlaylist(location.storageVolume, location.mediastoreId)
                val result = MediaStorePlaylistsActions.delete(context, uri)
                if (!result) failed.add(playlist)
            }
            return failed
        }

        private suspend fun selectDocumentUris(context: Context, filePlaylists: List<Playlist>): List<Uri?> {
            val paths = filePlaylists.map { playlist -> (playlist.location as FilePlaylistLocation).path }
            return selectDocumentUris(context, paths)
        }

    }
}