/*
 * Copyright (c) 2021 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.util

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
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

    @JvmStatic
    fun loadLyrics(raw: String): AbsLyrics {
        return if (checkType(raw) == AbsLyrics.LRC) {
            LyricsParsedSynchronized.parse(raw)
        } else {
            LyricsParsed.parse(raw)
        }
    }

    @JvmStatic
    fun checkType(raw: String): Short {
        val sample = if (raw.length > 50) raw.substring(0, 45) else raw

        val lrc = Pattern.compile("(\\[.+\\])+.*", Pattern.MULTILINE)
        return if (lrc.matcher(sample).find()) {
            AbsLyrics.LRC
        } else {
            AbsLyrics.TXT
        }
    }

    @Throws(IllegalStateException::class)
    @JvmStatic
    fun retrieveRawLyrics(song: Song): String {

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
                val filename = Pattern.quote(FileUtil.stripExtension(file.name))
                val songTitle = Pattern.quote(song.title)

                // precise pattern
                val preciseFormat = "%s\\.(lrc|txt)"
                val precisePattern = Pattern.compile(String.format(preciseFormat, filename), Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE)
//                val precisePatterns =listOf<Pattern>(
//                    Pattern.compile(String.format(preciseFormat, filename), Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE),
//                    Pattern.compile(String.format(preciseFormat, songTitle), Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE)
//                )

                // vague pattern
                val vagueFormat = ".*[-;]?%s[-;]?.*\\.(lrc|txt)"
                val vaguePatterns = listOf<Pattern>(
                    Pattern.compile(String.format(vagueFormat, filename), Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE),
                    Pattern.compile(String.format(vagueFormat, songTitle), Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE)
                )

                val preciseFiles: MutableList<File> = ArrayList(2)
                val vagueFiles: MutableList<File> = ArrayList(6)

                // start list file under the same dir
                dir.listFiles { f: File ->
                    // precise match

                    if (precisePattern.matcher(f.name).matches()) {
                        preciseFiles.add(f)
                        Log.d("LyricsUtil", "add a precise file: ${f.path}")
                        return@listFiles true
                    }

                    // vague match
                    for (pattern in vaguePatterns) {
                        if (pattern.matcher(f.name).matches()) {
                            vagueFiles.add(f)
                            Log.d("LyricsUtil", "add a vague file: ${f.path}")
                            return@listFiles true
                        }
                    }
                    false
                }?.let { allMatchedFiles ->
                    if (allMatchedFiles.isEmpty()) throw IllegalStateException("NO_LYRICS")

                    val s = StringBuffer().append("ALL FILES: ")
                    allMatchedFiles.forEach { s.append(it.path).append(" ") }
                    Log.d("LyricsUtil", s.toString())

                    // precise first
                    for (f in preciseFiles) {
                        try {
                            val raw = FileUtil.read(f)
                            if (raw.trim { it <= ' ' }.isNotEmpty()) {
                                Log.d("LyricsUtil", "use the precise file: ${f.path}")
                                return raw
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    // then vague
                    for (f in vagueFiles) {
                        try {
                            val raw = FileUtil.read(f)
                            if (raw.trim { it <= ' ' }.isNotEmpty()) {
                                Log.d("LyricsUtil", "use the vague file: ${f.path}")
                                return raw
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }

        throw IllegalStateException("NO_LYRICS")
    }

    @JvmStatic
    fun fetchLyrics(song: Song): AbsLyrics? {
        var raw: String? = null
        try {
            raw = retrieveRawLyrics(song)
        } catch (e: IllegalStateException) {
            if (e.message == "NO_LYRICS") {
                return null
            } else {
                e.printStackTrace()
            }
        } catch (e: Exception) {}

        return if (raw != null) loadLyrics(raw)
        else null
    }

    class LyricsFetcher(var lyrics: LyricsParsedSynchronized?) {

        constructor(song: Song) : this(null) {
            val lyrics = fetchLyrics(song)
            lyrics?.let {
                if (it.getType() == AbsLyrics.LRC) this.lyrics = it as LyricsParsedSynchronized
            }
        }

        fun replaceLyrics(lyrics: LyricsParsedSynchronized?) { this.lyrics = lyrics }

        fun getLine(time: Int): String? = lyrics?.getLine(time)
    }

    class LyricsRefresher(looper: Looper, private var context: Context, private var fetcher: LyricsFetcher) : Handler(looper) {

        constructor(looper: Looper, context: Context, song: Song) : this(looper, context, LyricsFetcher(song))
        constructor(looper: Looper, context: Context, lyrics: LyricsParsedSynchronized) : this(looper, context, LyricsFetcher(lyrics))

        fun start() {
            queueNextRefresh(1)
        }
        fun stop() {
            removeMessages(CMD_REFRESH_PROGRESS_VIEWS)
        }

        fun replaceFetcher(fetcher: LyricsFetcher) { this.fetcher = fetcher }
        fun replaceLyrics(lyrics: LyricsParsedSynchronized) {
            fetcher.lyrics = lyrics
        }
        fun replaceSong(song: Song) {
            fetcher.lyrics = fetchLyrics(song) as LyricsParsedSynchronized?
        }

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            if (msg.what == CMD_REFRESH_PROGRESS_VIEWS) {
                queueNextRefresh(refreshProgressViews().toLong())
            }
        }

        @Suppress("ReplaceJavaStaticMethodWithKotlinAnalog")
        private fun refreshProgressViews(): Int {
            val progressMillis = MusicPlayerRemote.getSongProgressMillis()
            val totalMillis = MusicPlayerRemote.getSongDurationMillis()

            broadcast(progressMillis)

            if (!MusicPlayerRemote.isPlaying()) {
                return UPDATE_INTERVAL_PAUSED
            }
            val remainingMillis = UPDATE_INTERVAL_PLAYING - progressMillis % UPDATE_INTERVAL_PLAYING
            return Math.max(MIN_INTERVAL, remainingMillis)
        }

        private fun queueNextRefresh(delay: Long) {
            val message = obtainMessage(CMD_REFRESH_PROGRESS_VIEWS)
            removeMessages(CMD_REFRESH_PROGRESS_VIEWS)
            sendMessageDelayed(message, delay)
        }

        private fun broadcast(time: Int) {
            fetcher.getLine(time)
                ?.let { broadcastLyrics(context, it) }
        }

        companion object {
            private const val CMD_REFRESH_PROGRESS_VIEWS = 1
            private const val MIN_INTERVAL = 20
            private const val UPDATE_INTERVAL_PLAYING = 1000
            private const val UPDATE_INTERVAL_PAUSED = 500
        }
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
