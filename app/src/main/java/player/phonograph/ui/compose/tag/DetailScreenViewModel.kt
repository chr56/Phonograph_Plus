/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.tag

import com.vanpra.composematerialdialogs.MaterialDialogState
import player.phonograph.model.Song
import player.phonograph.util.SongDetailUtil
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.app.Activity
import android.content.Context

abstract class AbsDetailScreenViewModel(
    val song: Song,
    val defaultColor: Color
) : ViewModel() {

    abstract val infoTableViewModel: InfoTableViewModel

    var artwork: MutableState<SongDetailUtil.BitmapPaletteWrapper?> = mutableStateOf(null)
    var artworkLoaded = mutableStateOf(false)

    fun loadArtwork(context: Context, onFinished: () -> Unit) {
        artwork = SongDetailUtil.loadArtwork(context, song) {
            artworkLoaded.value = true
            onFinished()
            val paletteColor = artwork.value?.paletteColor
            if (paletteColor != null) {
                infoTableViewModel.updateTitleColor(Color(paletteColor))
            }
        }
    }

    fun saveArtwork(activity: Context) {
        val wrapper = artwork.value ?: return
        val fileName = song.data.substringAfterLast('/').substringBeforeLast('.')
        SongDetailUtil.saveArtwork(viewModelScope, activity, wrapper, fileName)
    }

    val coverImageDetailDialogState = MaterialDialogState(false)
}

class DetailScreenViewModel(song: Song, defaultColor: Color) :
        AbsDetailScreenViewModel(song, defaultColor) {

    private var _infoTableViewModel: InfoTableViewModel? = null
    override val infoTableViewModel: InfoTableViewModel
        @Synchronized get() {
            if (_infoTableViewModel == null) {
                _infoTableViewModel =
                    InfoTableViewModel(SongDetailUtil.readSong(song), defaultColor)
            }
            return _infoTableViewModel!!
        }

    class Factory(private val song: Song, private val color: Color) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DetailScreenViewModel(song, color) as T
        }
    }
}

class TagEditorScreenViewModel(song: Song, defaultColor: Color) :
        AbsDetailScreenViewModel(song, defaultColor) {
    private var _infoTableViewModel: EditableInfoTableViewModel? = null
    override val infoTableViewModel: EditableInfoTableViewModel
        @Synchronized get() {
            if (_infoTableViewModel == null) {
                _infoTableViewModel =
                    EditableInfoTableViewModel(SongDetailUtil.readSong(song), defaultColor)
            }
            return _infoTableViewModel!!
        }

    val saveConfirmationDialogState = MaterialDialogState(false)
    val exitWithoutSavingDialogState = MaterialDialogState(false)
    fun requestExit(activity: Activity) {
        activity.finish()
    }

    class Factory(private val song: Song, private val color: Color) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TagEditorScreenViewModel(song, color) as T
        }
    }
}