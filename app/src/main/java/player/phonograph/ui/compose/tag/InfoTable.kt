/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.tag

import player.phonograph.R
import player.phonograph.model.getReadableDurationString
import player.phonograph.ui.compose.components.Title
import player.phonograph.ui.compose.components.VerticalTextFieldItem
import player.phonograph.ui.compose.components.VerticalTextItem
import player.phonograph.util.SongDetailUtil
import player.phonograph.util.SongDetailUtil.getFileSizeString
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/**
 * Text infomation
 */
@Composable
internal fun InfoTable(
    info: SongDetailUtil.SongInfo,
    titleColor: Color,
    editable: Boolean = false
) {
    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        //
        // File info
        //
        Spacer(modifier = Modifier.height(16.dp))
        Title(stringResource(R.string.file), color = titleColor)
        Item(R.string.label_file_name, info.fileName ?: NA)
        Item(R.string.label_file_path, info.filePath ?: NA)
        Item(R.string.label_track_length, getReadableDurationString(info.trackLength ?: -1))
        Item(R.string.label_file_format, info.fileFormat ?: NA)
        Item(R.string.label_file_size, getFileSizeString(info.fileSize ?: -1))
        Item(R.string.label_bit_rate, info.bitRate ?: NA)
        Item(R.string.label_sampling_rate, info.samplingRate ?: NA)
        //
        // Common Tag
        //
        Spacer(modifier = Modifier.height(16.dp))
        Title(stringResource(R.string.music_tags), color = titleColor)
        Tag(R.string.title, info.title, editable)
        Tag(R.string.artist, info.artist, editable)
        Tag(R.string.album, info.album, editable)
        Tag(R.string.album_artist, info.albumArtist, editable, true)
        Tag(R.string.composer, info.composer, editable, true)
        Tag(R.string.lyricist, info.lyricist, editable, true)
        Tag(R.string.year, info.year, editable)
        Tag(R.string.genre, info.genre, editable)
        Tag(R.string.track, info.track, editable, true)
        //
        // Other Tag (if available)
        //
        if (info.otherTags != null && info.comment != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Title(stringResource(R.string.other_information))
            Tag(stringResource(id = R.string.comment), info.comment, true)
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
private fun Tag(
    @StringRes tagStringRes: Int,
    name: String?,
    editable: Boolean = false,
    hideIfEmpty: Boolean = false,
) = Tag(name = stringResource(id = tagStringRes), value = name, editable, hideIfEmpty)

@Composable
internal fun Tag(
    name: String,
    value: String?,
    editable: Boolean = false,
    hideIfEmpty: Boolean = false,
) {
    var editMode: Boolean by remember { mutableStateOf(false) }
    val modifier = if (editable) Modifier.clickable { editMode = true } else Modifier
    Box(modifier = modifier) {
        if (editMode) {
            //
            // EditMode
            //
            EditableItem(name, value ?: "", {})
        } else {
            //
            // Common & Readonly
            //
            if (hideIfEmpty) {
                if (!value.isNullOrEmpty()) Item(name, value)
            } else {
                Item(name, value ?: NA)
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
    hint = value ?: "",
    onTextChanged = onTextChanged,
    extraTrailingIcon = trailingIcon,
    allowReset = allowReset
)

private const val NA = "-"