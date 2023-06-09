/*
 *  Copyright (c) 2023 chr_56
 */

package player.phonograph.ui.activities

import player.phonograph.R
import player.phonograph.mediastore.AlbumLoader
import player.phonograph.mediastore.ArtistLoader
import player.phonograph.mediastore.SongLoader
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
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
            viewModelScope.launch {

                val dataset: MutableList<Any> = mutableListOf()

                val songs = async(Dispatchers.IO) { SongLoader.getSongs(context, query) }
                val artists = async(Dispatchers.IO) { ArtistLoader.getArtists(context, query) }
                val albums = async(Dispatchers.IO) { AlbumLoader.getAlbums(context, query) }

                val songList = songs.await()
                if (songList.isNotEmpty()) {
                    dataset.add(context.resources.getString(R.string.songs))
                    dataset.addAll(songList)
                }
                val artistList = artists.await()
                if (songList.isNotEmpty()) {
                    dataset.add(context.resources.getString(R.string.artists))
                    dataset.addAll(artistList)
                }
                val albumList = albums.await()
                if (songList.isNotEmpty()) {
                    dataset.add(context.resources.getString(R.string.albums))
                    dataset.addAll(albumList)
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