/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.modules.player

import coil.request.Disposable
import coil.request.Parameters
import coil.target.Target
import org.koin.core.context.GlobalContext
import player.phonograph.R
import player.phonograph.coil.PARAMETERS_KEY_PALETTE
import player.phonograph.coil.PARAMETERS_KEY_QUICK_CACHE
import player.phonograph.coil.loadImage
import player.phonograph.coil.palette.PaletteColorTarget
import player.phonograph.mechanism.IFavorite
import player.phonograph.model.Song
import player.phonograph.service.MusicPlayerRemote
import player.phonograph.util.text.buildInfoString
import player.phonograph.util.text.readableDuration
import player.phonograph.util.theme.themeFooterColor
import androidx.annotation.ColorInt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlayerFragmentViewModel : ViewModel() {

    private val _currentSong: MutableStateFlow<Song?> = MutableStateFlow(null)
    val currentSong get() = _currentSong.asStateFlow()

    fun updateCurrentSong(context: Context, song: Song?) {
        viewModelScope.launch {
            _currentSong.emit(song)
            updateFavoriteState(context, song)
        }
    }

    private var _favoriteState: MutableStateFlow<Pair<Song?, Boolean>> = MutableStateFlow(null to false)
    val favoriteState get() = _favoriteState.asStateFlow()

    private var loadFavoriteStateJob: Job? = null
    fun updateFavoriteState(context: Context, song: Song?) {
        loadFavoriteStateJob?.cancel()
        if (song != null && song.id > 0) {
            loadFavoriteStateJob = viewModelScope.launch {
                _favoriteState.emit(song to favorite.isFavorite(context, song))
            }
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
            readableDuration(duration)
        )
    }


    //region Image & PaletteColor

    private val _paletteColor: MutableStateFlow<Int> = MutableStateFlow(Color.GRAY)
    val paletteColor get() = _paletteColor.asStateFlow()

    private var disposable: Disposable? = null
    fun refreshPaletteColor(context: Context, song: Song) {
        disposable?.dispose()
        disposable = loadImage(context)
            .from(song)
            .parameters(
                Parameters.Builder()
                    .set(PARAMETERS_KEY_PALETTE, true)
                    .set(PARAMETERS_KEY_QUICK_CACHE, true)
                    .build()
            )
            .into(
                PaletteColorTarget(
                    start = { _, color ->
                        _paletteColor.value = color
                    },
                    success = { _, color ->
                        _paletteColor.value = color
                    },
                    defaultColor = themeFooterColor(context),
                )
            )
            .enqueue()
    }

    fun refreshPaletteColor(@ColorInt color: Int) {
        viewModelScope.launch {
            _paletteColor.emit(color)
        }
    }

    //endregion

}
