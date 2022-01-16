/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.util

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import player.phonograph.R
import player.phonograph.helper.M3UWriter
import player.phonograph.model.Song
import player.phonograph.util.Util.coroutineToast
import player.phonograph.util.Util.sentPlaylistChangedLocalBoardCast
import java.io.FileNotFoundException
import java.io.IOException

object PlaylistWriter {
    fun createPlaylistViaSAF(name: String, songs: List<Song>?, activity: SAFCallbackHandlerActivity) {
        val safLauncher: SafLauncher = activity.getSafLauncher()
        activity as ComponentActivity
        // prepare callback
        val uriCallback: UriCallback = { uri ->
            // callback start
            GlobalScope.launch(Dispatchers.IO) {
                if (uri == null) {
                    coroutineToast(activity, R.string.failed)
                } else {
                    sentPlaylistChangedLocalBoardCast()
                    try {
                        val outputStream = activity.contentResolver.openOutputStream(uri, "rw")
                        if (outputStream != null) {
                            try {
                                if (songs != null) M3UWriter.write(outputStream, songs)
                                coroutineToast(activity, R.string.success)
                            } catch (e: IOException) {
                                coroutineToast(activity, activity.getString(R.string.failed) + ":${uri.path} can not be written")
                            } finally {
                                outputStream.close()
                            }
                        }
                    } catch (e: FileNotFoundException) {
                        coroutineToast(activity, activity.getString(R.string.failed) + ":${uri.path} is not available")
                    }
                }
            }
            // callback end
        }

        GlobalScope.launch(Dispatchers.IO) {
            while (safLauncher.uriDocumentInUse) yield()
            try {
                safLauncher.launchSAF("$name.m3u", uriCallback)
            } catch (e: Exception) {
                coroutineToast(activity, activity.getString(R.string.failed) + ": unknown")
                Log.i("CreatePlaylistDialog", "SaveFail: \n${e.message}")
            }
        }
    }

    fun createPlaylist(name: String, songs: List<Song>?, context: Context) {
        val playlistId = PlaylistsUtil.createPlaylist(context, name)
        if (songs != null && songs.isNotEmpty()) {
            PlaylistsUtil.addToPlaylist(context, songs, playlistId, true)
        }
    }
}
typealias UriCallback = (Uri?) -> Any
class SafLauncher(private val registry: ActivityResultRegistry) : DefaultLifecycleObserver {
    private lateinit var launcher: ActivityResultLauncher<String>
    lateinit var callback: UriCallback
    var uriDocumentInUse = false
        private set

    override fun onCreate(owner: LifecycleOwner) {
        launcher = registry.register("SAF", owner, ActivityResultContracts.CreateDocument()) {
            callback(it)
            uriDocumentInUse = false
        }
    }

    fun launchSAF(fileName: String, callback: UriCallback) {
        if (uriDocumentInUse) return // todo
        uriDocumentInUse = true
        this.callback = callback
        launcher.launch(fileName)
    }
}
interface SAFCallbackHandlerActivity {
    fun getSafLauncher(): SafLauncher
}
