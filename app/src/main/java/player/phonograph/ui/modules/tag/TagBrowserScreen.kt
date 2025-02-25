/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.modules.tag

import com.vanpra.composematerialdialogs.MaterialDialogState
import lib.storage.launcher.IOpenFileStorageAccessible
import player.phonograph.R
import player.phonograph.mechanism.metadata.JAudioTaggerMetadata
import player.phonograph.model.getFileSizeString
import player.phonograph.model.metadata.ConventionalMusicMetadataKey
import player.phonograph.model.metadata.Metadata
import player.phonograph.ui.compose.components.CascadeVerticalItem
import player.phonograph.ui.compose.components.CoverImage
import player.phonograph.ui.compose.components.Title
import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
    val metadata by viewModel.currentSongMetadata.collectAsState()
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
            val fileProperties = metadata.fileProperties
            val audioProperties = metadata.audioProperties
            Spacer(modifier = Modifier.height(16.dp))
            Title(stringResource(R.string.file), color = MaterialTheme.colors.primary)
            Item(R.string.label_file_name, fileProperties.fileName)
            Item(R.string.label_file_path, fileProperties.filePath)
            Item(R.string.label_file_size, getFileSizeString(fileProperties.fileSize))
            for (audioProperty in audioProperties.fields) {
                Item(stringResource(audioProperty.key.res), value = audioProperty.field.text().toString())
            }
            // music tags
            Spacer(modifier = Modifier.height(16.dp))
            Title(stringResource(R.string.music_tags), color = MaterialTheme.colors.primary)
            Item(stringResource(R.string.tag_format), metadata.audioMetadataFormat.id)
            val musicMetadata = metadata.musicMetadata
            if (musicMetadata is JAudioTaggerMetadata) {
                Spacer(modifier = Modifier.height(8.dp))
                val updateKey by viewModel.prefillUpdateKey
                val prefillsMap = remember(updateKey) { viewModel.prefillsMap }
                for ((key, field) in musicMetadata.textTagFields) {
                    CommonTag(key, field, editable, prefillsMap[key], viewModel::process)
                }
                if (editable) AddMoreButton(viewModel)
                Spacer(modifier = Modifier.height(16.dp))
                CascadeVerticalItem(stringResource(R.string.raw_tags)) {
                    for ((key, field) in musicMetadata.allTagFields) {
                        RawTag(key, field)
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
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
    Box {
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
                    val uri = selectImage((context as IOpenFileStorageAccessible).openFileStorageAccessDelegate)
                    if (uri != null) {
                        viewModel.process(
                            context, TagEditEvent.UpdateArtwork.from(context, uri, viewModel.song.value?.title ?: "")
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
    val metadata by model.currentSongMetadata.collectAsState()
    val musicMetadata = metadata.musicMetadata
    val existKeys = if (musicMetadata is JAudioTaggerMetadata) {
        musicMetadata.textTagFields.keys.toSet()
    } else {
        emptySet()
    }
    AddMoreButtonWithoutExistedKeys(existKeys, model::process)
}


@Composable
private fun CommonTag(
    key: ConventionalMusicMetadataKey,
    field: Metadata.Field,
    editable: Boolean,
    prefills: Collection<String>?,
    onEdit: (Context, TagEditEvent) -> Unit,
) {
    val context = LocalContext.current
    val tagName = if (key.res > 0 ) stringResource(key.res) else key.name
    val tagValue = field.text().toString()

    Box(modifier = Modifier.fillMaxWidth()) {
        if (editable) {
            EditableItem(key, tagName, tagValue, prefills.orEmpty(), onEdit = { onEdit(context, it) })
        } else {
            if (tagValue.isNotEmpty()) Item(tagName, tagValue)
        }
    }
}

