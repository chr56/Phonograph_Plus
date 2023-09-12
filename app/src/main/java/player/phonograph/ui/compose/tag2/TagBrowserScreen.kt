/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.tag2

import com.vanpra.composematerialdialogs.MaterialDialogState
import lib.phonograph.misc.IOpenFileStorageAccess
import org.jaudiotagger.tag.FieldKey
import player.phonograph.R
import player.phonograph.mechanism.tag.edit.selectImage
import player.phonograph.model.TagData
import player.phonograph.model.allFieldKey
import player.phonograph.model.getFileSizeString
import player.phonograph.model.text
import player.phonograph.ui.compose.components.CascadeVerticalItem
import player.phonograph.ui.compose.components.CoverImage
import player.phonograph.ui.compose.components.Title
import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun TagBrowserScreen(viewModel: TagBrowserViewModel) {
    val info by viewModel.currentSongInfo.collectAsState()
    val bitmap by viewModel.songBitmap.collectAsState()
    val editable by viewModel.editable.collectAsState()
    Column(
        modifier = Modifier
            .verticalScroll(state = rememberScrollState())
            .fillMaxSize()
    ) {
        // cover
        Artwork(viewModel, bitmap, editable)
        // file
        Column(
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Title(stringResource(R.string.file), color = MaterialTheme.colors.primary)
            Item(R.string.label_file_name, info.fileName.value())
            Item(R.string.label_file_path, info.filePath.value())
            Item(R.string.label_file_size, getFileSizeString(info.fileSize.value()))
            for ((key, field) in info.audioPropertyFields) {
                Item(stringResource(key.res), value = field.value().toString())
            }
            // music tags
            Spacer(modifier = Modifier.height(16.dp))
            Title(stringResource(R.string.music_tags), color = MaterialTheme.colors.primary)
            Item(stringResource(R.string.tag_format), info.tagFormat.id)
            Spacer(modifier = Modifier.height(8.dp))
            for ((key, field) in info.tagTextOnlyFields) {
                CommonTag(key, field.content, editable, viewModel::process)
            }
            if (editable) AddMoreButton(viewModel)
            Spacer(modifier = Modifier.height(16.dp))
            CascadeVerticalItem(stringResource(R.string.raw_tags)) {
                for ((key, rawTag) in info.allTags) {
                    RawTag(key, rawTag)
                }
            }
        }
    }
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
fun Artwork(viewModel: TagBrowserViewModel, bitmap: Bitmap?, editable: Boolean) {
    val context = LocalContext.current
    BoxWithConstraints {
        val coverImageDetailDialogState = remember { MaterialDialogState(false) }
        if (bitmap != null || editable) {
            CoverImage(bitmap, MaterialTheme.colors.primary, Modifier.clickable {
                coverImageDetailDialogState.show()
            })
        }
        CoverImageDetailDialog(
            state = coverImageDetailDialogState,
            artworkExist = bitmap != null,
            onSave = { viewModel.saveArtwork(context) },
            onDelete = { viewModel.process(context, TagEditEvent.RemoveArtwork) },
            onUpdate = {
                viewModel.viewModelScope.launch(Dispatchers.IO) {
                    val uri = selectImage((context as IOpenFileStorageAccess).openFileStorageAccessTool)
                    if (uri != null) {
                        viewModel.process(
                            context, TagEditEvent.UpdateArtwork.from(context, uri, viewModel.song.value.title)
                        )
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, android.R.string.cancel, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                coverImageDetailDialogState.hide()
            },
            editMode = editable
        )
    }
}

@Composable
private fun AddMoreButton(model: TagBrowserViewModel) {
    val songInfoModel by model.currentSongInfo.collectAsState()
    val existKeys = songInfoModel.tagTextOnlyFields.keys
    val remainedKeys = allFieldKey.subtract(existKeys)
    AddMoreButton(remainedKeys, model::process)
}


@Composable
private fun CommonTag(
    key: FieldKey,
    field: TagData,
    editable: Boolean,
    onEdit: (Context, TagEditEvent) -> Unit,
) {
    val context = LocalContext.current
    val tagName = key.text(context.resources)
    val tagValue = field.text()

    Box(modifier = Modifier.fillMaxWidth()) {
        if (editable) {
            EditableItem(key, tagName, tagValue, onEdit = { onEdit(context, it) })
        } else {
            if (tagValue.isNotEmpty()) Item(tagName, tagValue)
        }
    }
}

