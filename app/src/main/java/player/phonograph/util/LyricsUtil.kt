/*
 * Copyright (c) 2021 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import player.phonograph.App
import player.phonograph.helper.MusicPlayerRemote
import player.phonograph.model.Song
import player.phonograph.model.lyrics2.LrcLyrics
import player.phonograph.model.lyrics2.LyricsLoader.loadLyrics
import player.phonograph.model.lyrics2.getLrcLyrics
import player.phonograph.settings.Setting
import java.io.File

class LyricsFetcher {
    var lyrics: LrcLyrics?

    constructor(lyrics: LrcLyrics) {
        this.lyrics = lyrics
    }

    constructor(song: Song) {
        this.lyrics = null
        File(song.data).also { file ->
            if (!file.exists()) return@also
            CoroutineScope(Dispatchers.IO).launch {
                loadLyrics(file, song.title).let {
                    this@LyricsFetcher.lyrics = getLrcLyrics(it)
                }
            }
        }
    }

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
    constructor(looper: Looper, context: Context, lyrics: LrcLyrics) : this(looper, context, LyricsFetcher(lyrics))

    fun start() {
        queueNextRefresh(1)
    }
    fun stop() {
        removeMessages(CMD_REFRESH)
        App.instance.lyricsService.stopLyric()
    }

    fun replaceFetcher(fetcher: LyricsFetcher) { this.fetcher = fetcher }
    fun replaceLyrics(lyrics: LrcLyrics) {
        fetcher.lyrics = lyrics
    }
    fun replaceSong(song: Song) {
        fetcher = LyricsFetcher(song)
    }

    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)
        if (msg.what == CMD_REFRESH) {
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
        val message = obtainMessage(CMD_REFRESH)
        removeMessages(CMD_REFRESH)
        sendMessageDelayed(message, delay)
    }

    private var cache: String = ""

    private fun broadcast(time: Int) {
        fetcher.getLine(time)?.let { line ->
            if (line != cache) {
                // sending only when playing
                if (MusicPlayerRemote.isPlaying()) {
                    if (!Setting.instance.broadcastSynchronizedLyrics) return // do nothing
                    App.instance.lyricsService.updateLyric(line)
                }
                // update cache
                cache = line
            }
        } ?: App.instance.lyricsService.stopLyric()
    }

    companion object {
        private const val CMD_REFRESH = 1
        private const val MIN_INTERVAL = 20
        private const val UPDATE_INTERVAL_PLAYING = 1000
        private const val UPDATE_INTERVAL_PAUSED = 500
    }
}
