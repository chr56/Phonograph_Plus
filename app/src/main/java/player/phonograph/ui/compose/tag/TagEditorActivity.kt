/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.tag

import com.vanpra.composematerialdialogs.MaterialDialogState
import mt.pref.ThemeColor
import mt.pref.ThemeColor.primaryColor
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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle

class TagEditorActivity : ComposeToolbarActivity(), ICreateFileStorageAccess {

    override val createFileStorageAccessTool: CreateFileStorageAccessTool =
        CreateFileStorageAccessTool()

    private lateinit var song: Song
    private val model: TagEditorScreenViewModel
            by viewModels { TagEditorScreenViewModel.Factory(song, Color(primaryColor(this))) }

    override fun onCreate(savedInstanceState: Bundle?) {
        song = parseIntent(this, intent)
        createFileStorageAccessTool.register(lifecycle, activityResultRegistry)
        super.onCreate(savedInstanceState)
        model.loadArtwork(this) { updateBarsColor() }
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
            Icon(Icons.Default.Done, null)
        }
    }

    override val toolbarBackPressed: () -> Unit = {
        back()
    }

    override fun onBackPressed() {
        back()
    }

    private fun back() {
        if (model.infoTableState.allEditRequests.isEmpty()) {
            finish()
        } else {
            model.exitWithoutSavingDialogState.show()
        }
    }

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
                Intent(context.applicationContext, TagEditorActivity::class.java).apply {
                    putExtra(SONG_ID, songId)
                }
            )
        }
    }
}

class TagEditorScreenViewModel(song: Song, defaultColor: Color) :
        TagBrowserScreenViewModel(song, defaultColor) {
    private var _infoTableViewModel: EditableInfoTableState? = null
    override val infoTableState: EditableInfoTableState
        @Synchronized get() {
            if (_infoTableViewModel == null) {
                _infoTableViewModel =
                    EditableInfoTableState(SongDetailUtil.readSong(song), defaultColor)
            }
            return _infoTableViewModel!!
        }

    val saveConfirmationDialogState = MaterialDialogState(false)
    val exitWithoutSavingDialogState = MaterialDialogState(false)
    fun requestExit(activity: Activity) {
        activity.finish()
    }

    fun deleteArtwork(context: Context) {

    }

    fun replaceArtwork(context: Context) {

    }

    class Factory(private val song: Song, private val color: Color) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TagEditorScreenViewModel(song, color) as T
        }
    }
}