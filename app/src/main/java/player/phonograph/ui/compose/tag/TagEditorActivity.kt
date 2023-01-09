/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.tag

import org.jaudiotagger.tag.FieldKey
import player.phonograph.App
import player.phonograph.R
import player.phonograph.mediastore.SongLoader
import player.phonograph.model.Song
import player.phonograph.model.SongInfoModel
import player.phonograph.model.songTagNameRes
import player.phonograph.ui.compose.base.ComposeToolbarActivity
import player.phonograph.ui.compose.components.Title
import player.phonograph.util.SongDetailUtil.readSong
import player.phonograph.util.tageditor.applyTagEdit
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
        super.onCreate(savedInstanceState)
        val song = parseIntent(this, intent)
        model = TagEditorModel(song, readSong(song))
    }
    @Composable
    override fun SetUpContent() {
        TagEditorActivityContent(model, Color(primaryColor))
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
        if (model.allowExitWithoutSaving.value || model.editRequestModel.allRequests.isEmpty()) {
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

        const val SONG_ID = "SONG_ID"

        fun launch(context: Context, songId: Long) {
            context.startActivity(
                Intent(context.applicationContext, TagEditorActivity::class.java).apply {
                    putExtra(SONG_ID, songId)
                }
            )
        }
    }
}

class TagEditorModel(val song: Song, val infoModel: SongInfoModel) : ViewModel() {
    val editRequestModel: EditRequestModel = EditRequestModel()
    val showSaveConfirmation: MutableState<Boolean> = mutableStateOf(false)
    val showExitWithoutSavingConfirmation: MutableState<Boolean> = mutableStateOf(false)
    val allowExitWithoutSaving: MutableState<Boolean> = mutableStateOf(false)
}

@Composable
fun TagEditorActivityContent(
    model: TagEditorModel,
    titleColor: Color
) {
    Column(
        modifier = Modifier
            .verticalScroll(state = rememberScrollState())
            .fillMaxSize()
    ) {
        InfoTable(
            info = model.infoModel,
            titleColor = titleColor,
            editable = true,
            editRequestModel = model.editRequestModel
        )
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
        title = { Text("Exit without saving?") },
        buttons = {
            Row(Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = {
                        dismiss()
                        model.allowExitWithoutSaving.value = true
                    },
                    Modifier.weight(2f)
                ) {
                    Text(stringResource(id = android.R.string.ok))
                }
                Spacer(modifier = Modifier.widthIn(48.dp))
                TextButton(onClick = dismiss, Modifier.weight(2f)) {
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
        saveImpl(model.song, model.editRequestModel)
    }
    AlertDialog(
        onDismissRequest = dismiss,
        buttons = {
            Row(Modifier.fillMaxWidth()) {
                TextButton(onClick = save, Modifier.weight(2f)) {
                    Text(stringResource(id = android.R.string.ok), color = Color.Red)
                }
                Spacer(modifier = Modifier.widthIn(48.dp))
                TextButton(onClick = dismiss, Modifier.weight(2f)) {
                    Text(stringResource(id = android.R.string.cancel))
                }
            }
        },
        title = { Text(stringResource(id = R.string.music_tags)) },
        text = {
            DiffScreen(model.infoModel, model.editRequestModel)
        }
    )
}

fun saveImpl(song: Song, edit: EditRequestModel) =
    applyTagEdit(
        CoroutineScope(Dispatchers.Unconfined),
        App.instance,
        edit,
        File(song.data)
    )

@Composable
internal fun DiffScreen(old: SongInfoModel, new: EditRequestModel) {
    val diff = remember { EditRequestModel.generateDiff(old, new) }
    if (diff.isEmpty())
        Text(text = stringResource(id = R.string.empty))
    else
        LazyColumn(Modifier.padding(8.dp)) {
            for (tag in diff) {
                item {
                    Diff(tag)
                }
            }
        }
}

@Composable
private fun Diff(tag: Triple<FieldKey, String?, String?>) {
    Column(Modifier.padding(vertical = 16.dp)) {
        Title(stringResource(id = songTagNameRes(tag.first)), horizontalPadding = 0.dp)
        DiffText(tag.second)
        Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
        DiffText(tag.third)
    }
}

@Composable
private fun DiffText(string: String?, modifier: Modifier = Modifier) {
    if (string.isNullOrEmpty()) {
        Text(
            stringResource(id = R.string.empty),
            modifier
                .fillMaxWidth()
                .alpha(0.5f)
        )
    } else {
        Text(string, modifier.fillMaxWidth())
    }
}