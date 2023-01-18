/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.tag

import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.MaterialDialogState
import com.vanpra.composematerialdialogs.title
import mt.pref.ThemeColor
import mt.util.color.darkenColor
import player.phonograph.R
import player.phonograph.mediastore.SongLoader
import player.phonograph.misc.CreateFileStorageAccessTool
import player.phonograph.misc.ICreateFileStorageAccess
import player.phonograph.model.Song
import player.phonograph.ui.compose.ColorTools.makeSureContrastWith
import player.phonograph.ui.compose.base.ComposeToolbarActivity
import player.phonograph.ui.compose.components.CoverImage
import player.phonograph.ui.compose.theme.PhonographTheme
import player.phonograph.util.SongDetailUtil
import player.phonograph.util.SongDetailUtil.loadArtwork
import player.phonograph.util.SongDetailUtil.readSong
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.content.Intent
import android.os.Bundle

class DetailActivity : ComposeToolbarActivity(), ICreateFileStorageAccess {

    private lateinit var song: Song
    val model: DetailModel
            by viewModels { DetailModel.Factory(song, Color(ThemeColor.primaryColor(this))) }


    override val createFileStorageAccessTool: CreateFileStorageAccessTool =
        CreateFileStorageAccessTool()

    override fun onCreate(savedInstanceState: Bundle?) {
        createFileStorageAccessTool.register(lifecycle, activityResultRegistry)
        song = parseIntent(this, intent)
        super.onCreate(savedInstanceState)
        model.loadArtwork(this) { updateBarsColor() }
    }

    @Composable
    override fun SetUpContent() {
        PhonographTheme {
            DetailActivityContent(model, this)
        }
    }

    override val title: String get() = getString(R.string.label_details)

    private fun updateBarsColor() {
        model.artwork.value?.paletteColor?.let { color ->
            if (color != 0) {
                val colorInt = darkenColor(color)
                appbarColor.value = Color(colorInt)
                window.statusBarColor = darkenColor(colorInt)
                if (ThemeColor.coloredNavigationBar(this)) {
                    window.navigationBarColor = darkenColor(colorInt)
                }
            }
        }
    }


    companion object {
        private fun parseIntent(context: Context, intent: Intent): Song =
            SongLoader.getSong(context, intent.extras?.getLong(SONG_ID) ?: -1)

        private const val SONG_ID = "SONG_ID"

        fun launch(context: Context, songId: Long) {
            context.startActivity(
                Intent(context.applicationContext, DetailActivity::class.java).apply {
                    putExtra(SONG_ID, songId)
                }
            )
        }
    }
}

class DetailModel(
    val song: Song,
    val defaultColor: Color
) : ViewModel() {

    val infoTableViewModel: InfoTableViewModel = createInfoTableViewModel(song, defaultColor)

    private fun createInfoTableViewModel(song: Song, defaultColor: Color) =
        InfoTableViewModel(readSong(song), defaultColor)


    var artwork: MutableState<SongDetailUtil.BitmapPaletteWrapper?> = mutableStateOf(null)
    var artworkLoaded = mutableStateOf(false)

    fun loadArtwork(context: Context, onFinished: () -> Unit) {
        artwork = loadArtwork(context, song) {
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

    class Factory(private val song: Song, private val color: Color) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DetailModel(song, color) as T
        }
    }
}

@Composable
internal fun DetailActivityContent(viewModel: DetailModel, context: Context?) {
    val wrapper by remember { viewModel.artwork }
    val paletteColor =
        makeSureContrastWith(MaterialTheme.colors.surface) {
            if (wrapper != null) {
                Color(wrapper!!.paletteColor)
            } else {
                MaterialTheme.colors.primaryVariant
            }
        }

    Column(
        modifier = Modifier
            .verticalScroll(state = rememberScrollState())
            .fillMaxSize()
    ) {
        if (viewModel.artworkLoaded.value) {
            CoverImage(
                bitmap = wrapper!!.bitmap,
                backgroundColor = paletteColor,
                modifier = Modifier.clickable {
                    viewModel.coverImageDetailDialogState.show()
                }
            )
        }
        InfoTable(viewModel.infoTableViewModel)
    }
    CoverImageDetailDialog(
        state = viewModel.coverImageDetailDialogState,
        artwork = viewModel.artwork.value,
        onSave = { viewModel.saveArtwork(context!!) }
    )
}

@Composable
internal fun CoverImageDetailDialog(
    state: MaterialDialogState,
    artwork: SongDetailUtil.BitmapPaletteWrapper?,
    onSave: () -> Unit
) = MaterialDialog(
    dialogState = state,
    buttons = {
        positiveButton(res = android.R.string.ok) { state.hide() }
    }
) {
    title(res = R.string.label_details)
    Column(
        modifier = Modifier
            .padding(bottom = 28.dp, start = 24.dp, end = 24.dp)
            .wrapContentWidth()
    ) {
        if (artwork != null) {
            Text(
                text = stringResource(R.string.save),
                color = MaterialTheme.colors.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .requiredHeight(32.dp)
                    .clickable { onSave() },
                textAlign = TextAlign.Start
            )
        }
    }
}