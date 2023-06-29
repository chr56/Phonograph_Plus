/*
 * Copyright (c) 2022~2023 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.mechanism.lyrics

import player.phonograph.mechanism.StatusBarLyric
import player.phonograph.model.Song
import player.phonograph.model.lyrics.LrcLyrics
import player.phonograph.notification.ErrorNotification
import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.runBlocking
import java.io.File

class LyricsUpdater(song: Song?) {
    private lateinit var fetcher: LyricsFetcher

    /**
     * lyrics would be loaded after set
     */
    var currentSong: Song? = song
        @Synchronized set(value) {
            field = value
            updateLyrics()
        }

    private fun updateLyrics() {
        val song: Song = currentSong ?: return
        // fetch lyrics
        File(song.data).also { file ->
            if (!file.exists()) return@also
            runBlocking(exceptionHandler) {
                LyricsLoader.loadLyrics(file, song).let {
                    fetcher = LyricsFetcher(it.firstLrcLyrics())
                }
            }
        }
    }

    /**
     * cached lyrics line
     */
    private var cache: String = ""

    /**
     * broadcase lyrics
     */
    fun broadcast(processInMills: Int) {

        val newLine = fetcher.getLine(processInMills)

        if (newLine != null) {
            if (newLine != cache) {
                cache = newLine // update cache
                StatusBarLyric.updateLyric(newLine)
            }
        } else {
            cache = ""
            StatusBarLyric.stopLyric()
        }
    }

    @Synchronized
    fun forceReplaceLyrics(lyrics: LrcLyrics) {
        fetcher = LyricsFetcher(lyrics)
    }

    fun clear() {
        cache = ""
        currentSong = null
    }

    private val exceptionHandler by lazy {
        CoroutineExceptionHandler { _, throwable ->
            val msg = "Exception while fetching lyrics!"
            Log.w("LyricsFetcher", "${msg}\n${throwable.message}")
            ErrorNotification.postErrorNotification(throwable, note = msg)
        }
    }

    class LyricsFetcher(private val lyrics: LrcLyrics?) {
        fun getLine(time: Int): String? {
            val offsetTime = if (time > 100) time - 100 else time
            return lyrics?.getLine(offsetTime)?.first
        }
    }
}
