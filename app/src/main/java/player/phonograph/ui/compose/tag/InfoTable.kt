/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.tag

import player.phonograph.R
import player.phonograph.model.SongInfoModel
import player.phonograph.model.getFileSizeString
import player.phonograph.model.getReadableDurationString
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/**
 * Text infomation
 */
@Composable
internal fun InfoTable(
    info: SongInfoModel,
    titleColor: Color,
    editable: Boolean = false
) {
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
        Tag(R.string.title, info.title.value(), editable)
        Tag(R.string.artist, info.artist.value(), editable)
        Tag(R.string.album, info.album.value(), editable)
        Tag(R.string.album_artist, info.albumArtist.value(), editable, true)
        Tag(R.string.composer, info.composer.value(), editable, true)
        Tag(R.string.lyricist, info.lyricist.value(), editable, true)
        Tag(R.string.year, info.year.value(), editable)
        Tag(R.string.genre, info.genre.value(), editable)
        Tag(R.string.track, info.track.value(), editable, true)
        Tag(R.string.comment, info.comment.value(), true)
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
    Box(modifier = modifier.fillMaxWidth()) {
        if (editMode) {
            //
            // EditMode
            //
            EditableItem(title = name, value = value, onTextChanged = { /** todo **/ })
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
    hint = title,
    onTextChanged = onTextChanged,
    extraTrailingIcon = trailingIcon,
    allowReset = allowReset
)

private const val NA = "-"