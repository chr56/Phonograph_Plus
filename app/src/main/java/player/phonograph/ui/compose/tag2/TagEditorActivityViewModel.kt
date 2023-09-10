/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.tag2

import player.phonograph.model.Song
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.updateAndGet

class TagEditorActivityViewModel : ViewModel() {
    private val _song: MutableStateFlow<Song> = MutableStateFlow(Song.EMPTY_SONG)
    val song get() = _song.asStateFlow()
    fun updateSong(song: Song) {
        readSongInfo(_song.updateAndGet { song })
    }

    private fun readSongInfo(song: Song) {

    }

}