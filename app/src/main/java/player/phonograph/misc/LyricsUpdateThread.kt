/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.misc

import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.runBlocking
import player.phonograph.App
import player.phonograph.helper.MusicPlayerRemote
import player.phonograph.model.Song
import player.phonograph.model.lyrics2.LrcLyrics
import player.phonograph.model.lyrics2.LyricsLoader
import player.phonograph.notification.ErrorNotification
import player.phonograph.settings.Setting
import java.io.File

class LyricsUpdateThread(song: Song? = null) : Thread() {
    /**
     * update this in [updateLyrics]
     */
    private var _lyricsFetcher: LyricsFetcher? = null
    private val lyricsFetcher: LyricsFetcher get() = _lyricsFetcher!!

    init {
        updateLyrics()
    }

    /**
     * lyrics would be loaded after setter
     */
    var currentSong: Song? = song
        @Synchronized get
        @Synchronized set(value) {
            field = value
            updateLyrics()
        }

    private var quit: Boolean = false

    /**
     * stop loop and quit
     */
    fun quit() {
        quit = true
    }

    private var intervalTime: Long = 500
    private var sleepTime: Long = 50

    override fun run() {
        super.run()
        loop()
        App.instance.lyricsService.stopLyric()
    }

    /**
     * cached lyrics line
     */
    private var cache: String = ""

    /**
     * update lyricsFetcher by [currentSong]
     */
    private fun updateLyrics() {
        if (currentSong == null) return
        // fetch lyrics
        File(currentSong!!.data).also { file ->
            if (!file.exists()) return@also
            runBlocking(exceptionHandler) {
                LyricsLoader.loadLyrics(file, currentSong!!).let {
                    _lyricsFetcher = LyricsFetcher(it.getLrcLyrics())
                }
            }
        }
    }
    private fun loop() {
        while (!quit) {

            sleep(sleepTime)

            if (!checkFetcher()) continue

            if (!MusicPlayerRemote.isPlaying() || !Setting.instance.broadcastSynchronizedLyrics || lyricsFetcher.lyrics == null) { // sending only when playing
                sleepTime = 1000 // todo
                Log.v("LyricsUpdateThread", "Stop lyrics broadcast!")
                App.instance.lyricsService.stopLyric()
                continue
            } else {
                sleepTime = 50
            }

            val newLine = lyricsFetcher.getLine(MusicPlayerRemote.getSongProgressMillis())

            if (newLine != null) {
                if (newLine != cache) {
                    cache = newLine // update cache
                    App.instance.lyricsService.updateLyric(newLine)
                }
            } else {
                cache = ""
                App.instance.lyricsService.stopLyric()
            }
            sleep(intervalTime)
        }
    }

    @Synchronized
    fun forceReplaceLyrics(lyrics: LrcLyrics) {
        _lyricsFetcher = LyricsFetcher(lyrics)
    }

    private fun checkFetcher(): Boolean {
        if (_lyricsFetcher == null)return false
        if (_lyricsFetcher?.lyrics == null) return false

        return true
    }

    private val exceptionHandler by lazy {
        CoroutineExceptionHandler { _, throwable ->
            val msg = "Exception while fetching lyrics!"
            Log.w("LyricsFetcher", "${msg}\n${throwable.message}")
            ErrorNotification.init()
            ErrorNotification.postErrorNotification(throwable, note = msg)
        }
    }
}

class LyricsFetcher(lyrics: LrcLyrics? = null) {
    var lyrics: LrcLyrics? = lyrics
        private set

    fun replaceLyrics(newLyrics: LrcLyrics) {
        this.lyrics = newLyrics
    }

    fun getLine(time: Int): String? {
        val offsetTime = if (time > 100) time - 100 else time
        return lyrics?.getLine(offsetTime)?.first
    }
}
