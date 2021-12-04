/*
 * Copyright (c) 2021 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import player.phonograph.App
import player.phonograph.BuildConfig
import player.phonograph.helper.MusicPlayerRemote
import player.phonograph.model.Song
import player.phonograph.model.lyrics.AbsLyrics
import player.phonograph.model.lyrics.LyricsParsed
import player.phonograph.model.lyrics.LyricsParsedSynchronized
import java.io.File
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
            AudioFileIO.read(file).tagOrCreateDefault.getFirst(FieldKey.LYRICS).also {
                if (it.trim().isNotEmpty()) return it
            }
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

        fun getLine(time: Int): String? {
            val offsetTime = if (time > 100) time - 100 else time
            return lyrics?.getLine(offsetTime)
        }
    }

    /**
     * broadcast for "MIUI StatusBar Lyrics" Xposed module
     */
    class LyricsRefresher(looper: Looper, private var context: Context, private var fetcher: LyricsFetcher) : Handler(looper) {

        constructor(looper: Looper, context: Context, song: Song) : this(looper, context, LyricsFetcher(song))
        constructor(looper: Looper, context: Context, lyrics: LyricsParsedSynchronized) : this(looper, context, LyricsFetcher(lyrics))

        fun start() {
            queueNextRefresh(1)
        }
        fun stop() {
            removeMessages(CMD_REFRESH_PROGRESS_VIEWS)
            App.instance.lyricsService.stopLyric()
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

        private var cache: String = ""

        private fun broadcast(time: Int) {
            fetcher.getLine(time)?.let { line ->
                if (line != cache) {
                    // sending only when playing
                    if (MusicPlayerRemote.isPlaying()) {
                        if (!PreferenceUtil.getInstance(context).broadcastSynchronizedLyrics()) return // do nothing
                        App.instance.lyricsService.updateLyric(line)
                    }
                    // update cache
                    cache = line
                }
            } ?: App.instance.lyricsService.stopLyric()
        }

        companion object {
            private const val CMD_REFRESH_PROGRESS_VIEWS = 1
            private const val MIN_INTERVAL = 20
            private const val UPDATE_INTERVAL_PLAYING = 1000
            private const val UPDATE_INTERVAL_PAUSED = 500
        }
    }
}
