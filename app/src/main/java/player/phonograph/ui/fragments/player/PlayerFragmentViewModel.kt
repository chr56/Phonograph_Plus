/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.fragments.player

import player.phonograph.App
import player.phonograph.mechanism.Favorite.isFavorite
import player.phonograph.model.Song
import player.phonograph.util.reportError
import androidx.annotation.ColorInt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlayerFragmentViewModel : ViewModel() {

    val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        reportError(throwable, "PlayerFragment", "")
    }

    private val _currentSong: MutableStateFlow<Song> = MutableStateFlow(Song.EMPTY_SONG)
    val currentSong get() = _currentSong.asStateFlow()

    fun updateCurrentSong(song: Song, context: Context?) {
        viewModelScope.launch {
            _currentSong.emit(song)
            updateFavoriteState(song, context)
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
