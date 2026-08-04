/*
 * Copyright (c) 2022-2026 chr_56
 */

package player.phonograph.ui.modules.genre

import player.phonograph.model.Genre
import player.phonograph.model.Song
import player.phonograph.repo.loader.Genres
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GenreDetailActivityViewModel(val genreId: Long) : ViewModel() {

    private val _genre: MutableStateFlow<Genre> = MutableStateFlow(Genre(genreId, null, 0))
    val genre get() = _genre.asStateFlow()

    private val _songs: MutableStateFlow<List<Song>> = MutableStateFlow(emptyList())
    val songs get() = _songs.asStateFlow()

    fun loadDataSet(context: Context) {
        viewModelScope.launch(Dispatchers.IO + SupervisorJob()) {
            _genre.emit(Genres.id(context, genreId) ?: _genre.value)
            _songs.emit(Genres.songs(context, genreId))
        }
    }
}
