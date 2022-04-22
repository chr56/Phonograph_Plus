/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

@file:Suppress("MemberVisibilityCanBePrivate")

package util.phonograph.m3u

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.activity.ComponentActivity
import com.afollestad.materialdialogs.DialogCallback
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import legacy.phonograph.LegacyPlaylistsUtil
import player.phonograph.App
import player.phonograph.R
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.playlist.FilePlaylist
import player.phonograph.model.Song
import player.phonograph.util.PlaylistsUtil
import player.phonograph.misc.SAFCallbackHandlerActivity
import player.phonograph.misc.SafLauncher
import player.phonograph.util.Util.coroutineToast
import util.mdcolor.pref.ThemeColor
import util.phonograph.m3u.internal.M3UGenerator
import util.phonograph.m3u.internal.appendTimestampSuffix
import java.io.File
import java.io.IOException

class PlaylistsManager(private val context: Context, requester: SAFCallbackHandlerActivity?) {
    private val activity: ComponentActivity? = requester as ComponentActivity?
    private val safLauncher: SafLauncher? = requester?.getSafLauncher()

    fun createPlaylist(name: String, songs: List<Song>? = null, path: String? = null) {
        GlobalScope.launch(Dispatchers.Default) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && activity != null && safLauncher != null) {
                FileOperator.createPlaylistViaSAF(name, songs, safLauncher, activity)
            } else {
                // legacy ways
                LegacyPlaylistsUtil.createPlaylist(context, name).also { id ->
                    if (PlaylistsUtil.doesPlaylistExist(context, id)) {
                        songs?.let {
                            LegacyPlaylistsUtil.addToPlaylist(context, it, id, true)
                        }
                    } else {
                        coroutineToast(context, R.string.failed)
                    }
                }
            }
        }
    }

    fun appendPlaylist(songs: List<Song>, filePlaylist: FilePlaylist) {
        GlobalScope.launch(Dispatchers.Default) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && safLauncher != null && activity != null) {
                coroutineToast(activity, R.string.direction_open_file_with_saf)
                FileOperator.appendToPlaylistViaSAF(songs, filePlaylist, false, context, safLauncher)
            } else {
                LegacyPlaylistsUtil.addToPlaylist(context, songs, filePlaylist.id, true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) coroutineToast(context, R.string.failed)
            }
        }
    }
    fun appendPlaylist(songs: List<Song>, playlistId: Long) {
        appendPlaylist(songs, PlaylistsUtil.getPlaylist(context, playlistId))
    }

    fun deletePlaylistWithGuide(filePlaylists: List<FilePlaylist>) {
        GlobalScope.launch(Dispatchers.Default) {
            // try to deleted
            val failList = LegacyPlaylistsUtil.deletePlaylists(context, filePlaylists)

            if (failList.isNotEmpty()) {

                // generate error msg
                val list = StringBuffer()
                for (playlist in failList) {
                    list.append(playlist.name).append("\n")
                }
                val msg = "${ context.resources.getQuantityString(R.plurals.msg_deletion_result, filePlaylists.size, filePlaylists.size - failList.size, filePlaylists.size) }\n" +
                    " ${context.getString(R.string.failed_to_delete)}: \n $list "

                // setup delete with saf callback
                val callback: DialogCallback = {
                    GlobalScope.launch(Dispatchers.IO) {
                        if (safLauncher != null && activity != null) {
                            // todo remove hardcode
                            val regex = "/(sdcard)|(storage/emulated)/\\d+/".toRegex()
                            val rawPath = PlaylistsUtil.getPlaylistPath(context, filePlaylists[0])
                            val path = regex.replace(rawPath.removePrefix(Environment.getExternalStorageDirectory().absolutePath), "")

                            val parentFolderUri = Uri.parse(
                                "content://com.android.externalstorage.documents/document/primary:" + Uri.encode(path)
                            )

                            coroutineToast(context, context.getString(R.string.direction_open_folder_with_saf), true)
                            safLauncher.openDir(parentFolderUri) { uri: Uri? ->
                                uri?.let { FileOperator.deletePlaylistsViaSAF(activity, filePlaylists, it) }
                                return@openDir Unit
                            }
                        } else {
                            coroutineToast(context, R.string.failed)
                        }
                    }
                }
                // report failure
                withContext(Dispatchers.Main) {
                    MaterialDialog(context)
                        .title(R.string.failed_to_delete)
                        .message(text = msg)
                        .positiveButton(android.R.string.ok)
                        .negativeButton(R.string.delete_with_saf, click = callback)
                        .also {
                            // color
                            it.getActionButton(WhichButton.POSITIVE).updateTextColor(ThemeColor.accentColor(context))
                            it.getActionButton(WhichButton.NEGATIVE).updateTextColor(ThemeColor.accentColor(context))
                            it.getActionButton(WhichButton.NEUTRAL).updateTextColor(ThemeColor.accentColor(context))
                        }
                        .show()
                }
            }
        }
    }

    fun duplicatePlaylistsViaSaf(filePlaylists: List<Playlist>) {
        GlobalScope.launch(Dispatchers.Default) {

            if (activity != null && safLauncher != null) {
                FileOperator.createPlaylistsViaSAF(filePlaylists, context, safLauncher)
            } else {
                // legacy ways
                withContext(Dispatchers.IO) {
                    legacySavePlaylists(filePlaylists)
                }
            }
        }
    }
    fun duplicatePlaylistViaSaf(playlist: Playlist) {
        val songs: List<Song> = playlist.getSongs(activity ?: App.instance)

        createPlaylist(appendTimestampSuffix(playlist.name), songs)
    }

    private suspend fun legacySavePlaylists(filePlaylists: List<Playlist>) {
        var successes = 0
        var failures = 0
        var dir: String? = ""
        val failureList = StringBuffer()
        for (playlist in filePlaylists) {
            try {
                dir = M3UGenerator.writeFile(
                    App.instance, File(Environment.getExternalStorageDirectory(), Environment.DIRECTORY_DOWNLOADS), playlist
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

    companion object {
        private const val TAG = "PlaylistManager"
    }
}
