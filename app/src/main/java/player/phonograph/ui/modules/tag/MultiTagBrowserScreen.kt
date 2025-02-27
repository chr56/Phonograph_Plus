/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.modules.tag

import lib.storage.launcher.IOpenFileStorageAccessible
import player.phonograph.R
import player.phonograph.model.metadata.ConventionalMusicMetadataKey
import player.phonograph.ui.compose.components.CascadeVerticalItem
import player.phonograph.ui.compose.components.Title
import player.phonograph.ui.compose.components.VerticalTextItem
import player.phonograph.ui.modules.tag.MetadataUIEvent.Edit
import player.phonograph.ui.modules.tag.components.EditableTagItem
import player.phonograph.ui.modules.tag.components.InsertNewButton
import player.phonograph.ui.modules.tag.components.ReadonlyTagItem
import player.phonograph.ui.modules.tag.dialogs.CoverImageDetailDialog
import player.phonograph.ui.modules.tag.dialogs.ExitWithoutSavingDialog
import player.phonograph.ui.modules.tag.dialogs.SaveConfirmationDialog
import player.phonograph.ui.modules.tag.util.selectImage
import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun MultiTagBrowserScreen(viewModel: MultiTagBrowserActivityViewModel) {
    Column(
        modifier = Modifier
            .verticalScroll(state = rememberScrollState())
            .padding(horizontal = 16.dp)
            .fillMaxSize()
    ) {
        Title(stringResource(R.string.files), color = MaterialTheme.colors.primary)
        FileList(viewModel)
        Spacer(modifier = Modifier.height(16.dp))
        CoverUpdater(viewModel)
        Spacer(modifier = Modifier.height(16.dp))
        Title(stringResource(R.string.music_tags), color = MaterialTheme.colors.primary)
        GenericTagItems(viewModel)
        Spacer(modifier = Modifier.height(64.dp))
    }
    val editable by viewModel.editable.collectAsState()
    if (editable) {
        val context = LocalContext.current
        SaveConfirmationDialog(
            viewModel.saveConfirmationDialogState,
            viewModel::generateMetadataDifference
        ) { viewModel.submitEvent(context, MetadataUIEvent.Save) }
        val activity = context as? ComponentActivity
        ExitWithoutSavingDialog(viewModel.exitWithoutSavingDialogState) { activity?.finish() }
    }
}

@Composable
private fun GenericTagItems(viewModel: MultiTagBrowserActivityViewModel) {
    val editable by viewModel.editable.collectAsState()
    val state by viewModel.state.collectAsState()
    val displayTags = state?.displayed ?: emptyMap()
    val reducedTags = state?.fields ?: emptyMap()
    for ((key, _) in displayTags) {
        val reducedValues = reducedTags[key]?.map { it.text().toString() }
        val editorValue = displayTags[key]
        GenericTagItem(
            key,
            reducedValues,
            editorValue,
            editable,
            viewModel::submitEvent
        )
    }
    if (editable){
        val remainedKeys = ConventionalMusicMetadataKey.entries.subtract(reducedTags.keys)
        InsertNewButton(remainedKeys, viewModel::submitEvent)
    }
}

@Composable
private fun GenericTagItem(
    key: ConventionalMusicMetadataKey,
    allValues: List<String>?,
    editorValue: String?,
    editable: Boolean,
    onEdit: (Context, Edit) -> Unit,
) {
    val context = LocalContext.current
    val tagName = if (key.res > 0) stringResource(key.res) else key.name

    Box(modifier = Modifier.fillMaxWidth()) {
        if (editable) {
            EditableTagItem(key, tagName, editorValue.orEmpty(), allValues.orEmpty(), onEdit = { onEdit(context, it) })
        } else {
            ReadonlyTagItem(tagName, allValues.orEmpty())
        }
    }
}


@Composable
private fun CoverUpdater(viewModel: MultiTagBrowserActivityViewModel) {
    Text(
        text = stringResource(R.string.update_image),
        Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .clickable {
                viewModel.coverImageDetailDialogState.show()
            }
    )
    val context = LocalContext.current
    CoverImageDetailDialog(
        state = viewModel.coverImageDetailDialogState,
        artworkExist = false,
        onSave = { }, onDelete = { viewModel.submitEvent(context, Edit.RemoveArtwork) },
        onUpdate = {
            viewModel.viewModelScope.launch(Dispatchers.IO) {
                val uri = selectImage((context as IOpenFileStorageAccessible).openFileStorageAccessDelegate)
                if (uri != null) {
                    viewModel.submitEvent(
                        context, Edit.UpdateArtwork.from(context, uri, viewModel.state.hashCode().toString())
                    )
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, android.R.string.cancel, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            viewModel.coverImageDetailDialogState.hide()
        },
        editMode = true
    )
}

@Composable
@Suppress("UNUSED_PARAMETER")
private fun FileList(viewModel: MultiTagBrowserActivityViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsState()
    val songs = state?.songs
    if (songs != null) {
        CascadeVerticalItem(
            stringResource(R.string.files),
            collapsible = true,
            collapsed = false
        ) {
            for ((index, song) in songs.withIndex()) {
                Row(modifier = Modifier.fillMaxWidth(), Arrangement.Start) {
                    Box(
                        Modifier
                            .align(Alignment.Top)
                            .padding(top = 8.dp)
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_music_note_white_24dp), null
                        )
                        Text(
                            "${index + 1}",
                            Modifier.align(Alignment.BottomEnd),
                            style = MaterialTheme.typography.overline
                        )
                    }
                    Column {
                        VerticalTextItem(
                            stringResource(R.string.title),
                            song.title
                        )
                        VerticalTextItem(
                            stringResource(R.string.label_file_path),
                            song.data
                        )
                    }
                }
            }
        }
    }
}