/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.fragments.player

import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.*
import player.phonograph.model.Song
import player.phonograph.model.lyrics2.AbsLyrics
import player.phonograph.model.lyrics2.LyricsLoader
import player.phonograph.model.lyrics2.LyricsPack
import java.io.File

class PlayerFragmentViewModel : ViewModel() {
    val backgroundCoroutine: CoroutineScope by lazy { CoroutineScope(Dispatchers.IO) }

    private val exceptionHandler by lazy {
        CoroutineExceptionHandler { _, throwable ->
            Log.w("LyricsFetcher", "Exception while fetching lyrics!\n${throwable.message}")
        }
    }

    var lyricsPack: LyricsPack? = null
        private set
    var currentLyrics: AbsLyrics? = null
        private set

    var songLocked: Song? = null
        private set
    var lockLyricForSong: Boolean = false
        private set

    fun lockLyricsWithSong(song: Song) {
        songLocked = song
        lockLyricForSong = true
    }
    fun unlockLyrics() {
        songLocked = null
        lockLyricForSong = false
    }

    private var loadLyricsJob: Job? = null
    fun loadLyrics(song: Song) {
        // cancel old song's lyrics after switching
        loadLyricsJob?.cancel()
        currentLyrics = null
        lyricsPack = null
        // load new lyrics
        loadLyricsJob = backgroundCoroutine.launch(exceptionHandler) {
            lyricsPack = LyricsLoader.loadLyrics(File(song.data), song.title)
            currentLyrics = lyricsPack!!.getLyrics()
        }
    }
}
