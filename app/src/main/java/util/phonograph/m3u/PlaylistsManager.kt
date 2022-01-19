/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

@file:Suppress("MemberVisibilityCanBePrivate")

package util.phonograph.m3u

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.activity.ComponentActivity
import chr_56.MDthemer.core.ThemeColor
import com.afollestad.materialdialogs.DialogCallback
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import legacy.phonograph.LegacyPlaylistsUtil
import player.phonograph.R
import player.phonograph.model.Playlist
import player.phonograph.model.Song
import player.phonograph.util.PlaylistsUtil
import player.phonograph.util.SAFCallbackHandlerActivity
import player.phonograph.util.SafLauncher
import player.phonograph.util.Util.coroutineToast

class PlaylistsManager(private val context: Context, requester: SAFCallbackHandlerActivity?) {
    private val activity: ComponentActivity? = requester as ComponentActivity?
    private val safLauncher: SafLauncher? = requester?.getSafLauncher()

    fun createPlaylist(name: String, songs: List<Song>? = null, path: String? = null) {
        GlobalScope.launch(Dispatchers.Default) {
            if (activity != null && safLauncher != null) {
                FileOperator.createPlaylistViaSAF(name, songs, safLauncher, activity)
            } else {
                coroutineToast(context, R.string.failed)
                LegacyPlaylistsUtil.createPlaylist(context, name)
            }
        }
    }

    fun appendPlaylist(songs: List<Song>, playlist: Playlist) {
        GlobalScope.launch(Dispatchers.Default) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && safLauncher != null) {
                FileOperator.appendToPlaylistViaSAF(songs, playlist, false, context, safLauncher)
            } else {
                LegacyPlaylistsUtil.addToPlaylist(context, songs, playlist.id, true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) coroutineToast(context, R.string.failed)
            }
        }
    }
    fun appendPlaylist(songs: List<Song>, playlistId: Long) {
        appendPlaylist(songs, PlaylistsUtil.getPlaylist(context, playlistId))
    }

    fun deletePlaylistWithGuide(playlists: List<Playlist>) {
        GlobalScope.launch(Dispatchers.Default) {
            // try to deleted
            val failList = LegacyPlaylistsUtil.deletePlaylists(context, playlists)

            if (failList.isNotEmpty()) {

                // generate error msg
                val list = StringBuffer()
                for (playlist in failList) {
                    list.append(playlist.name).append("\n")
                }
                val msg = "${ context.resources.getQuantityString(R.plurals.msg_deletion_result, playlists.size, playlists.size - failList.size, playlists.size) }\n" +
                    " ${context.getString(R.string.failed_to_delete)}: \n $list "

                // setup delete with saf callback
                val callback: DialogCallback = {
                    GlobalScope.launch(Dispatchers.IO) {
                        if (safLauncher != null && activity != null) {
                            // todo remove hardcode
                            val regex = "/(sdcard)|(storage/emulated)/\\d+/".toRegex()
                            val rawPath = PlaylistsUtil.getPlaylistPath(context, playlists[0])
                            val path = regex.replace(rawPath.removePrefix(Environment.getExternalStorageDirectory().absolutePath), "")

                            val parentFolderUri = Uri.parse(
                                "content://com.android.externalstorage.documents/document/primary:" + Uri.encode(path)
                            )

                            coroutineToast(context, context.getString(R.string.direction_open_folder_with_saf), true)
                            safLauncher.openDir(parentFolderUri) { uri: Uri? ->
                                uri?.let { FileOperator.deletePlaylistsViaSAF(activity, playlists, it) }
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
}
