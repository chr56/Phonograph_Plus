/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.tag

import mt.pref.ThemeColor
import mt.util.color.darkenColor
import player.phonograph.R
import player.phonograph.mediastore.SongLoader
import player.phonograph.misc.CreateFileStorageAccessTool
import player.phonograph.misc.ICreateFileStorageAccess
import player.phonograph.model.Song
import player.phonograph.ui.compose.base.ComposeToolbarActivity
import player.phonograph.ui.compose.theme.PhonographTheme
import player.phonograph.util.SongDetailUtil
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

class DetailScreenViewModel(song: Song, defaultColor: Color) :
        TagBrowserScreenViewModel(song, defaultColor) {
    private var _infoTableState: InfoTableState? = null
    override val infoTableState: InfoTableState
        @Synchronized get() {
            if (_infoTableState == null) {
                _infoTableState =
                    InfoTableState(SongDetailUtil.readSong(song), defaultColor)
            }
            return _infoTableState!!
        }

    class Factory(private val song: Song, private val color: Color) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DetailScreenViewModel(song, color) as T
        }
    }
}