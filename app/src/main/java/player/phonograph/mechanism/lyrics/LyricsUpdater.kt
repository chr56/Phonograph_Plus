/*
 * Copyright (c) 2022~2023 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.mechanism.lyrics

import player.phonograph.mechanism.StatusBarLyric
import player.phonograph.model.Song
import player.phonograph.model.lyrics.LrcLyrics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class LyricsUpdater(song: Song?) {
    private var fetcher: LyricsFetcher? = null

    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    /**
     * lyrics would be loaded after set
     */
    var currentSong: Song? = song
        @Synchronized set(value) {
            field = value
            updateLyrics(value)
        }

    private fun updateLyrics(song: Song?) {
        // fetch lyrics
        coroutineScope.launch(Dispatchers.IO) {
            fetcher =
                if (song != null) {
                    File(song.data).let { file ->
                        if (file.exists()) {
                            val lyrics = LyricsLoader.loadLyrics(file, song)
                            LyricsFetcher(lyrics.firstLrcLyrics())
                        } else {
                            LyricsFetcher(null)
                        }
                    }
                } else {
                    LyricsFetcher(null)
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
        coroutineScope.launch(Dispatchers.IO) {
            val newLine = fetcher?.getLine(processInMills)

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
    }

    @Synchronized
    fun forceReplaceLyrics(lyrics: LrcLyrics) {
        fetcher = LyricsFetcher(lyrics)
    }

    fun clear() {
        cache = ""
        currentSong = null
    }

    class LyricsFetcher(private val lyrics: LrcLyrics?) {
        fun getLine(time: Int): String? {
            val offsetTime = if (time > 100) time - 100 else time
            return lyrics?.getLine(offsetTime)?.first
        }
    }
}
