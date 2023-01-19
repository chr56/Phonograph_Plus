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
import androidx.lifecycle.viewModelScope
import android.content.Context

abstract class TagBrowserScreenViewModel(
    val song: Song,
    val defaultColor: Color
) : ViewModel() {

    abstract val infoTableState: InfoTableState

    var artwork: ArtworkState = mutableStateOf(null)
    var artworkLoaded = mutableStateOf(false)

    fun loadArtwork(context: Context, onFinished: () -> Unit) =
        loadArtworkImpl(context, song, onFinished)

    protected fun loadArtworkImpl(context: Context, what: Any, onFinished: () -> Unit) {
        artwork = SongDetailUtil.loadArtwork(context, what) {
            artworkLoaded.value = true
            onFinished()
            val paletteColor = paletteColor(artwork)
            if (paletteColor != null) {
                infoTableState.updateTitleColor(Color(paletteColor))
            }
        }
    }

    fun saveArtwork(activity: Context) {
        val wrapper = artwork.value ?: return
        val fileName = fileName(fullPath = song.data)
        SongDetailUtil.saveArtwork(viewModelScope, activity, wrapper, fileName)
    }

    val coverImageDetailDialogState = MaterialDialogState(false)
}

typealias ArtworkState = MutableState<SongDetailUtil.BitmapPaletteWrapper?>

private fun paletteColor(artwork: ArtworkState) = artwork.value?.paletteColor

private fun fileName(fullPath: String) =
    fullPath.substringAfterLast('/').substringBeforeLast('.')