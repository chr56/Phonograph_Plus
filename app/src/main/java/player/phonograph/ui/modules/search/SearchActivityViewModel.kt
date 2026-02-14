/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.modules.search

import org.koin.core.context.GlobalContext
import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Genre
import player.phonograph.model.QueueSong
import player.phonograph.model.Song
import player.phonograph.model.playlist.Playlist
import player.phonograph.repo.loader.Albums
import player.phonograph.repo.loader.Artists
import player.phonograph.repo.loader.Genres
import player.phonograph.repo.loader.Playlists
import player.phonograph.repo.loader.Songs
import player.phonograph.service.queue.QueueManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SearchActivityViewModel : ViewModel() {

    private var currentType: SearchType = SearchType.SONGS
    fun switch(context: Context, type: SearchType) {
        currentType = type
        search(context, currentType, _query.value)
    }

    private var _query: MutableStateFlow<String> = MutableStateFlow("")
    val query get() = _query.asStateFlow()
    fun query(context: Context, query: String) {
        _query.value = query
        search(context, currentType, query)
    }

    fun refresh(context: Context) {
        search(context, currentType, query.value)
    }

    private var _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs = _songs.asStateFlow()
    private var _artists = MutableStateFlow<List<Artist>>(emptyList())
    val artists = _artists.asStateFlow()
    private var _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums = _albums.asStateFlow()
    private var _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists = _playlists.asStateFlow()
    private var _genres = MutableStateFlow<List<Genre>>(emptyList())
    val genres = _genres.asStateFlow()
    private var _songsInQueue = MutableStateFlow<List<QueueSong>>(emptyList())
    val songsInQueue = _songsInQueue.asStateFlow()

    private var searchJob: Job? = null

    private fun search(context: Context, type: SearchType, query: String) {
        searchJob?.cancel()

        if (query.isNotBlank()) {
            searchJob = viewModelScope.launch(Dispatchers.IO) {
                when (type) {
                    SearchType.SONGS     -> _songs.value = Songs.searchByTitle(context, query)
                    SearchType.ARTISTS   -> _artists.value = Artists.searchByName(context, query)
                    SearchType.ALBUMS    -> _albums.value = Albums.searchByName(context, query)
                    SearchType.PLAYLISTS -> _playlists.value = Playlists.searchByName(context, query)
                    SearchType.GENRES    -> _genres.value = Genres.searchByName(context, query)
                    SearchType.QUEUE     -> {
                        val queueManager: QueueManager = GlobalContext.get().get()
                        _songsInQueue.value = queueManager.playingQueue.mapIndexedNotNull { index, song ->
                            if (song.title.contains(query, true)) QueueSong(song, index) else null
                        }
                    }
                }
            }
        } else {
            _songs.value = emptyList()
            _artists.value = emptyList()
            _albums.value = emptyList()
            _playlists.value = emptyList()
            _genres.value = emptyList()
            _songsInQueue.value = emptyList()
        }
    }

}