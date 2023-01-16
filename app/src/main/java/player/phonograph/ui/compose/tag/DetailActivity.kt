/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.tag

import mt.pref.ThemeColor
import mt.util.color.darkenColor
import player.phonograph.R
import player.phonograph.mediastore.SongLoader
import player.phonograph.model.Song
import player.phonograph.ui.compose.ColorTools.makeSureContrastWith
import player.phonograph.ui.compose.base.ComposeToolbarActivity
import player.phonograph.ui.compose.theme.PhonographTheme
import player.phonograph.util.SongDetailUtil
import player.phonograph.util.SongDetailUtil.loadArtwork
import player.phonograph.util.SongDetailUtil.readSong
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle

class DetailActivity : ComposeToolbarActivity() {

    private lateinit var song: Song
    val model: DetailModel
            by viewModels { DetailModel.Factory(song, Color(ThemeColor.primaryColor(this))) }

    override fun onCreate(savedInstanceState: Bundle?) {
        song = parseIntent(this, intent)
        super.onCreate(savedInstanceState)
        model.loadArtwork(this) { updateBarsColor() }
    }

    @Composable
    override fun SetUpContent() {
        PhonographTheme {
            DetailActivityContent(model)
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
    var isDefaultArtwork = mutableStateOf(true)

    fun loadArtwork(context: Context, onFinished: () -> Unit) {
        artwork = loadArtwork(context, song) {
            onFinished()
            isDefaultArtwork.value = false
            val paletteColor = artwork.value?.paletteColor
            if (paletteColor != null) {
                infoTableViewModel.updateTitleColor(Color(paletteColor))
            }
        }
    }

    class Factory(private val song: Song, private val color: Color) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DetailModel(song, color) as T
        }
    }
}

@Composable
internal fun DetailActivityContent(viewModel: DetailModel) {
    val wrapper by remember { viewModel.artwork }
    val isDefaultArtwork by remember { viewModel.isDefaultArtwork }
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
        CoverImage(
            bitmap = wrapper!!.bitmap,
            backgroundColor = paletteColor,
            showCover = !isDefaultArtwork
        )
        InfoTable(viewModel.infoTableViewModel)
    }
}