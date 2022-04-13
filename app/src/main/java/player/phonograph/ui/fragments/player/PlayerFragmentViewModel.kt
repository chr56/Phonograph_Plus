/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.fragments.player

import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.*
import player.phonograph.model.Song
import player.phonograph.model.lyrics2.LyricsLoader
import player.phonograph.model.lyrics2.LyricsPack
import player.phonograph.model.lyrics2.LyricsWithSource
import java.io.File

class PlayerFragmentViewModel : ViewModel() {
    val backgroundCoroutine: CoroutineScope by lazy { CoroutineScope(Dispatchers.IO) }

    private val exceptionHandler by lazy {
        CoroutineExceptionHandler { _, throwable ->
            Log.w("LyricsFetcher", "Exception while fetching lyrics!\n${throwable.message}")
        }
    }

    var currentSong: Song = Song.EMPTY_SONG
        set(value) {
            field = value
            loadLyrics(value)
        }
    var lyricsPack: LyricsPack? = null
        private set
    var currentLyrics: LyricsWithSource? = null
        private set
    fun forceReplaceLyrics(lyrics: LyricsWithSource) {
        currentLyrics = lyrics
    }

    private var loadLyricsJob: Job? = null
    private fun loadLyrics(song: Song) {
        if (song == Song.EMPTY_SONG) return
        // cancel old song's lyrics after switching
        loadLyricsJob?.cancel()
        currentLyrics = null
        lyricsPack = null
        // load new lyrics
        loadLyricsJob = backgroundCoroutine.launch(exceptionHandler) {
            lyricsPack = LyricsLoader.loadLyrics(File(song.data), song.title)
            currentLyrics = lyricsPack!!.getAvailableLyrics()
        }
    }
}
