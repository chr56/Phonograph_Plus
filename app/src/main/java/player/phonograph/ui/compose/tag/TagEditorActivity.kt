/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.tag

import mt.pref.ThemeColor
import player.phonograph.App
import player.phonograph.R
import player.phonograph.mediastore.SongLoader
import player.phonograph.model.Song
import player.phonograph.ui.compose.base.ComposeToolbarActivity
import player.phonograph.util.SongDetailUtil.readSong
import player.phonograph.util.tageditor.applyTagEdit
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.io.File

class TagEditorActivity : ComposeToolbarActivity() {
    private lateinit var model: TagEditorModel
    override fun onCreate(savedInstanceState: Bundle?) {
        val song = parseIntent(this, intent)
        val infoTableViewModel =
            EditableInfoTableViewModel(readSong(song), Color(ThemeColor.primaryColor(this)))
        model = TagEditorModel(song, infoTableViewModel)
        super.onCreate(savedInstanceState)
    }

    @Composable
    override fun SetUpContent() {
        TagEditorActivityContent(model)
    }

    override val title: String get() = getString(R.string.action_tag_editor)

    override val toolbarActions: @Composable RowScope.() -> Unit = {
        IconButton(onClick = ::save) {
            Icon(Icons.Default.Done, null)
        }
    }

    private fun save() {
        model.showSaveConfirmation.value = true
    }

    override val toolbarBackPressed: () -> Unit = {
        if (model.allowExitWithoutSaving.value || model.infoTableViewModel.allEditRequests.isEmpty()) {
            finish()
        } else {
            model.showExitWithoutSavingConfirmation.value = true
        }
    }

    override fun onBackPressed() {
        toolbarBackPressed()
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

class TagEditorModel(
    val song: Song,
    val infoTableViewModel: EditableInfoTableViewModel
) : ViewModel() {
    val showSaveConfirmation: MutableState<Boolean> = mutableStateOf(false)
    val showExitWithoutSavingConfirmation: MutableState<Boolean> = mutableStateOf(false)
    val allowExitWithoutSaving: MutableState<Boolean> = mutableStateOf(false)
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
    if (model.showSaveConfirmation.value) {
        SaveConfirmationDialog(model)
    }
    if (model.showExitWithoutSavingConfirmation.value) {
        ExitWithoutSavingDialog(model)
    }
}

@Composable
fun ExitWithoutSavingDialog(model: TagEditorModel) {
    val dismiss = { model.showExitWithoutSavingConfirmation.value = false }
    AlertDialog(
        onDismissRequest = dismiss,
        title = {
            Text(
                stringResource(id = R.string.exit_without_saving),
                style = MaterialTheme.typography.h6
            )
        },
        buttons = {
            Row(Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = {
                        dismiss()
                        model.allowExitWithoutSaving.value = true
                    },
                    Modifier
                        .padding(horizontal = 16.dp)
                        .wrapContentWidth(Alignment.Start)
                ) {
                    Text(stringResource(id = android.R.string.ok))
                }
                Spacer(modifier = Modifier.widthIn(48.dp))
                TextButton(
                    onClick = dismiss,
                    Modifier
                        .padding(horizontal = 16.dp)
                        .wrapContentWidth(Alignment.End)
                ) {
                    Text(stringResource(id = android.R.string.cancel))
                }
            }
        }
    )
}

@Composable
fun SaveConfirmationDialog(model: TagEditorModel) {
    val dismiss = { model.showSaveConfirmation.value = false }
    val save = {
        dismiss()
        saveImpl(model)
    }
    AlertDialog(
        onDismissRequest = dismiss,
        buttons = {
            Row(Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = save,
                    Modifier
                        .padding(horizontal = 16.dp)
                        .wrapContentWidth(Alignment.Start)
                ) {
                    Text(stringResource(id = R.string.save), color = Color.Red)
                }
                Spacer(modifier = Modifier.widthIn(48.dp))
                TextButton(
                    onClick = dismiss,
                    Modifier
                        .padding(horizontal = 16.dp)
                        .wrapContentWidth(Alignment.End)
                ) {
                    Text(stringResource(id = android.R.string.cancel))
                }
            }
        },
        title = {
            Text(
                stringResource(id = R.string.save),
                style = MaterialTheme.typography.h6
            )
        },
        text = {
            DiffScreen(model)
        }
    )
}

fun saveImpl(model: TagEditorModel) =
    applyTagEdit(
        CoroutineScope(Dispatchers.Unconfined),
        App.instance,
        model.infoTableViewModel.allEditRequests,
        File(model.song.data)
    )

