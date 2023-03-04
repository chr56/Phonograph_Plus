/*
 * Copyright (c) 2022 chr_56
 */

@file:Suppress("MemberVisibilityCanBePrivate")

package util.phonograph.m3u

import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import legacy.phonograph.LegacyPlaylistsUtil
import lib.phonograph.misc.ICreateFileStorageAccess
import lib.phonograph.misc.IOpenDirStorageAccess
import lib.phonograph.misc.IOpenFileStorageAccess
import mt.pref.ThemeColor
import player.phonograph.App
import player.phonograph.R
import player.phonograph.model.Song
import player.phonograph.model.playlist.FilePlaylist
import player.phonograph.model.playlist.Playlist
import player.phonograph.notification.ErrorNotification
import player.phonograph.settings.Setting
import player.phonograph.util.CoroutineUtil.coroutineToast
import player.phonograph.util.PlaylistsUtil
import util.phonograph.playlist.m3u.M3UGenerator
import util.phonograph.playlist.appendTimestampSuffix
import util.phonograph.playlist.appendToPlaylistViaSAF
import util.phonograph.playlist.createPlaylistViaSAF
import util.phonograph.playlist.createPlaylistsViaSAF
import util.phonograph.playlist.deletePlaylistsViaSAF
import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException

object PlaylistsManager {

    /**
     * @param context must be ICreateFileStorageAccess
     */
    fun createPlaylist(
        context: Context,
        name: String,
        songs: List<Song>? = null,
        path: String? = null,
    ) {
        CoroutineScope(SupervisorJob())
            .launch(Dispatchers.Default) {
                if (shouldUseSAF && context is ICreateFileStorageAccess) {
                    createPlaylistViaSAF(context, playlistName = name, songs = songs)
                } else {
                    // legacy ways
                    LegacyPlaylistsUtil.createPlaylist(context, name).also { id ->
                        if (PlaylistsUtil.doesPlaylistExist(context, id)) {
                            songs?.let {
                                LegacyPlaylistsUtil.addToPlaylist(context, it, id, true)
                                coroutineToast(context, R.string.success)
                            }
                        } else {
                            coroutineToast(context, R.string.failed)
                            ErrorNotification.postErrorNotification(
                                Exception("Failed to save playlist (id=$id)"),
                                null
                            )
                        }
                    }
                }
            }
    }

    fun appendPlaylist(
        context: Context,
        songs: List<Song>,
        filePlaylist: FilePlaylist,
    ) {
        CoroutineScope(SupervisorJob()).launch(Dispatchers.Default) {
            if (shouldUseSAF && context is IOpenFileStorageAccess) {
                coroutineToast(context, R.string.direction_open_file_with_saf)
                appendToPlaylistViaSAF(
                    context,
                    songs = songs,
                    filePlaylist = filePlaylist,
                )
            } else {
                LegacyPlaylistsUtil.addToPlaylist(context, songs, filePlaylist.id, true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) coroutineToast(
                    context,
                    R.string.failed
                )
            }
        }
    }

    fun appendPlaylist(
        context: Context,
        songs: List<Song>,
        playlistId: Long,
    ) = appendPlaylist(context, songs, PlaylistsUtil.getPlaylist(context, playlistId))

    /**
     * @param context must be IOpenDirStorageAccess
     */
    fun deletePlaylistWithGuide(
        context: Context,
        filePlaylists: List<FilePlaylist>,
    ) {
        val scope = CoroutineScope(SupervisorJob())
        scope.launch(Dispatchers.Default) {
            // try to delete
            val failList = LegacyPlaylistsUtil.deletePlaylists(context, filePlaylists)

            if (failList.isNotEmpty()) {

                // generate error msg
                val list = StringBuffer()
                for (playlist in failList) {
                    list.append(playlist.name).append("\n")
                }
                val msg = "${
                    context.resources.getQuantityString(
                        R.plurals.msg_deletion_result,
                        filePlaylists.size,
                        filePlaylists.size - failList.size,
                        filePlaylists.size
                    )
                }\n" +
                        " ${context.getString(R.string.failed_to_delete)}: \n $list "

                // report failure
                withContext(Dispatchers.Main) {
                    MaterialDialog(context)
                        .title(R.string.failed_to_delete)
                        .message(text = msg)
                        .positiveButton(android.R.string.ok)
                        .negativeButton(R.string.delete_with_saf) {
                            scope.launch(Dispatchers.IO) {
                                if (context is Activity && context is IOpenDirStorageAccess) {
                                    deletePlaylistsViaSAF(context, filePlaylists)
                                } else {
                                    coroutineToast(context, R.string.failed)
                                }
                            }
                        }
                        .also {
                            // color
                            it.getActionButton(WhichButton.POSITIVE)
                                .updateTextColor(ThemeColor.accentColor(context))
                            it.getActionButton(WhichButton.NEGATIVE)
                                .updateTextColor(ThemeColor.accentColor(context))
                            it.getActionButton(WhichButton.NEUTRAL)
                                .updateTextColor(ThemeColor.accentColor(context))
                        }
                        .show()
                }
            }
        }
    }

    private val shouldUseSAF: Boolean
        get() {
            return when (val behavior = Setting.instance.playlistFilesOperationBehaviour) {
                Setting.PLAYLIST_OPS_BEHAVIOUR_FORCE_SAF    -> true
                Setting.PLAYLIST_OPS_BEHAVIOUR_FORCE_LEGACY -> false
                Setting.PLAYLIST_OPS_BEHAVIOUR_AUTO         -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                else                                        -> {
                    Setting.instance.playlistFilesOperationBehaviour =
                        Setting.PLAYLIST_OPS_BEHAVIOUR_AUTO // reset to default
                    throw IllegalStateException("$behavior is not a valid option")
                }
            }
        }

    fun duplicatePlaylistsViaSaf(
        context: Context,
        filePlaylists: List<Playlist>,
    ) {
        CoroutineScope(SupervisorJob()).launch(Dispatchers.Default) {
            if (context is IOpenDirStorageAccess) {
                createPlaylistsViaSAF(context, filePlaylists)
            } else {
                // legacy ways
                withContext(Dispatchers.IO) {
                    legacySavePlaylists(context, filePlaylists)
                }
            }
        }
    }

    fun duplicatePlaylistViaSaf(
        context: Context,
        playlist: Playlist,
    ) = createPlaylist(context, appendTimestampSuffix(playlist.name), playlist.getSongs(context))

    private suspend fun legacySavePlaylists(context: Context, filePlaylists: List<Playlist>) {
        var successes = 0
        var failures = 0
        var dir: String? = ""
        val failureList = StringBuffer()
        for (playlist in filePlaylists) {
            try {
                dir = M3UGenerator.writeFile(
                    App.instance,
                    File(
                        Environment.getExternalStorageDirectory(),
                        Environment.DIRECTORY_DOWNLOADS
                    ),
                    playlist
                ).parent
                successes++
            } catch (e: IOException) {
                failures++
                failureList.append(playlist.name).append(" ")
                Log.w(TAG, e.message ?: "")
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

    private const val TAG = "PlaylistManager"
}
