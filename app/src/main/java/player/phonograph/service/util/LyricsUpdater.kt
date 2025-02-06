/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.service.util

import player.phonograph.App
import player.phonograph.mechanism.StatusBarLyric
import player.phonograph.mechanism.lyrics.LyricsLoader
import player.phonograph.model.Song
import player.phonograph.model.lyrics.LrcLyrics
import player.phonograph.service.MusicService
import player.phonograph.service.ServiceComponent
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

class LyricsUpdater : ServiceComponent {
    override var created: Boolean = true // stateless
    private var fetcher: LyricsFetcher? = null

    suspend fun updateViaSong(song: Song?) {
        if (song == null) return
        val enableLyrics = Setting(App.instance)[Keys.enableLyrics].flowData()
        if (!enableLyrics) return
        val file = File(song.data)
        fetcher = LyricsFetcher(
            if (file.exists()) LyricsLoader.loadLyrics(file, song)?.firstLrcLyrics() else null
        )
    }

    fun updateViaLyrics(lyrics: LrcLyrics) {
        fetcher = LyricsFetcher(lyrics)
    }

    override fun onCreate(musicService: MusicService) {
        val song = musicService.queueManager.currentSong
        musicService.coroutineScope.launch(SupervisorJob()) {
            updateViaSong(song)
        }
    }

    override fun onDestroy(musicService: MusicService) {
        clear()
    }

    /**
     * cached lyrics line
     */
    private var cache: String = ""

    /**
     * broadcast lyrics
     */
    fun broadcast(processInMills: Int) {
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

    fun clear() {
        cache = ""
        fetcher = null
    }

    class LyricsFetcher(private val lyrics: LrcLyrics?) {
        fun getLine(time: Int): String? {
            val offsetTime = if (time > 100) time - 100 else time
            return lyrics?.getLine(offsetTime)?.first
        }
    }
}
