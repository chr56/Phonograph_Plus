/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.util

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.Intent.*
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
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
            while (safLauncher.createCallbackInUse) yield()
            try {
                safLauncher.createFile("$name.m3u", uriCallback)
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
    private lateinit var createLauncher: ActivityResultLauncher<String>
    lateinit var createCallback: UriCallback
    var createCallbackInUse = false
        private set

    private lateinit var dirLauncher: ActivityResultLauncher<Uri?>
    lateinit var dirCallback: UriCallback
    var dirCallbackInUse = false
        private set

    private lateinit var openLauncher: ActivityResultLauncher<Array<String>?>
    lateinit var openCallback: UriCallback
    var openCallbackInUse = false
        private set
    override fun onCreate(owner: LifecycleOwner) {
        createLauncher = registry.register("CreateFile", owner, ActivityResultContracts.CreateDocument()) {
            createCallback(it)
            createCallbackInUse = false
        }
        dirLauncher = registry.register("OpenDir", owner, GrandDirContract()) {
            dirCallback(it)
            dirCallbackInUse = false
        }
        openLauncher = registry.register("OpenDir", owner, ActivityResultContracts.OpenDocument()) {
            openCallback(it)
            openCallbackInUse = false
        }
    }

    fun createFile(fileName: String, callback: UriCallback) {
        if (createCallbackInUse) return // todo
        createCallbackInUse = true
        this.createCallback = callback
        createLauncher.launch(fileName)
    }
    fun openDir(dir: Uri, callback: UriCallback) {
        if (dirCallbackInUse) return // todo
        dirCallbackInUse = true
        this.dirCallback = callback
        dirLauncher.launch(dir)
    }
    fun openFile(type: Array<String>?, callback: UriCallback) {
        if (openCallbackInUse) return // todo
        openCallbackInUse = true
        this.openCallback = callback
        openLauncher.launch(type)
    }
}
interface SAFCallbackHandlerActivity {
    fun getSafLauncher(): SafLauncher
}

@TargetApi(21)
class GrandDirContract : ActivityResultContract<Uri?, Uri?>() {
    override fun createIntent(context: Context, input: Uri?): Intent {
        return Intent(ACTION_OPEN_DOCUMENT_TREE).apply {
            flags = FLAG_GRANT_READ_URI_PERMISSION or FLAG_GRANT_WRITE_URI_PERMISSION or FLAG_GRANT_PERSISTABLE_URI_PERMISSION or FLAG_GRANT_PREFIX_URI_PERMISSION
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && input != null) {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, input)
            }
        }
    }
    override fun getSynchronousResult(context: Context, input: Uri?): SynchronousResult<Uri?>? = null
    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        if (intent == null || resultCode != Activity.RESULT_OK) null else intent.data
}
