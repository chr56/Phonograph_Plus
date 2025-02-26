/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.tag

import com.vanpra.composematerialdialogs.MaterialDialogState
import lib.storage.launcher.IOpenFileStorageAccessible
import player.phonograph.R
import player.phonograph.mechanism.metadata.JAudioTaggerMetadata
import player.phonograph.model.getFileSizeString
import player.phonograph.model.metadata.AudioMetadata
import player.phonograph.model.metadata.ConventionalMusicMetadataKey
import player.phonograph.model.metadata.Metadata
import player.phonograph.model.metadata.MusicMetadata
import player.phonograph.ui.compose.components.CascadeVerticalItem
import player.phonograph.ui.compose.components.CoverImage
import player.phonograph.ui.compose.components.Title
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun TagBrowserScreen(viewModel: AudioMetadataViewModel) {
    val state by viewModel.state.collectAsState()
    val editable by viewModel.editable.collectAsState()
    Column(
        modifier = Modifier
            .verticalScroll(state = rememberScrollState())
            .fillMaxSize()
    ) {
        // cover
        Artwork(viewModel, state?.image, editable)
        // text
        val metadata = state?.metadata
        if (metadata != null) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {

                Spacer(modifier = Modifier.height(16.dp))
                Title(stringResource(R.string.file), color = MaterialTheme.colors.primary)
                AudioProperties(metadata)

                Spacer(modifier = Modifier.height(16.dp))
                Title(stringResource(R.string.music_tags), color = MaterialTheme.colors.primary)
                MusicTags(metadata, viewModel, editable)
            }
        }
    }
    if (editable) {
        val context = LocalContext.current
        SaveConfirmationDialog(
            viewModel.saveConfirmationDialogState,
            viewModel::generateTagDiff
        ) { viewModel.save(context) }
        val activity = context as? ComponentActivity
        ExitWithoutSavingDialog(viewModel.exitWithoutSavingDialogState) { activity?.finish() }
    }
}


@Composable
private fun AudioProperties(metadata: AudioMetadata) {
    val fileProperties = metadata.fileProperties
    val audioProperties = metadata.audioProperties
    Item(R.string.label_file_name, fileProperties.fileName)
    Item(R.string.label_file_path, fileProperties.filePath)
    Item(R.string.label_file_size, getFileSizeString(fileProperties.fileSize))
    for (audioProperty in audioProperties.fields) {
        Item(stringResource(audioProperty.key.res), value = audioProperty.field.text().toString())
    }
}

@Composable
private fun MusicTags(metadata: AudioMetadata, viewModel: AudioMetadataViewModel, editable: Boolean) {
    // Format
    Item(stringResource(R.string.tag_format), metadata.audioMetadataFormat.id)
    // Generic Tags
    Spacer(modifier = Modifier.height(8.dp))
    GenericTagItems(viewModel, metadata.musicMetadata, editable)
    // Raw Tags (JAudioTaggerMetadata Only)
    val musicMetadata = metadata.musicMetadata
    if (musicMetadata is JAudioTaggerMetadata) {
        Spacer(modifier = Modifier.height(16.dp))
        RawTagItems(musicMetadata)
    } else {
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun GenericTagItems(viewModel: AudioMetadataViewModel, musicMetadata: MusicMetadata, editable: Boolean) {
    val updateKey by viewModel.prefillUpdateKey
    val prefillsMap = remember(updateKey) { viewModel.prefillsMap }
    for ((key, field) in musicMetadata.textTagFields) {
        GenericTagItem(key, field, editable, prefillsMap[key], viewModel::submitEditEvent)
    }
    if (editable) AddMoreButton(musicMetadata, viewModel::submitEditEvent)
}

@Composable
private fun RawTagItems(musicMetadata: JAudioTaggerMetadata) {
    CascadeVerticalItem(stringResource(R.string.raw_tags)) {
        for ((key, field) in musicMetadata.allTagFields) {
            JAudioTaggerTagItem(key, field)
        }
    }
}

@Composable
private fun Artwork(viewModel: AudioMetadataViewModel, bitmap: Bitmap?, editable: Boolean) {
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
            onDelete = { viewModel.submitEditEvent(context, TagEditEvent.RemoveArtwork) },
            onUpdate = {
                viewModel.viewModelScope.launch(Dispatchers.IO) {
                    val uri = selectImage((context as IOpenFileStorageAccessible).openFileStorageAccessDelegate)
                    if (uri != null) {
                        val name = viewModel.state.value?.song?.title ?: ""
                        viewModel.submitEditEvent(
                            context, TagEditEvent.UpdateArtwork.from(context, uri, name)
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
private fun AddMoreButton(musicMetadata: MusicMetadata, onApplied: (Context, TagEditEvent) -> Unit) {
    val existKeys = musicMetadata.textTagFields.keys.toSet()
    val remainedKeys = ConventionalMusicMetadataKey.entries.subtract(existKeys)
    AddMoreButton(remainedKeys, onApplied)
}


@Composable
private fun GenericTagItem(
    key: ConventionalMusicMetadataKey,
    field: Metadata.Field,
    editable: Boolean,
    prefills: Collection<String>?,
    onEdit: (Context, TagEditEvent) -> Unit,
) {
    val context = LocalContext.current
    val tagName = if (key.res > 0) stringResource(key.res) else key.name
    val tagValue = field.text().toString()

    Box(modifier = Modifier.fillMaxWidth()) {
        if (editable) {
            EditableItem(key, tagName, tagValue, prefills.orEmpty(), onEdit = { onEdit(context, it) })
        } else {
            if (tagValue.isNotEmpty()) Item(tagName, tagValue)
        }
    }
}

@Composable
private fun JAudioTaggerTagItem(@Suppress("UNUSED_PARAMETER") key: String, rawField: JAudioTaggerMetadata.Field) {
    val (
        id: String,
        name: String,
        value: Metadata.Field,
        description: String?,
    ) = rawField

    Column(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        // name and id
        Row(
            modifier = Modifier
                .align(Alignment.Start)
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Text(
                text = name,
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                ),
                textAlign = TextAlign.Left,
                modifier = Modifier.weight(8f),
            )
            Text(
                text = id,
                style = TextStyle(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                ),
                textAlign = TextAlign.Right,
                modifier = Modifier.weight(2f),
            )
        }
        // description
        if (description != null) {
            Text(
                text = description,
                style = TextStyle(
                    fontWeight = FontWeight.Light,
                    fontSize = 9.sp,
                ),
                modifier = Modifier.align(Alignment.Start),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        // content
        SelectionContainer {
            Text(
                text = value.text().toString(),
                style = TextStyle(
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.92f),
                    fontSize = 14.sp,
                ),
                modifier = Modifier.align(Alignment.Start)
            )
        }
    }
}
