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
import mt.util.color.darkenColor
import player.phonograph.R
import player.phonograph.mechanism.tag.edit.selectNewArtwork
import player.phonograph.mechanism.tag.loadSongInfo
import player.phonograph.model.Song
import player.phonograph.repo.mediastore.loaders.SongLoader
import player.phonograph.ui.compose.base.ComposeToolbarActivity
import player.phonograph.ui.compose.theme.PhonographTheme
import androidx.activity.viewModels
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

class TagEditorActivity :
        ComposeToolbarActivity(),
        ICreateFileStorageAccess,
        IOpenFileStorageAccess {

    override val createFileStorageAccessTool: CreateFileStorageAccessTool =
        CreateFileStorageAccessTool()
    override val openFileStorageAccessTool: OpenFileStorageAccessTool =
        OpenFileStorageAccessTool()

    private lateinit var song: Song
    private val model: TagEditorScreenViewModel
            by viewModels { TagEditorScreenViewModel.Factory(song, Color(primaryColor(this))) }

    override fun onCreate(savedInstanceState: Bundle?) {
        song = parseIntent(this, intent)
        createFileStorageAccessTool.register(lifecycle, activityResultRegistry)
        openFileStorageAccessTool.register(lifecycle, activityResultRegistry)
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

    override val title: String get() = getString(R.string.action_tag_editor)

    override val toolbarActions: @Composable RowScope.() -> Unit = {
        IconButton(onClick = { model.saveConfirmationDialogState.show() }) {
            Icon(painterResource(id = R.drawable.ic_save_white_24dp), null)
        }
    }

    override val toolbarBackPressed: () -> Unit = {
        back()
    }

    override fun onBackPressed() {
        back()
    }

    private fun back() {
        if (!model.audioDetailState.hasEdited) {
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
}

class TagEditorScreenViewModel(song: Song, defaultColor: Color) :
        TagBrowserScreenViewModel(song, defaultColor) {
    private var _audioDetailState: AudioDetailState? = null
    override val audioDetailState: AudioDetailState
        get() {
            if (_audioDetailState == null) {
                _audioDetailState =
                    AudioDetailState(loadSongInfo(song), defaultColor, true)
            }
            return _audioDetailState!!
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
            loadArtworkImpl(context, uri)
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
    val current = audioDetailState.info.value
    val tagDiff = audioDetailState.allEditRequests.map { (key, new) ->
        val old = current.tagFields[key]?.value() ?: ""
        Triple(key, old, new)
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