/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.fragments.player

import player.phonograph.App
import player.phonograph.R
import player.phonograph.coil.loadImage
import player.phonograph.coil.target.PaletteBitmap
import player.phonograph.mechanism.Favorite.isFavorite
import player.phonograph.model.Song
import player.phonograph.model.buildInfoString
import player.phonograph.model.getReadableDurationString
import player.phonograph.model.lyrics.LrcLyrics
import player.phonograph.service.MusicPlayerRemote
import androidx.annotation.ColorInt
import androidx.collection.LruCache
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlayerFragmentViewModel : ViewModel() {

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
    val favoriteState get() = _favoriteState.asStateFlow()

    private var loadFavoriteStateJob: Job? = null
    fun updateFavoriteState(song: Song, context: Context?) {
        loadFavoriteStateJob?.cancel()
        loadFavoriteStateJob = viewModelScope.launch {
            if (song == Song.EMPTY_SONG) return@launch
            _favoriteState.emit(song to isFavorite(context ?: App.instance, song))
        }
    }

    private val _lyrics: MutableStateFlow<LrcLyrics?> = MutableStateFlow(null)
    val lyrics get() = _lyrics.asStateFlow()

    fun updateLrcLyrics(lyrics: LrcLyrics?) {
        viewModelScope.launch {
            _lyrics.emit(lyrics)
        }
    }

    private var _shownToolbar: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val showToolbar get() = _shownToolbar.asStateFlow()

    fun toggleToolbar() =
        _shownToolbar.tryEmit(
            !_shownToolbar.value
        )

    fun upNextAndQueueTime(resources: Resources): String {
        val duration = MusicPlayerRemote.getQueueDurationMillis(MusicPlayerRemote.position)
        return buildInfoString(
            resources.getString(R.string.up_next),
            getReadableDurationString(duration)
        )
    }


    //region Image & PaletteColor

    private val _paletteColor: MutableStateFlow<Int> = MutableStateFlow(0)
    val paletteColor get() = _paletteColor.asStateFlow()

    fun refreshPaletteColor(context: Context, song: Song?) {
        viewModelScope.launch {
            val color = fetchPaletteColor(context, song = song ?: currentSong.value)
            _paletteColor.emit(color)
        }
    }

    private val imageCache: LruCache<Song, PaletteBitmap> = LruCache(6)

    private fun putCache(song: Song, bitmap: Bitmap, color: Int) {
        imageCache.put(song, PaletteBitmap(bitmap, color))
    }

    private fun getPaletteColorFromCache(song: Song) = imageCache[song]?.paletteColor
    private fun getImageFromCache(song: Song) = imageCache[song]?.bitmap

    private suspend fun fetchPaletteColor(context: Context, song: Song): Int {
        val cached = getPaletteColorFromCache(song)
        return if (cached == null) {
            val loaded = loadImage(context, song)
            putCache(song, loaded.bitmap, loaded.paletteColor)
            loaded.paletteColor
        } else {
            cached
        }
    }

    suspend fun fetchBitmap(context: Context, song: Song): Bitmap {
        val cached = getImageFromCache(song)
        return if (cached == null) {
            val loaded = loadImage(context, song)
            putCache(song, loaded.bitmap, loaded.paletteColor)
            loaded.bitmap
        } else {
            cached
        }
    }
    //endregion

}
