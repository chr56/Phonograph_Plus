/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.tag

import com.vanpra.composematerialdialogs.MaterialDialogState
import mt.pref.ThemeColor
import player.phonograph.model.Song
import player.phonograph.util.SongDetailUtil
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

abstract class TagBrowserScreenViewModel(
    val song: Song,
    val defaultColor: Color
) : ViewModel() {

    abstract val infoTableState: InfoTableState

    var artwork: ArtworkStateFlow = MutableStateFlow(null)

    fun loadArtwork(context: Context) = loadArtworkImpl(context, song)

    protected fun loadArtworkImpl(context: Context, what: Any) {
        viewModelScope.launch(Dispatchers.Unconfined) {
            // observe
            artwork.collect { newArtworkState ->
                val paletteColor = newArtworkState?.paletteColor
                infoTableState.updateTitleColor(
                    if (paletteColor != null) {
                        Color(paletteColor)
                    } else {
                        Color(ThemeColor.primaryColor(context))
                    }
                )
            }
        }
        // execute
        SongDetailUtil.loadArtwork(context, artwork, what)
    }

    fun saveArtwork(activity: Context) {
        val wrapper = artwork.value ?: return
        val fileName = fileName(fullPath = song.data)
        SongDetailUtil.saveArtwork(viewModelScope, activity, wrapper, fileName)
    }

    val coverImageDetailDialogState = MaterialDialogState(false)
}

typealias ArtworkStateFlow = MutableStateFlow<SongDetailUtil.BitmapPaletteWrapper?>

private fun fileName(fullPath: String) =
    fullPath.substringAfterLast('/').substringBeforeLast('.')