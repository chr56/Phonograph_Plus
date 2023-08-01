/*
 *  Copyright (c) 2023 chr_56
 */

package player.phonograph.ui.activities

import player.phonograph.App
import player.phonograph.R
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

    private fun search(context: Context, query: String) {
        if (query.isNotBlank()) {
            viewModelScope.launch(Dispatchers.IO) {

                val dataset: MutableList<Any> = mutableListOf()

                val songs = SongLoader.searchByTitle(context, query)
                val artists = ArtistLoader.searchByName(context, query)
                val albums = AlbumLoader.searchByName(context, query)
                val playlists = PlaylistLoader.searchByName(context, query)
                val songsInQueue =
                    App.instance.queueManager.playingQueue.filter { it.title.contains(query, true) }

                if (songs.isNotEmpty()) {
                    dataset.add(context.resources.getString(R.string.songs))
                    dataset.addAll(songs)
                }
                if (artists.isNotEmpty()) {
                    dataset.add(context.resources.getString(R.string.artists))
                    dataset.addAll(artists)
                }
                if (albums.isNotEmpty()) {
                    dataset.add(context.resources.getString(R.string.albums))
                    dataset.addAll(albums)
                }
                if (playlists.isNotEmpty()) {
                    dataset.add(context.getString(R.string.playlists))
                    dataset.addAll(playlists)
                }
                if (songsInQueue.isNotEmpty()) {
                    dataset.add(context.getString(R.string.label_playing_queue))
                    dataset.addAll(songsInQueue)
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