/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.mechanism.playlist

import lib.activityresultcontract.ICreateFileStorageAccess
import lib.activityresultcontract.IOpenFileStorageAccess
import player.phonograph.App
import player.phonograph.R
import player.phonograph.model.Song
import player.phonograph.model.playlist.FilePlaylist
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.playlist.SmartPlaylist
import player.phonograph.repo.mediastore.loaders.PlaylistLoader
import player.phonograph.settings.Keys
import player.phonograph.settings.PLAYLIST_OPS_BEHAVIOUR_AUTO
import player.phonograph.settings.PLAYLIST_OPS_BEHAVIOUR_FORCE_LEGACY
import player.phonograph.settings.PLAYLIST_OPS_BEHAVIOUR_FORCE_SAF
import player.phonograph.settings.Setting
import player.phonograph.util.coroutineToast
import player.phonograph.util.sentPlaylistChangedLocalBoardCast
import player.phonograph.util.text.currentDate
import player.phonograph.util.text.dateTimeSuffix
import player.phonograph.util.warning
import player.phonograph.mechanism.playlist.m3u.M3UWriter
import player.phonograph.mechanism.playlist.mediastore.addToPlaylistViaMediastore
import player.phonograph.mechanism.playlist.mediastore.createOrFindPlaylistViaMediastore
import player.phonograph.mechanism.playlist.saf.appendToPlaylistViaSAF
import player.phonograph.mechanism.playlist.saf.createPlaylistViaSAF
import player.phonograph.mechanism.playlist.saf.createPlaylistsViaSAF
import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

object PlaylistEdit {

    suspend fun create(
        context: Context,
        name: String,
        songs: List<Song>,
    ) = if (shouldUseSAF(context) && context is ICreateFileStorageAccess) {
        PlaylistEditImpl.SAF.create(context, name, songs)
    } else {
        PlaylistEditImpl.MediaStore.create(context, name, songs)
    }

    suspend fun append(
        context: Context,
        songs: List<Song>,
        filePlaylist: FilePlaylist,
    ) = if (shouldUseSAF(context) && context is IOpenFileStorageAccess) {
        PlaylistEditImpl.SAF.append(context, songs, filePlaylist)
    } else {
        PlaylistEditImpl.MediaStore.append(context, songs, filePlaylist)
    }

    suspend fun duplicate(
        context: Context,
        playlist: Playlist,
    ) = create(context, playlist.name + dateTimeSuffix(currentDate()), playlist.getSongs(context))

    suspend fun duplicate(
        context: Context,
        playlists: List<Playlist>,
    ) =  if (shouldUseSAF(context) && context is ICreateFileStorageAccess) {
        PlaylistEditImpl.SAF.duplicate(context, playlists)
    } else {
        PlaylistEditImpl.MediaStore.duplicate(context, playlists)
    }


    private fun shouldUseSAF(context: Context): Boolean {
        val preference = Setting(context)[Keys.playlistFilesOperationBehaviour]
        return when (preference.data) {
            PLAYLIST_OPS_BEHAVIOUR_FORCE_SAF    -> true
            PLAYLIST_OPS_BEHAVIOUR_FORCE_LEGACY -> false
            PLAYLIST_OPS_BEHAVIOUR_AUTO         -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
            else                                -> {
                preference.data = PLAYLIST_OPS_BEHAVIOUR_AUTO // reset to default
                return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
            }
        }
    }
}

private sealed interface PlaylistEditImpl {

    suspend fun create(
        context: Context,
        name: String,
        songs: List<Song>,
    )

    suspend fun append(
        context: Context,
        songs: List<Song>,
        filePlaylist: FilePlaylist,
    )

    suspend fun duplicate(
        context: Context,
        playlists: List<Playlist>,
    )

    data object SAF : PlaylistEditImpl {
        override suspend fun create(context: Context, name: String, songs: List<Song>) {
            CoroutineScope(Dispatchers.IO).launch { // independent scope to avoid scope is canceled
                createPlaylistViaSAF(context, playlistName = name, songs = songs)
            }
        }

        override suspend fun append(context: Context, songs: List<Song>, filePlaylist: FilePlaylist) {
            coroutineToast(context, R.string.direction_open_file_with_saf)
            appendToPlaylistViaSAF(context, songs = songs, filePlaylist = filePlaylist)
        }

        override suspend fun duplicate(context: Context, playlists: List<Playlist>) {
            CoroutineScope(Dispatchers.IO).launch { // independent scope to avoid scope is canceled
                createPlaylistsViaSAF(context, playlists, defaultDirectory.absolutePath)
            }
        }

        private val defaultDirectory: File get() = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
    }

    data object MediaStore : PlaylistEditImpl {

        private const val TAG = "PlaylistEditImpl"

        override suspend fun create(context: Context, name: String, songs: List<Song>) {
            val id = createOrFindPlaylistViaMediastore(context, name)
            if (PlaylistLoader.checkExistence(context, id)) {
                addToPlaylistViaMediastore(context, songs, id, true)
                coroutineToast(context, R.string.success)
                delay(250)
                sentPlaylistChangedLocalBoardCast()
            } else {
                warning(TAG, "Failed to save playlist (id=$id)")
                coroutineToast(context, R.string.failed)
            }
        }

        override suspend fun append(context: Context, songs: List<Song>, filePlaylist: FilePlaylist) {
            addToPlaylistViaMediastore(context, songs, filePlaylist.id, true)
        }

        override suspend fun duplicate(context: Context, playlists: List<Playlist>) {
            var successes = 0
            var failures = 0
            var dir: String? = ""
            val failureList = StringBuffer()
            for (playlist in playlists) {
                try {
                    val filename: String =
                        if (playlist is SmartPlaylist) {
                            // Since AbsCustomPlaylists are dynamic, we add a timestamp after their names.
                            playlist.name + dateTimeSuffix(currentDate())
                        } else {
                            playlist.name
                        }
                    val songs = playlist.getSongs(context)
                    dir = M3UWriter.write(File(Environment.DIRECTORY_DOWNLOADS), songs, filename).parent
                    successes++
                } catch (e: IOException) {
                    failures++
                    failureList.append(playlist.name).append(" ")
                    Log.w(TAG, e.message.orEmpty())
                }
            }
            val msg =
                if (failures == 0) String.format(
                    App.instance.applicationContext.getString(R.string.saved_x_playlists_to_x),
                    successes, dir
                ) else String.format(
                    App.instance.applicationContext.getString(R.string.saved_x_playlists_to_x_failed_to_save_x),
                    successes, dir, failureList
                )
            coroutineToast(context, msg)
        }
    }
}