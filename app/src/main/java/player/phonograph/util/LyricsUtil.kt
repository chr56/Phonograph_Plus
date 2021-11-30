/*
 * Copyright (c) 2021 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.util

import android.content.Context
import android.content.Intent
import android.os.Environment
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.json.JSONArray
import player.phonograph.App
import player.phonograph.BuildConfig
import player.phonograph.R
import player.phonograph.helper.MusicPlayerRemote
import player.phonograph.model.Song
import player.phonograph.model.lyrics.AbsLyrics
import player.phonograph.model.lyrics.LyricsParsed
import player.phonograph.model.lyrics.LyricsParsedSynchronized
import java.io.File
import java.io.FileOutputStream
import java.util.regex.Pattern

/**
 * Util for Lyrics
 */
object LyricsUtil {

    fun readRawLyrics(song: Song): String {

        var rawLyrics: String? = null
        val file = File(song.data)

        // from song
        try {
            rawLyrics = AudioFileIO.read(file).tagOrCreateDefault.getFirst(FieldKey.LYRICS)
        } catch (e: Exception) {
            val buildType = BuildConfig.BUILD_TYPE
            if (buildType != "release" || buildType != "preview") { e.printStackTrace() }
        }

        // from file
        if (rawLyrics == null || rawLyrics.trim { it <= ' ' }.isEmpty()) {
            val dir = file.absoluteFile.parentFile
            if (dir != null && dir.exists() && dir.isDirectory) {
                val format = ".*%s.*\\.(lrc|txt)"

                val filename = Pattern.quote(FileUtil.stripExtension(file.name))
                val songTitle = Pattern.quote(song.title)

                val patterns = listOf<Pattern>(
                    Pattern.compile(String.format(format, filename), Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE),
                    Pattern.compile(String.format(format, songTitle), Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE)
                )

                val files = dir.listFiles { f: File ->
                    for (pattern in patterns) {
                        if (pattern.matcher(f.name).matches())
                            return@listFiles true
                    }
                    false
                }

                if (files != null && files.isNotEmpty()) {
                    for (f in files) {
                        // todo Precise match
                        try {
                            rawLyrics = FileUtil.read(f)
                            if (rawLyrics.trim { it <= ' ' }.isNotEmpty()) {
                                return rawLyrics
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }

        return rawLyrics ?: throw Exception("NO_LYRICS")
    }

    fun checkType(raw: String): Short {
        val lrc = Pattern.compile("(\\[.+\\])+.*", Pattern.MULTILINE)
        return if (lrc.matcher(raw).find()) {
            AbsLyrics.LRC
        } else {
            AbsLyrics.TXT
        }
    }

    fun loadLyrics(raw: String): AbsLyrics? {
        return if (checkType(raw) == AbsLyrics.LRC) {
            LyricsParsedSynchronized.parse(raw)
        } else {
            LyricsParsed.parse(raw)
        }
    }

    fun fetchLyrics(song: Song): AbsLyrics? {
        var raw: String? = null
        try {
            raw = readRawLyrics(song)
        } catch (e: Exception) {
            if (e.message == "NO_LYRICS") {
                return null
            } else {
                e.printStackTrace()
            }
        }
        return if (raw != null) loadLyrics(raw)
        else null
    }

    /**
     * broadcast for "MIUI StatusBar Lyrics" Xposed module
     * @param line the lyrics
     */
    fun broadcastLyrics(context: Context, line: String) {
        if (!PreferenceUtil.getInstance(context).broadcastSynchronizedLyrics()) return
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
        if ((!PreferenceUtil.getInstance(context).broadcastSynchronizedLyrics()) && (!force)) return
        context.sendBroadcast(
            Intent().setAction("Lyric_Server").putExtra("Lyric_Type", "app_stop")
        )
    }

    /**
     * write a file for "MIUI StatusBar Lyrics" Xposed module
     * @param line the lyrics
     */
    fun writeLyricsFile(context: Context, line: String) {
        if (!PreferenceUtil.getInstance(context).broadcastSynchronizedLyrics()) return
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
        if (!PreferenceUtil.getInstance(App.instance).broadcastSynchronizedLyrics()) return
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
