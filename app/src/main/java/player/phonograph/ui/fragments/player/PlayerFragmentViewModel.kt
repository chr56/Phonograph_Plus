/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.fragments.player

import android.app.Application
import android.content.Context
import android.graphics.Color
import android.view.MenuItem
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import java.io.File
import kotlinx.coroutines.*
import player.phonograph.R
import player.phonograph.model.Song
import player.phonograph.model.lyrics.AbsLyrics
import player.phonograph.model.lyrics.LyricsList
import player.phonograph.model.lyrics.LyricsLoader
import player.phonograph.notification.ErrorNotification
import player.phonograph.util.FavoriteUtil
import player.phonograph.util.FavoriteUtil.isFavorite
import player.phonograph.util.ImageUtil.getTintedDrawable
import util.mddesign.util.ToolbarColorUtil

class PlayerFragmentViewModel(application: Application) : AndroidViewModel(application) {

    val context get() = getApplication<Application>()

    val backgroundCoroutine: CoroutineScope by lazy { CoroutineScope(Dispatchers.IO) }

    var currentSong: Song = Song.EMPTY_SONG
        set(value) {
            field = value
            loadLyrics(value)
            updateFavoriteState(value)
        }

    var lyricsMenuItem: MenuItem? = null

    var lyricsList: LyricsList? = null
        private set
    var currentLyrics: AbsLyrics? = null
        private set
    fun forceReplaceLyrics(lyrics: AbsLyrics) {
        currentLyrics = lyrics
    }

    var onLyricsReadyCallback: ((AbsLyrics?) -> Unit)? = null

    private var loadLyricsJob: Job? = null
    fun loadLyrics(song: Song) {
        // cancel old song's lyrics after switching
        loadLyricsJob?.cancel()
        currentLyrics = null
        lyricsList = null
        lyricsMenuItem?.isVisible = false
        // load new lyrics
        loadLyricsJob = backgroundCoroutine.launch(exceptionHandler) {
            if (song == Song.EMPTY_SONG) return@launch
            lyricsList = LyricsLoader.loadLyrics(File(song.data), song)
            currentLyrics = lyricsList!!.getAvailableLyrics()

            // update ui
            onLyricsReadyCallback?.invoke(currentLyrics)
        }
    }

    var favoriteMenuItem: MenuItem? = null

    private var favoriteState: Pair<Song, Boolean> = Song.EMPTY_SONG to false

    private var loadFavoriteStateJob: Job? = null
    fun updateFavoriteState(song: Song) {
        loadFavoriteStateJob?.cancel()
        favoriteState = Song.EMPTY_SONG to false
        loadFavoriteStateJob = backgroundCoroutine.launch(exceptionHandler) {
            if (song == Song.EMPTY_SONG) return@launch
            favoriteState = song to isFavorite(context, song)

            // update ui
            favoriteMenuItem?.let {
                withContext(Dispatchers.Main) {
                    updateFavoriteIcon(favoriteState.second)
                }
            }
        }
    }

    fun updateFavoriteIcon(isFavorite: Boolean) =
        context.run {
            val res = if (isFavorite) R.drawable.ic_favorite_white_24dp else R.drawable.ic_favorite_border_white_24dp
            val color = ToolbarColorUtil.toolbarContentColor(context, Color.TRANSPARENT)
            favoriteMenuItem?.apply {
                icon = getTintedDrawable(res, color)
                title =
                    if (isFavorite) getString(R.string.action_remove_from_favorites)
                    else getString(R.string.action_add_to_favorites)
            }
        }

    var favoriteAnimateCallback: ((Boolean) -> Unit)? = null
    fun toggleFavorite(context: Context, song: Song) {
        val result = FavoriteUtil.toggleFavorite(context, song)
        favoriteAnimateCallback?.invoke(result)
    }

    val exceptionHandler by lazy {
        CoroutineExceptionHandler { _, throwable ->
            ErrorNotification.postErrorNotification(throwable)
        }
    }

    companion object {
        fun from(application: Application): ViewModelProvider.Factory {
            return ViewModelProvider.AndroidViewModelFactory(application)
        }
    }
}
