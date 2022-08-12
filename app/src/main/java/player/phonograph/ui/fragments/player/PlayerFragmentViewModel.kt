/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.fragments.player

import android.content.Context
import android.graphics.Color
import android.view.MenuItem
import androidx.lifecycle.ViewModel
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

class PlayerFragmentViewModel : ViewModel() {
    val backgroundCoroutine: CoroutineScope by lazy { CoroutineScope(Dispatchers.IO) }

    var currentSong: Song = Song.EMPTY_SONG
        set(value) {
            field = value
            loadLyrics(value)
        }
    var lyricsList: LyricsList? = null
        private set
    var currentLyrics: AbsLyrics? = null
        private set
    fun forceReplaceLyrics(lyrics: AbsLyrics) {
        currentLyrics = lyrics
    }

    private var loadLyricsJob: Job? = null
    private fun loadLyrics(song: Song) {
        if (song == Song.EMPTY_SONG) return
        // cancel old song's lyrics after switching
        loadLyricsJob?.cancel()
        currentLyrics = null
        lyricsList = null
        // load new lyrics
        loadLyricsJob = backgroundCoroutine.launch(exceptionHandler) {
            lyricsList = LyricsLoader.loadLyrics(File(song.data), song)
            currentLyrics = lyricsList!!.getAvailableLyrics()
        }
    }

    var favoriteMenuItem: MenuItem? = null
    fun updateFavoriteState(context: Context, song: Song) {
        backgroundCoroutine.launch(exceptionHandler) {
            val state = isFavorite(context, song)
            favoriteMenuItem?.let {
                withContext(Dispatchers.Main) {
                    updateFavoriteIcon(context, state)
                }
            }
        }
    }

    fun updateFavoriteIcon(context: Context, isFavorite: Boolean) =
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
        FavoriteUtil.toggleFavorite(context, song)
        favoriteAnimateCallback?.invoke(isFavorite(context, song))
    }

    private val exceptionHandler by lazy {
        CoroutineExceptionHandler { _, throwable ->
            ErrorNotification.postErrorNotification(throwable)
        }
    }
}
