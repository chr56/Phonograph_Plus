/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.modules.player

import lib.storage.extension.getBasePath
import org.koin.core.context.GlobalContext
import player.phonograph.mechanism.lyrics.LyricsLoader
import player.phonograph.model.Song
import player.phonograph.model.lyrics.AbsLyrics
import player.phonograph.model.lyrics.LyricsInfo
import player.phonograph.service.queue.QueueManager
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
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

    private var _lyricsInfo: MutableStateFlow<LyricsInfo?> = MutableStateFlow(null)
    val lyricsInfo get() = _lyricsInfo.asStateFlow()

    val hasLyrics get() = !_lyricsInfo.value.isNullOrEmpty()

    fun forceReplaceLyrics(lyrics: AbsLyrics) {
        viewModelScope.launch {
            val new = _lyricsInfo.value?.createWithActivated(lyrics)
            if (new != null) _lyricsInfo.emit(new)
        }
    }

    private var loadLyricsJob: Job? = null
    fun loadLyricsFor(context: Context, song: Song) {
        // cancel old song's lyrics after switching
        loadLyricsJob?.cancel()
        // load new lyrics
        loadLyricsJob = viewModelScope.launch {
            val enableLyrics = Setting(context)[Keys.enableLyrics].flowData()
            if (enableLyrics) {
                _lyricsInfo.emit(
                    if (song != Song.EMPTY_SONG) {
                        LyricsLoader.loadLyrics(File(song.data), song)
                    } else {
                        null
                    }
                )
            }
        }
    }

    suspend fun loadLyricsFrom(context: Context, uri: Uri?) {
        if (uri != null) {
            val lyrics = LyricsLoader.parseFromUri(context, uri)
            if (lyrics != null) {
                delay(600)
                val old = _lyricsInfo.value
                val new = if (old != null) {
                    old.createWithAppended(lyrics).createWithActivated(lyrics)
                } else {
                    val song = GlobalContext.get().get<QueueManager>().currentSong
                    LyricsInfo(song, arrayListOf(lyrics), 0)
                }
                _lyricsInfo.emit(new)
            } else {
                coroutineToast(context, "${uri.getBasePath(context)} is not a validated lyrics")
            }
        }
    }

    // for LyricsDialog
    val requireLyricsFollowing = MutableStateFlow(false)
}