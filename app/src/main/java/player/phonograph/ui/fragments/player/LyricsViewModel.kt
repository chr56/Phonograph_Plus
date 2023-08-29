/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.fragments.player

import lib.phonograph.storage.getBasePath
import player.phonograph.mechanism.lyrics.LyricsLoader
import player.phonograph.model.Song
import player.phonograph.model.lyrics.AbsLyrics
import player.phonograph.model.lyrics.LyricsInfo
import player.phonograph.util.coroutineToast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class LyricsViewModel : ViewModel() {

    private var _lyricsInfo: MutableStateFlow<LyricsInfo> = MutableStateFlow(LyricsInfo.EMPTY)
    val lyricsInfo get() = _lyricsInfo.asStateFlow()

    fun forceReplaceLyrics(lyrics: AbsLyrics) {
        viewModelScope.launch {
            val new = _lyricsInfo.value.replaceActivated(lyrics)
            if (new != null) _lyricsInfo.emit(new)
        }
    }

    private var loadLyricsJob: Job? = null
    fun loadLyrics(song: Song) {
        // cancel old song's lyrics after switching
        loadLyricsJob?.cancel()
        // load new lyrics
        loadLyricsJob = viewModelScope.launch {
            if (song == Song.EMPTY_SONG) {
                _lyricsInfo.emit(LyricsInfo.EMPTY)
            } else {
                val newLyrics = LyricsLoader.loadLyrics(File(song.data), song)
                _lyricsInfo.emit(newLyrics)
            }
        }
    }

    suspend fun insert(context: Context, uri: Uri?) {
        if (uri != null) {
            val lyrics = LyricsLoader.parseFromUri(context, uri)
            if (lyrics != null) {
                delay(600)
                val info = _lyricsInfo.value.createAmended(lyrics).replaceActivated(lyrics)!!
                _lyricsInfo.emit(info)
            } else {
                coroutineToast(context, "${uri.getBasePath(context)} is not a validated lyrics")
            }
        }
    }

    // for LyricsDialog
    val requireLyricsFollowing = MutableStateFlow(false)
}