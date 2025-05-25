/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.modules.tag

import player.phonograph.R
import player.phonograph.model.Song
import player.phonograph.model.metadata.ConventionalMusicMetadataKey
import player.phonograph.ui.compose.components.Title
import player.phonograph.ui.modules.tag.MetadataUIEvent.Edit
import player.phonograph.ui.modules.tag.components.ArtworkSection
import player.phonograph.ui.modules.tag.components.EditableTagItem
import player.phonograph.ui.modules.tag.components.FileItems
import player.phonograph.ui.modules.tag.components.InsertNewButton
import player.phonograph.ui.modules.tag.components.ReadonlyTagItem
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import android.content.Context

@Composable
fun MultiTagBrowserScreen(viewModel: MultiTagBrowserActivityViewModel) {
    val state by viewModel.state.collectAsState()
    BrowserScreenFrame(viewModel, {
        FileListSection(state?.songs)
    }, {
        ArtworkSection(viewModel)
    }) {
        Spacer(modifier = Modifier.height(16.dp))
        Title(stringResource(R.string.label_music_tags), color = MaterialTheme.colors.primary)
        GenericTagItems(viewModel)
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
            key, reducedValues, editorValue, editable, viewModel::submitEvent
        )
    }
    if (editable) {
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
            val value = editorValue.orEmpty()
            val options = allValues.orEmpty()
            EditableTagItem(key, tagName, value, options, onEdit = { onEdit(context, it) })
        } else {
            val value = allValues?.toSet()?.joinToString("\n") ?: stringResource(R.string.msg_empty)
            ReadonlyTagItem(tagName, value)
        }
    }
}


@Composable
private fun FileListSection(songs: Set<Song>?) {
    if (songs != null) FileItems(songs.toList())
}

@Composable
private fun ArtworkSection(viewModel: MultiTagBrowserActivityViewModel) {
    val editable by viewModel.editable.collectAsState()
    ArtworkSection(viewModel, false, editable, viewModel.state.hashCode().toString()) {
        Text(
            text = stringResource(R.string.action_update_image),
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .fillMaxWidth(),
            color = MaterialTheme.colors.primary,
            style = MaterialTheme.typography.subtitle2,
        )
    }
}
