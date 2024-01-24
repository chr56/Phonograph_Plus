/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.fragments.player

import org.koin.core.context.GlobalContext
import player.phonograph.App
import player.phonograph.R
import player.phonograph.coil.PreloadImageCache
import player.phonograph.mechanism.IFavorite
import player.phonograph.model.Song
import player.phonograph.model.buildInfoString
import player.phonograph.model.getReadableDurationString
import player.phonograph.service.MusicPlayerRemote
import androidx.annotation.ColorInt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
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
            _favoriteState.emit(song to favorite.isFavorite(context ?: App.instance, song))
        }
    }

    val favorite: IFavorite by GlobalContext.get().inject()

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

    private val _paletteColor: MutableStateFlow<Int> = MutableStateFlow(Color.GRAY)
    val paletteColor get() = _paletteColor.asStateFlow()

    private var fetcherDeferred: Deferred<Int>? = null
    fun refreshPaletteColor(context: Context, song: Song) {
        fetcherDeferred?.cancel()
        fetcherDeferred = viewModelScope.async {
            preloadImageCache.fetchPaletteColor(context, song = song)
        }
        viewModelScope.launch {
            val current = fetcherDeferred
            if (current != null && !current.isCancelled) {
                val color = current.await()
                if (current == fetcherDeferred) {
                    _paletteColor.emit(color)
                }
            }
        }
    }

    fun refreshPaletteColor(@ColorInt color: Int) {
        viewModelScope.launch {
            _paletteColor.emit(color)
        }
    }

    private val preloadImageCache: PreloadImageCache = PreloadImageCache(6)

    suspend fun fetchBitmap(context: Context, song: Song): Bitmap = preloadImageCache.fetchBitmap(context, song)
    //endregion

}
