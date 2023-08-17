/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.tag

import lib.phonograph.misc.CreateFileStorageAccessTool
import lib.phonograph.misc.ICreateFileStorageAccess
import mt.pref.ThemeColor
import mt.util.color.darkenColor
import player.phonograph.R
import player.phonograph.mechanism.tag.loadSongInfo
import player.phonograph.model.Song
import player.phonograph.repo.mediastore.loaders.SongLoader
import player.phonograph.ui.compose.base.ComposeToolbarActivity
import player.phonograph.ui.compose.theme.PhonographTheme
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.coroutines.launch

class DetailActivity : ComposeToolbarActivity(), ICreateFileStorageAccess {

    private lateinit var song: Song
    val model: DetailScreenViewModel
            by viewModels { DetailScreenViewModel.Factory(song, Color(ThemeColor.primaryColor(this))) }


    override val createFileStorageAccessTool: CreateFileStorageAccessTool =
        CreateFileStorageAccessTool()

    override fun onCreate(savedInstanceState: Bundle?) {
        createFileStorageAccessTool.register(lifecycle, activityResultRegistry)
        song = parseIntent(this, intent)
        super.onCreate(savedInstanceState)
        setupObservers()
        model.loadArtwork(this)
    }

    private fun setupObservers() {
        model.viewModelScope.launch {
            model.artwork.collect {
                val newColor = it?.paletteColor ?: primaryColor
                updateBarsColor(darkenColor(newColor))
            }
        }
    }

    @Composable
    override fun SetUpContent() {
        PhonographTheme {
            TagBrowserScreen(model, this)
        }
    }

    override val title: String get() = getString(R.string.label_details)

    companion object {
        private fun parseIntent(context: Context, intent: Intent): Song =
            SongLoader.id(context, intent.extras?.getLong(SONG_ID) ?: -1)

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

class DetailScreenViewModel(song: Song, defaultColor: Color) :
        TagBrowserScreenViewModel(song, defaultColor) {
    private var _audioDetailState: AudioDetailState? = null
    override val audioDetailState: AudioDetailState
        get() {
            if (_audioDetailState == null) {
                _audioDetailState =
                    AudioDetailState(loadSongInfo(song), defaultColor, false)
            }
            return _audioDetailState!!
        }

    class Factory(private val song: Song, private val color: Color) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DetailScreenViewModel(song, color) as T
        }
    }
}