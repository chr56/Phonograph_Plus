/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.fragments.player

import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.*
import player.phonograph.model.Song
import player.phonograph.model.lyrics2.AbsLyrics
import player.phonograph.model.lyrics2.LyricsList
import player.phonograph.model.lyrics2.LyricsLoader
import player.phonograph.notification.ErrorNotification
import java.io.File

class PlayerFragmentViewModel : ViewModel() {
    val backgroundCoroutine: CoroutineScope by lazy { CoroutineScope(Dispatchers.IO) }

    var currentSong: Song = Song.EMPTY_SONG
        set(value) {
            field = value
            loadLyrics(value)
        }
    var lyricsList: LyricsList? = null
        private set
    var currentLyrics: AbsLyrics? = null
        private set
    fun forceReplaceLyrics(lyrics: AbsLyrics) {
        currentLyrics = lyrics
    }

    private var loadLyricsJob: Job? = null
    private fun loadLyrics(song: Song) {
        if (song == Song.EMPTY_SONG) return
        // cancel old song's lyrics after switching
        loadLyricsJob?.cancel()
        currentLyrics = null
        lyricsList = null
        // load new lyrics
        loadLyricsJob = backgroundCoroutine.launch(exceptionHandler) {
            lyricsList = LyricsLoader.loadLyrics(File(song.data), song)
            currentLyrics = lyricsList!!.getAvailableLyrics()
        }
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
