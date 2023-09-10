/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.tag

import com.vanpra.composematerialdialogs.MaterialDialogState
import lib.phonograph.misc.CreateFileStorageAccessTool
import lib.phonograph.misc.ICreateFileStorageAccess
import lib.phonograph.misc.IOpenFileStorageAccess
import lib.phonograph.misc.OpenFileStorageAccessTool
import mt.pref.ThemeColor.primaryColor
import player.phonograph.R
import player.phonograph.mechanism.tag.EditAction
import player.phonograph.mechanism.tag.edit.selectNewArtwork
import player.phonograph.mechanism.tag.loadSongInfo
import player.phonograph.model.Song
import player.phonograph.repo.mediastore.loaders.SongLoader
import player.phonograph.ui.compose.base.ComposeThemeActivity
import player.phonograph.ui.compose.theme.PhonographTheme
import player.phonograph.ui.compose.web.IWebSearchRequester
import player.phonograph.ui.compose.web.WebSearchLauncher
import player.phonograph.ui.compose.web.WebSearchTool
import util.phonograph.tagsources.Source
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

class TagEditorActivity :
        ComposeThemeActivity(),
        IWebSearchRequester,
        ICreateFileStorageAccess,
        IOpenFileStorageAccess {

    override val createFileStorageAccessTool: CreateFileStorageAccessTool =
        CreateFileStorageAccessTool()
    override val openFileStorageAccessTool: OpenFileStorageAccessTool =
        OpenFileStorageAccessTool()
    override val webSearchTool: WebSearchTool =
        WebSearchTool()

    private lateinit var song: Song
    private val model: TagEditorScreenViewModel
            by viewModels { TagEditorScreenViewModel.Factory(song, Color(primaryColor(this))) }

    override fun onCreate(savedInstanceState: Bundle?) {
        song = parseIntent(this, intent)
        createFileStorageAccessTool.register(lifecycle, activityResultRegistry)
        openFileStorageAccessTool.register(lifecycle, activityResultRegistry)
        webSearchTool.register(lifecycle, activityResultRegistry)
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
                            title = { Text(stringResource(R.string.action_tag_editor)) },
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
                            actions = {
                                RequestWebSearch()
                                IconButton(onClick = { model.saveConfirmationDialogState.show() }) {
                                    Icon(painterResource(id = R.drawable.ic_save_white_24dp), null)
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

    override fun onBackPressed() {
        if (model.audioDetail.value?.hasEdited != true) {
            finish()
        } else {
            model.exitWithoutSavingDialogState.show()
        }
    }

    companion object {
        private fun parseIntent(context: Context, intent: Intent): Song =
            SongLoader.id(context, intent.extras?.getLong(SONG_ID) ?: -1)

        private const val SONG_ID = "SONG_ID"

        fun launch(context: Context, songId: Long) {
            context.startActivity(
                Intent(context.applicationContext, TagEditorActivity::class.java).apply {
                    putExtra(SONG_ID, songId)
                }
            )
        }
    }

    @Composable
    private fun RequestWebSearch() {
        var state by remember { mutableStateOf(false) }
        IconButton(onClick = { state = !state }) {
            Icon(painterResource(id = R.drawable.ic_search_white_24dp), null)
        }
        DropdownMenu(expanded = state, onDismissRequest = { state = false }) {
            fun search(source: Source) {
                val context = this@TagEditorActivity
                val intent = when (source) {
                    Source.LastFm -> WebSearchLauncher.searchLastFmSong(context, song)
                    Source.MusicBrainz -> WebSearchLauncher.searchMusicBrainzSong(context, song)
                }
                webSearchTool.launch(intent) {
                    Log.v("TagEditor", it.toString()) //todo
                }
            }
            DropdownMenuItem(onClick = { search(Source.MusicBrainz) }
            ) {
                Text(Source.MusicBrainz.name, Modifier.padding(8.dp))
            }
            DropdownMenuItem(onClick = { search(Source.LastFm) }
            ) {
                Text(Source.LastFm.name, Modifier.padding(8.dp))
            }
        }
    }
}

class TagEditorScreenViewModel(song: Song, defaultColor: Color) :
        TagBrowserScreenViewModel(song, defaultColor) {

    private var _audioDetail: MutableStateFlow<AudioDetailState?> = MutableStateFlow(null)
    override val audioDetail: StateFlow<AudioDetailState?> get() = _audioDetail.asStateFlow()

    override fun loadAudioDetail(context: Context) {
        viewModelScope.launch {
            _audioDetail.emit(
                AudioDetailState(loadSongInfo(song), defaultColor, true)
            )
            loadArtwork(context, song)
        }
    }


    val saveConfirmationDialogState = MaterialDialogState(false)
    val exitWithoutSavingDialogState = MaterialDialogState(false)
    fun requestExit(activity: Activity) {
        activity.finish()
    }

    var needDeleteCover = false
        private set
    var needReplaceCover = false
        private set

    var newCover: Uri? = null
        private set

    fun deleteArtwork() {
        needDeleteCover = true
        needReplaceCover = false
        artwork.tryEmit(null)
    }

    fun replaceArtwork(context: Context) {
        viewModelScope.launch {
            val newArtwork = selectNewArtwork(context)
            while (newArtwork.value == null) yield()
            val uri = newArtwork.value ?: throw Exception("Coroutine Error")
            needReplaceCover = true
            needDeleteCover = false
            newCover = uri
            loadArtwork(context, uri)
        }
    }

    class Factory(private val song: Song, private val color: Color) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TagEditorScreenViewModel(song, color) as T
        }
    }
}

internal fun TagEditorScreenViewModel.generateDiff(): TagDiff {
    val audioDetail = audioDetail.value
    require(audioDetail != null)
    audioDetail.mergeActions()
    val current = audioDetail.info.value
    val tagDiff = audioDetail.pendingEditRequests.map { action ->
        val old = current.tagFields[action.key]?.value() ?: ""
        val new = when (action) {
            is EditAction.Delete -> null
            is EditAction.Update -> action.newValue
        }
        Triple(action.key, old, new)
    }
    val artworkDiff =
        if (needReplaceCover) {
            TagDiff.ArtworkDiff.Replaced(newCover)
        } else if (needDeleteCover) {
            TagDiff.ArtworkDiff.Deleted
        } else {
            TagDiff.ArtworkDiff.None
        }
    return TagDiff(tagDiff, artworkDiff)
}