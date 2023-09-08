/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.tag

import lib.phonograph.misc.CreateFileStorageAccessTool
import lib.phonograph.misc.ICreateFileStorageAccess
import mt.pref.ThemeColor
import player.phonograph.R
import player.phonograph.mechanism.tag.loadSongInfo
import player.phonograph.model.Song
import player.phonograph.repo.mediastore.loaders.SongLoader
import player.phonograph.ui.compose.base.ComposeThemeActivity
import player.phonograph.ui.compose.theme.PhonographTheme
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DetailActivity : ComposeThemeActivity(), ICreateFileStorageAccess {

    private lateinit var song: Song
    val model: DetailScreenViewModel
            by viewModels { DetailScreenViewModel.Factory(song, Color(ThemeColor.primaryColor(this))) }


    override val createFileStorageAccessTool: CreateFileStorageAccessTool =
        CreateFileStorageAccessTool()

    override fun onCreate(savedInstanceState: Bundle?) {
        createFileStorageAccessTool.register(lifecycle, activityResultRegistry)
        song = parseIntent(this, intent)
        super.onCreate(savedInstanceState)

        setContent {
            val highlightColor by primaryColor.collectAsState()
            PhonographTheme(highlightColor) {
                val scaffoldState = rememberScaffoldState()
                Scaffold(
                    Modifier.statusBarsPadding(),
                    scaffoldState = scaffoldState,
                    topBar = {
                        TopAppBar(
                            title = { Text(stringResource(R.string.label_details)) },
                            navigationIcon = {
                                Box(Modifier.padding(16.dp)) {
                                    Icon(
                                        Icons.Default.ArrowBack, null,
                                        Modifier.clickable {
                                            onBackPressedDispatcher.onBackPressed()
                                        }
                                    )
                                }
                            },
                            backgroundColor = highlightColor
                        )
                    }
                ) {
                    Box(Modifier.padding(it)) {
                        TagBrowserScreen(model)
                    }
                }

            }

        }
        setupObservers()
        model.loadAudioDetail(this)
    }

    private fun setupObservers() {
        model.viewModelScope.launch {
            model.artwork.collect {
                val newColor = it?.paletteColor
                if (newColor != null) {
                    primaryColor.value = Color(newColor)
                }
            }
        }
    }

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

    private var _audioDetail: MutableStateFlow<AudioDetailState?> = MutableStateFlow(null)
    override val audioDetail: StateFlow<AudioDetailState?> get() = _audioDetail.asStateFlow()

    override fun loadAudioDetail(context: Context) {
        viewModelScope.launch {
            _audioDetail.emit(
                AudioDetailState(loadSongInfo(song), defaultColor, false)
            )
            loadArtwork(context, song)
        }
    }

    class Factory(private val song: Song, private val color: Color) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DetailScreenViewModel(song, color) as T
        }
    }
}