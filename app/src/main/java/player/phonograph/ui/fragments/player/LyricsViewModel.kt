/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.fragments.player

import player.phonograph.mediastore.LyricsLoader
import player.phonograph.model.Song
import player.phonograph.model.lyrics.AbsLyrics
import player.phonograph.model.lyrics.LyricsInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class LyricsViewModel : ViewModel() {
    private var _lyricsList: MutableStateFlow<LyricsInfo> = MutableStateFlow(LyricsInfo.EMPTY)
    val lyricsList get() = _lyricsList.asStateFlow()

    fun forceReplaceLyrics(lyrics: AbsLyrics) {
        viewModelScope.launch {
            val new = _lyricsList.value.replaceActivated(lyrics)
            if (new != null) _lyricsList.emit(new)
        }
    }

    private var loadLyricsJob: Job? = null
    fun loadLyrics(song: Song) {
        // cancel old song's lyrics after switching
        loadLyricsJob?.cancel()
        // _lyricsList.value = LyricsInfo.EMPTY
        // load new lyrics
        loadLyricsJob = viewModelScope.launch {
            if (song == Song.EMPTY_SONG) return@launch
            val newLyrics = LyricsLoader.loadLyrics(File(song.data), song)
            _lyricsList.emit(newLyrics)
        }
    }
}