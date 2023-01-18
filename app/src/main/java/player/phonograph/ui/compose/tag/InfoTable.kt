/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.tag

import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.FieldKey.ALBUM
import org.jaudiotagger.tag.FieldKey.ALBUM_ARTIST
import org.jaudiotagger.tag.FieldKey.ARTIST
import org.jaudiotagger.tag.FieldKey.COMMENT
import org.jaudiotagger.tag.FieldKey.COMPOSER
import org.jaudiotagger.tag.FieldKey.GENRE
import org.jaudiotagger.tag.FieldKey.LYRICIST
import org.jaudiotagger.tag.FieldKey.TITLE
import org.jaudiotagger.tag.FieldKey.TRACK
import org.jaudiotagger.tag.FieldKey.YEAR
import player.phonograph.R
import player.phonograph.model.SongInfoModel
import player.phonograph.model.getFileSizeString
import player.phonograph.model.getReadableDurationString
import player.phonograph.model.songTagNameRes
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
internal fun InfoTable(viewModel: InfoTableState) {

    val titleColor = viewModel.titleColor.collectAsState().value
    val info = viewModel.info.collectAsState().value

    val editable = viewModel is EditableInfoTableState
    val editRequest: EditRequest = remember {
        { key, newValue -> (viewModel as? EditableInfoTableState)?.editRequest(key, newValue) }
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
        Item(R.string.label_track_length, getReadableDurationString(info.trackLength.value()))
        Item(R.string.label_file_format, info.fileFormat.value())
        Item(R.string.label_bit_rate, info.bitRate.value())
        Item(R.string.label_sampling_rate, info.samplingRate.value())
        //
        // Common Tag
        //
        Spacer(modifier = Modifier.height(16.dp))
        Title(stringResource(R.string.music_tags), color = titleColor)
        Tag(info, TITLE, editable, editRequest)
        Tag(info, ARTIST, editable, editRequest)
        Tag(info, ALBUM, editable, editRequest)
        Tag(info, ALBUM_ARTIST, editable, editRequest, hideIfEmpty = true)
        Tag(info, COMPOSER, editable, editRequest, hideIfEmpty = true)
        Tag(info, LYRICIST, editable, editRequest, hideIfEmpty = true)
        Tag(info, YEAR, editable, editRequest)
        Tag(info, GENRE, editable, editRequest)
        Tag(info, TRACK, editable, editRequest, hideIfEmpty = true)
        Tag(info, COMMENT, editable, editRequest, hideIfEmpty = true)
        //
        // Other Tag (if available)
        //
        if (info.otherTags != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Title(stringResource(R.string.other_information))
            info.otherTags?.let { tags ->
                for (tag in tags) {
                    Item(tag.key, tag.value)
                }
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
    info: SongInfoModel,
    key: FieldKey,
    editable: Boolean = false,
    editRequest: EditRequest? = null,
    hideIfEmpty: Boolean = false,
) {
    val tagNameRes = remember { songTagNameRes(key) }
    val tagName = stringResource(id = tagNameRes)
    val tagValue = remember { info.tagValue(key).value() }

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
                onTextChanged = { newValue -> editRequest?.invoke(key, newValue) }
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
    allowReset: Boolean = true
) = VerticalTextFieldItem(
    title = title,
    value = value,
    hint = title,
    onTextChanged = onTextChanged,
    extraTrailingIcon = trailingIcon,
    allowReset = allowReset
)

private const val NA = "-"