/*
 * Copyright (c) 2021 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.util

import android.content.Context
import android.content.Intent
import android.os.Environment
import org.json.JSONArray
import player.phonograph.App
import player.phonograph.R
import player.phonograph.helper.MusicPlayerRemote
import java.io.FileOutputStream

/**
 * Util for "MIUI StatusBar Lyrics" Xposed module
 */
object LyricsSendUtil {

    /**
     * broadcast for "MIUI StatusBar Lyrics" Xposed module
     * @param line the lyrics
     */
    fun broadcastLyrics(context: Context, line: String) {
        if (!PreferenceUtil.getInstance(context).broadcastSynchronizedLyrics()) return;
        // sending only when playing
        if (MusicPlayerRemote.isPlaying()) {
            if (line.isNotEmpty()) {
                context.sendBroadcast(
                    Intent().setAction("Lyric_Server")
                        .putExtra("Lyric_Type", "app")
                        .putExtra("Lyric_Data", line)
                        .putExtra("Lyric_PackName", App.PACKAGE_NAME)
                        // Actually, PackName is (music) service name, so we have no suffix (.plus.YOUR_BUILD_TYPE)
                        .putExtra("Lyric_Icon", context.resources.getString(R.string.icon_base64))
                        .putExtra("Lyric_UseSystemMusicActive", true)
                )
            } else {
                broadcastLyricsStop(context, false) // clear, because is null
            }
        }
    }

    /**
     * broadcast for "MIUI StatusBar Lyrics" Xposed module
     * @param force send stop intent but ignoring preference
     */
    fun broadcastLyricsStop(context: Context, force: Boolean) {
        if ((!PreferenceUtil.getInstance(context).broadcastSynchronizedLyrics()) && (!force)) return;
        context.sendBroadcast(
            Intent().setAction("Lyric_Server").putExtra("Lyric_Type", "app_stop")
        )
    }

    /**
     * write a file for "MIUI StatusBar Lyrics" Xposed module
     * @param line the lyrics
     */
    fun writeLyricsFile(context: Context, line: String) {
        if (!PreferenceUtil.getInstance(context).broadcastSynchronizedLyrics()) return;
        // sending only when playing
        if (!MusicPlayerRemote.isPlaying()) return
        try {
            val outputStream = FileOutputStream(PATH)

            val jsonArray = JSONArray()
            jsonArray.put("app")
            jsonArray.put(App.PACKAGE_NAME)
            // Actually, PackName is (music) service name, so we have no suffix (.plus.YOUR_BUILD_TYPE)
            jsonArray.put(line)
            jsonArray.put(context.resources.getString(R.string.icon_base64))
            jsonArray.put(true)

            val json: String = jsonArray.toString()
            outputStream.write(json.toByteArray())
            outputStream.close()
        } catch (ignored: Exception) {
        }
    }

    /**
     * write a file for "MIUI StatusBar Lyrics" Xposed module
     */
    fun writeLyricsFileStop() {
        if (!PreferenceUtil.getInstance(App.instance).broadcastSynchronizedLyrics()) return;
        try {
            val outputStream = FileOutputStream(PATH)
            val jsonArray = JSONArray()
            jsonArray.put("app_stop")
            val json: String = jsonArray.toString()
            outputStream.write(json.toByteArray())
            outputStream.close()
        } catch (ignored: Exception) {
        }
    }

    private val PATH =
        "${Environment.getExternalStorageDirectory().absolutePath}/Android/media/miui.statusbar.lyric/lyric.txt"
}
