/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.tag

import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.MaterialDialogState
import com.vanpra.composematerialdialogs.customView
import com.vanpra.composematerialdialogs.title
import mt.pref.ThemeColor.primaryColor
import player.phonograph.App
import player.phonograph.R
import player.phonograph.mediastore.SongLoader
import player.phonograph.model.Song
import player.phonograph.ui.compose.base.ComposeToolbarActivity
import player.phonograph.util.SongDetailUtil.readSong
import player.phonograph.util.tageditor.applyTagEdit
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.io.File

class TagEditorActivity : ComposeToolbarActivity() {

    private lateinit var song: Song
    private val model: TagEditorModel
            by viewModels { TagEditorModel.Factory(song, ::finish, Color(primaryColor(this))) }

    override fun onCreate(savedInstanceState: Bundle?) {
        song = parseIntent(this, intent)
        super.onCreate(savedInstanceState)
    }

    @Composable
    override fun SetUpContent() {
        TagEditorActivityContent(model)
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
        if (model.infoTableViewModel.allEditRequests.isEmpty()) {
            finish()
        } else {
            model.exitWithoutSavingDialogState.show()
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

class TagEditorModel internal constructor(
    val song: Song,
    val exitCallback: () -> Unit,
    val primaryColor: Color
) : ViewModel() {

    val infoTableViewModel = createInfoTableViewModel(song, primaryColor)
    private fun createInfoTableViewModel(song: Song, primaryColor: Color) =
        EditableInfoTableViewModel(readSong(song), primaryColor)

    val saveConfirmationDialogState = MaterialDialogState(false)
    val exitWithoutSavingDialogState = MaterialDialogState(false)
    fun requestExit() = exitCallback()

    class Factory(
        private val song: Song,
        private val exitCallback: () -> Unit,
        private val primaryColor: Color
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return TagEditorModel(song, exitCallback, primaryColor) as T
        }
    }
}

@Composable
fun TagEditorActivityContent(model: TagEditorModel) {
    Column(
        modifier = Modifier
            .verticalScroll(state = rememberScrollState())
            .fillMaxSize()
    ) {
        InfoTable(model.infoTableViewModel)
    }
    SaveConfirmationDialog(model)
    ExitWithoutSavingDialog(model)
}

@Composable
fun ExitWithoutSavingDialog(model: TagEditorModel) {
    val dismiss = { model.exitWithoutSavingDialogState.hide() }
    MaterialDialog(
        dialogState = model.exitWithoutSavingDialogState,
        elevation = 0.dp,
        autoDismiss = false,
        buttons = {
            positiveButton(res = android.R.string.cancel, onClick = dismiss)
            button(res = android.R.string.ok) {
                dismiss()
                model.requestExit()
            }
        }
    ) {
        title(res = R.string.exit_without_saving)
    }
}

@Composable
fun SaveConfirmationDialog(model: TagEditorModel) {
    val dismiss = { model.saveConfirmationDialogState.hide() }
    val save = {
        dismiss()
        saveImpl(model)
    }
    MaterialDialog(
        dialogState = model.saveConfirmationDialogState,
        elevation = 0.dp,
        autoDismiss = false,
        buttons = {
            button(res = R.string.save, onClick = save)
            button(res = android.R.string.cancel, onClick = dismiss)
        }
    ) {
        title(res = R.string.save)
        customView {
            DiffScreen(model)
        }
    }
}

fun saveImpl(model: TagEditorModel) =
    applyTagEdit(
        CoroutineScope(Dispatchers.Unconfined),
        App.instance,
        model.infoTableViewModel.allEditRequests,
        File(model.song.data)
    )

