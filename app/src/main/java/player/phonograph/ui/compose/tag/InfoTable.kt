/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.tag

import player.phonograph.R
import player.phonograph.model.getReadableDurationString
import player.phonograph.ui.compose.components.Title
import player.phonograph.ui.compose.components.VerticalTextItem
import player.phonograph.util.SongDetailUtil
import player.phonograph.util.SongDetailUtil.getFileSizeString
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
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
        Tag(R.string.title, info.title)
        Tag(R.string.artist, info.artist)
        Tag(R.string.album, info.album)
        Tag(R.string.album_artist, info.albumArtist, true)
        Tag(R.string.composer, info.composer, true)
        Tag(R.string.lyricist, info.lyricist, true)
        Tag(R.string.year, info.year)
        Tag(R.string.genre, info.genre)
        Tag(R.string.track, info.track, true)
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
private fun Tag(@StringRes tagStringRes: Int, value: String?, hideIfEmpty: Boolean = false) =
    Tag(tag = stringResource(id = tagStringRes), value = value, hideIfEmpty)

@Composable
internal fun Tag(tag: String, value: String?, hideIfEmpty: Boolean = false) {
    if (hideIfEmpty) {
        if (!value.isNullOrEmpty()) Item(tag, value)
    } else {
        Item(tag, value ?: NA)
    }
}


@Composable
private fun Item(@StringRes tagStringRes: Int, value: String) =
    Item(stringResource(tagStringRes), value)


@Composable
internal fun Item(tag: String, value: String) = VerticalTextItem(tag, value)

private const val NA = "-"