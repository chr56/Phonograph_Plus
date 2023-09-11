/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.tag2

import lib.phonograph.misc.IOpenFileStorageAccess
import org.jaudiotagger.tag.FieldKey
import player.phonograph.R
import player.phonograph.mechanism.tag.edit.selectImage
import player.phonograph.model.TagData
import player.phonograph.model.TagField
import player.phonograph.model.allFieldKey
import player.phonograph.ui.compose.components.CascadeVerticalItem
import player.phonograph.ui.compose.components.VerticalTextItem
import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
        Title(stringResource(R.string.files))
        FileList(viewModel)
        Spacer(modifier = Modifier.height(16.dp))
        CoverUpdater(viewModel)
        Spacer(modifier = Modifier.height(16.dp))
        Title(stringResource(R.string.music_tags))
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
    val songInfoModels by viewModel.originalSongInfos.collectAsState()
    val reduced =
        songInfoModels.fold(mutableMapOf<FieldKey, TagField>()) { acc, model ->
            for ((key, value) in model.tagTextOnlyFields) {
                val oldValue = acc[key]
                val newValue = if (oldValue != null) {
                    TagField(
                        key, TagData.TextData("${oldValue.content.text()},\n${value.content.text()}")
                    )
                } else {
                    value
                }
                acc[key] = newValue
            }
            acc
        }
    for ((key, field) in reduced) {
        CommonTag(key, field.content, editable, viewModel::process)
    }
    AddMoreButton(reduced, viewModel::process)
}

@Composable
private fun AddMoreButton(allKeys: Map<FieldKey, TagField>, onEdit: (Context, TagEditEvent) -> Unit) {
    val existKeys = allKeys.keys
    val remainedKeys = allFieldKey.subtract(existKeys)
    AddMoreButton(keys = remainedKeys, onEdit)
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
        state = viewModel.exitWithoutSavingDialogState,
        artworkExist = false,
        onSave = { }, onDelete = { viewModel.process(context, TagEditEvent.RemoveArtwork) },
        onUpdate = {
            viewModel.viewModelScope.launch(Dispatchers.IO) {
                val uri = selectImage((context as IOpenFileStorageAccess).openFileStorageAccessTool)
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
private fun FileList(viewModel: MultiTagBrowserViewModel, modifier: Modifier = Modifier) {
    val songs by viewModel.songs.collectAsState()
    CascadeVerticalItem(
        stringResource(R.string.files),
        collapsible = true,
        collapsed = false
    ) {
        for ((index, song) in songs.withIndex()) {
            Row(modifier = Modifier.fillMaxWidth(), Arrangement.Start) {
                Text(text = "$index", modifier = Modifier.align(Alignment.CenterVertically))
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