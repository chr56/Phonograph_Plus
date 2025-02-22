/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.modules.tag

import lib.storage.launcher.IOpenFileStorageAccessible
import player.phonograph.R
import player.phonograph.mechanism.metadata.edit.selectImage
import player.phonograph.model.metadata.ConventionalMusicMetadataKey
import player.phonograph.ui.compose.components.CascadeVerticalItem
import player.phonograph.ui.compose.components.Title
import player.phonograph.ui.compose.components.VerticalTextItem
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
internal fun MultiTagBrowserScreen(viewModel: MultiTagBrowserViewModel) {
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
        CommonTags(viewModel)
    }
    val editable by viewModel.editable.collectAsState()
    if (editable) {
        val context = LocalContext.current
        SaveConfirmationDialog(
            viewModel.saveConfirmationDialogState,
            viewModel::diff
        ) { viewModel.save(context) }
        val activity = context as? ComponentActivity
        ExitWithoutSavingDialog(viewModel.exitWithoutSavingDialogState) { activity?.finish() }
    }
}

@Composable
private fun CommonTags(viewModel: MultiTagBrowserViewModel) {
    val editable by viewModel.editable.collectAsState()
    val displayTags by viewModel.displayTags.collectAsState()
    val reducedTags by viewModel.reducedOriginalTags().collectAsState(mutableMapOf())
    for ((key, _) in displayTags) {
        val reducedValues = reducedTags[key]?.map { it.text().toString() }
        val editorValue = displayTags[key]
        CommonTag(
            key,
            reducedValues,
            editorValue,
            editable,
            viewModel::process
        )
    }
    if (editable) AddMoreButtonWithoutExistedKeys(reducedTags.keys, viewModel::process)
}

@Composable
private fun CommonTag(
    key: ConventionalMusicMetadataKey,
    allValues: List<String>?,
    editorValue: String?,
    editable: Boolean,
    onEdit: (Context, TagEditEvent) -> Unit,
) {
    val context = LocalContext.current
    val tagName = if (key.res > 0 ) stringResource(key.res) else key.name

    Box(modifier = Modifier.fillMaxWidth()) {
        if (editable) {
            EditableItem(key, tagName, editorValue.orEmpty(), allValues.orEmpty(), onEdit = { onEdit(context, it) })
        } else {
            if (!allValues.isNullOrEmpty()) {
                Item(tagName, allValues.joinToString(",\n"))
            }
        }
    }
}


@Composable
private fun CoverUpdater(viewModel: MultiTagBrowserViewModel) {
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
        onSave = { }, onDelete = { viewModel.process(context, TagEditEvent.RemoveArtwork) },
        onUpdate = {
            viewModel.viewModelScope.launch(Dispatchers.IO) {
                val uri = selectImage((context as IOpenFileStorageAccessible).openFileStorageAccessDelegate)
                if (uri != null) {
                    viewModel.process(
                        context, TagEditEvent.UpdateArtwork.from(context, uri, viewModel.songs.hashCode().toString())
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
private fun FileList(viewModel: MultiTagBrowserViewModel, modifier: Modifier = Modifier) {
    val songs by viewModel.songs.collectAsState()
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
                        .padding(top = 8.dp)) {
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