/*
 *  Copyright (c) 2023 chr_56
 */

package player.phonograph.ui.activities

import player.phonograph.App
import player.phonograph.R
import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Song
import player.phonograph.model.playlist.Playlist
import player.phonograph.repo.mediastore.loaders.AlbumLoader
import player.phonograph.repo.mediastore.loaders.ArtistLoader
import player.phonograph.repo.mediastore.loaders.PlaylistLoader
import player.phonograph.repo.mediastore.loaders.SongLoader
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SearchActivityViewModel : ViewModel() {

    private var _query: MutableStateFlow<String> = MutableStateFlow("")
    val query get() = _query.asStateFlow()
    fun query(context: Context, query: String) {
        _query.value = query
        search(context, query)
    }

    private val _results: MutableStateFlow<List<Any>> = MutableStateFlow(emptyList())
    val results get() = _results.asStateFlow()

    private var _songs: MutableStateFlow<List<Song>> = MutableStateFlow(emptyList())
    val songs get() = _songs.asStateFlow()
    private var _artists: MutableStateFlow<List<Artist>> = MutableStateFlow(emptyList())
    val artists get() = _artists.asStateFlow()
    private var _albums: MutableStateFlow<List<Album>> = MutableStateFlow(emptyList())
    val albums get() = _albums.asStateFlow()
    private var _playlists: MutableStateFlow<List<Playlist>> = MutableStateFlow(emptyList())
    val playlists get() = _playlists.asStateFlow()
    private var _songsInQueue: MutableStateFlow<List<SearchResultAdapter.PlayingQueueSong>> =
        MutableStateFlow(emptyList())
    val songsInQueue get() = _songsInQueue.asStateFlow()

    private fun search(context: Context, query: String) {
        if (query.isNotBlank()) {
            viewModelScope.launch(Dispatchers.IO) {

                val dataset: MutableList<Any> = mutableListOf()

                _songs.value = SongLoader.searchByTitle(context, query)
                _artists.value = ArtistLoader.searchByName(context, query)
                _albums.value = AlbumLoader.searchByName(context, query)
                _playlists.value = PlaylistLoader.searchByName(context, query)
                _songsInQueue.value =
                    App.instance.queueManager.playingQueue.mapIndexedNotNull { index, song ->
                        if (song.title.contains(query, true)) {
                            SearchResultAdapter.PlayingQueueSong(song, index)
                        } else {
                            null
                        }
                    }

                if (songs.value.isNotEmpty()) {
                    dataset.add(context.resources.getString(R.string.songs))
                    dataset.addAll(songs.value)
                }
                if (artists.value.isNotEmpty()) {
                    dataset.add(context.resources.getString(R.string.artists))
                    dataset.addAll(artists.value)
                }
                if (albums.value.isNotEmpty()) {
                    dataset.add(context.resources.getString(R.string.albums))
                    dataset.addAll(albums.value)
                }
                if (playlists.value.isNotEmpty()) {
                    dataset.add(context.getString(R.string.playlists))
                    dataset.addAll(playlists.value)
                }
                if (songsInQueue.value.isNotEmpty()) {
                    dataset.add(context.getString(R.string.label_playing_queue))
                    dataset.addAll(songsInQueue.value)
                }

                _results.value = dataset
            }
        } else {
            _results.value = emptyList()
        }
    }

    fun refresh(context: Context) {
        search(context, query.value)
    }
}