/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import player.phonograph.R
import player.phonograph.helper.M3UWriter
import player.phonograph.model.Song
import player.phonograph.ui.activities.MainActivity
import player.phonograph.util.Util.coroutineToast
import player.phonograph.util.Util.sentPlaylistChangedLocalBoardCast
import java.io.FileNotFoundException
import java.io.IOException

object PlaylistWriter {
    fun savePlaylistViaSAF(name: String, songs: List<Song>?, mainActivity: MainActivity) {
        try {
            mainActivity.openDocumentPicker("$name.m3u") { uri ->
                GlobalScope.launch(Dispatchers.IO) {
                    if (uri == null) {
                        coroutineToast(mainActivity, R.string.failed)
                    } else {
                        sentPlaylistChangedLocalBoardCast()
                        try {
                            val outputStream = mainActivity.contentResolver.openOutputStream(uri, "rw")
                            if (outputStream != null) {
                                try {
                                    if (songs != null) M3UWriter.write(outputStream, songs)
                                    coroutineToast(mainActivity, R.string.success)
                                } catch (e: IOException) {
                                    coroutineToast(mainActivity, mainActivity.getString(R.string.failed) + ":${uri.path} can not be written")
                                } finally {
                                    outputStream.close()
                                }
                            }
                        } catch (e: FileNotFoundException) {
                            coroutineToast(mainActivity, mainActivity.getString(R.string.failed) + ":${uri.path} is not available")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.i("CreatePlaylistDialog", "SaveFail: \n${e.message}")
        }
    }
}
