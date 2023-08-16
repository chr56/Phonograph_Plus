/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.tag

import player.phonograph.R
import player.phonograph.model.TagField
import player.phonograph.model.getFileSizeString
import player.phonograph.model.res
import player.phonograph.ui.compose.components.Title
import player.phonograph.ui.compose.components.VerticalTextFieldItem
import player.phonograph.ui.compose.components.VerticalTextItem
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/**
 * Text infomation
 */
@Composable
internal fun InfoTable(stateHolder: InfoTableState) {

    val titleColor = stateHolder.titleColor.collectAsState().value
    val info = stateHolder.info.collectAsState().value

    val editable = stateHolder is EditableInfoTableState
    val editRequest: EditRequest = remember {
        { key, newValue -> (stateHolder as? EditableInfoTableState)?.editRequest(key, newValue) }
    }

    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        //
        // File info
        //
        Spacer(modifier = Modifier.height(16.dp))
        Title(stringResource(R.string.file), color = titleColor)
        Item(R.string.label_file_name, info.fileName.value())
        Item(R.string.label_file_path, info.filePath.value())
        Item(R.string.label_file_size, getFileSizeString(info.fileSize.value()))

        for ((key, field) in info.audioPropertyFields) {
            Item(stringResource(key.res), value = field.value().toString())
        }

        //
        // Common Tag
        //
        Spacer(modifier = Modifier.height(16.dp))
        Title(stringResource(R.string.music_tags), color = titleColor)
        for ((_, field) in info.tagFields) {
            Tag(field, editable, editRequest, hideIfEmpty = true)
        }
        //
        // Other Tag (if available)
        //
        info.allTags?.let { tags ->
            Spacer(modifier = Modifier.height(8.dp))
            Title(stringResource(R.string.raw_tags), color = titleColor)
            Item(R.string.tag_format, info.tagFormat.id)
            for ((key, value) in tags) {
                Item(key, value)
            }
        }
        // Lyrics
        // Spacer(modifier = Modifier.height(16.dp))
        // Title(stringResource(R.string.lyrics), color = color)
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
internal fun Tag(
    field: TagField,
    editable: Boolean = false,
    editRequest: EditRequest? = null,
    hideIfEmpty: Boolean = false,
) {
    val tagNameRes = remember { field.key.res() }
    val tagName = stringResource(id = tagNameRes)
    val tagValue = remember { field.value() }

    var editMode: Boolean by remember { mutableStateOf(false) }
    val modifier = if (editable) Modifier.clickable { editMode = true } else Modifier

    Box(modifier = modifier.fillMaxWidth()) {
        if (editMode) {
            //
            // EditMode
            //
            EditableItem(
                title = tagName,
                value = tagValue,
                onTextChanged = { newValue -> editRequest?.invoke(field.key, newValue) }
            )
        } else {
            //
            // Common & Readonly
            //
            if (hideIfEmpty && !editable) {
                if (tagValue.isNotEmpty()) Item(tagName, tagValue)
            } else {
                Item(tagName, tagValue)
            }
        }
    }
}


@Composable
private fun Item(@StringRes tagStringRes: Int, value: String) =
    Item(stringResource(tagStringRes), value)

@Composable
internal fun Item(tag: String, value: String) = VerticalTextItem(title = tag, value = value)

@Composable
internal fun EditableItem(
    title: String,
    value: String?,
    onTextChanged: (String) -> Unit,
    trailingIcon: @Composable (() -> Unit)? = null,
    allowReset: Boolean = true,
) = VerticalTextFieldItem(
    title = title,
    value = value,
    hint = title,
    onTextChanged = onTextChanged,
    extraTrailingIcon = trailingIcon,
    allowReset = allowReset
)

private const val NA = "-"