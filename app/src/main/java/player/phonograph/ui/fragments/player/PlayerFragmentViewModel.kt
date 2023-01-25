/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.fragments.player

import player.phonograph.App
import player.phonograph.mediastore.LyricsLoader
import player.phonograph.model.Song
import player.phonograph.model.lyrics.AbsLyrics
import player.phonograph.model.lyrics.LyricsList
import player.phonograph.util.FavoriteUtil.isFavorite
import player.phonograph.util.Util.reportError
import androidx.annotation.ColorInt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.view.MenuItem
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class PlayerFragmentViewModel : ViewModel() {

    val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        reportError(throwable, "PlayerFragment", "")
    }

    private val _currentSong: MutableStateFlow<Song> = MutableStateFlow(Song.EMPTY_SONG)
    val currentSong get() = _currentSong.asStateFlow()

    fun updateCurrentSong(song: Song, context: Context?) {
        viewModelScope.launch {
            _currentSong.emit(song)
            loadLyrics(song)
            updateFavoriteState(song, context)
        }
    }

    var lyricsMenuItem: MenuItem? = null

    private var _lyricsList: MutableStateFlow<LyricsList> = MutableStateFlow(LyricsList())
    val lyricsList get() = _lyricsList.asStateFlow()

    private var _currentLyrics: MutableStateFlow<AbsLyrics?> = MutableStateFlow(null)
    val currentLyrics get() = _currentLyrics.asStateFlow()

    fun forceReplaceLyrics(lyrics: AbsLyrics) {
        viewModelScope.launch {
            _currentLyrics.emit(lyrics)
        }
    }

    private var loadLyricsJob: Job? = null
    fun loadLyrics(song: Song) {
        // cancel old song's lyrics after switching
        loadLyricsJob?.cancel()
        _currentLyrics.value = null
        _lyricsList.value = LyricsList()
        lyricsMenuItem?.isVisible = false
        // load new lyrics
        loadLyricsJob = viewModelScope.launch {
            if (song == Song.EMPTY_SONG) return@launch
            val newLyrics = LyricsLoader.loadLyrics(File(song.data), song)
            _lyricsList.emit(newLyrics)
            _currentLyrics.emit(newLyrics.getAvailableLyrics())
        }
    }

    private var _favoriteState: MutableStateFlow<Pair<Song, Boolean>> =
        MutableStateFlow(Song.EMPTY_SONG to false)
    val favoriteState = _favoriteState.asStateFlow()

    private var loadFavoriteStateJob: Job? = null
    fun updateFavoriteState(song: Song, context: Context?) {
        loadFavoriteStateJob?.cancel()
        _favoriteState.value = Song.EMPTY_SONG to false
        loadFavoriteStateJob = viewModelScope.launch(exceptionHandler) {
            if (song == Song.EMPTY_SONG) return@launch
            _favoriteState.emit(song to isFavorite(context ?: App.instance, song))
        }
    }

    private val _paletteColor: MutableStateFlow<Int> = MutableStateFlow(0)
    val paletteColor get() = _paletteColor.asStateFlow()

    fun updatePaletteColor(@ColorInt newColor: Int) {
        viewModelScope.launch {
            _paletteColor.emit(newColor)
        }
    }
}
