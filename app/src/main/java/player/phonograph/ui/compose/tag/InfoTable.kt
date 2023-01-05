/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.compose.tag

import player.phonograph.R
import player.phonograph.model.getReadableDurationString
import player.phonograph.ui.compose.components.Title
import player.phonograph.util.SongDetailUtil
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
internal fun InfoTable(info: SongDetailUtil.SongInfo, titleColor: Color) {
    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        // File info
        Spacer(modifier = Modifier.height(16.dp))
        Title(stringResource(R.string.file), color = titleColor)
        TagItem(stringResource(id = R.string.label_file_name), info.fileName)
        TagItem(stringResource(id = R.string.label_file_path), info.filePath)
        TagItem(
            stringResource(id = R.string.label_track_length),
            getReadableDurationString(info.trackLength ?: -1)
        )
        TagItem(stringResource(id = R.string.label_file_format), info.fileFormat)
        TagItem(
            stringResource(id = R.string.label_file_size),
            SongDetailUtil.getFileSizeString(info.fileSize ?: -1)
        )
        TagItem(stringResource(id = R.string.label_bit_rate), info.bitRate)
        TagItem(stringResource(id = R.string.label_sampling_rate), info.samplingRate)
        // Common Tag
        Spacer(modifier = Modifier.height(16.dp))
        Title(stringResource(R.string.music_tags), color = titleColor)
        TagItem(stringResource(id = R.string.title), info.title)
        TagItem(stringResource(id = R.string.artist), info.artist)
        TagItem(stringResource(id = R.string.album), info.album)
        TagItem(stringResource(id = R.string.album_artist), info.albumArtist, true)
        TagItem(stringResource(id = R.string.composer), info.composer, true)
        TagItem(stringResource(id = R.string.lyricist), info.lyricist, true)
        TagItem(stringResource(id = R.string.year), info.year)
        TagItem(stringResource(id = R.string.genre), info.genre)
        TagItem(stringResource(id = R.string.track), info.track, true)
        // Other Tag (if available)
        if (info.otherTags != null && info.comment != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Title(stringResource(R.string.other_information))
            TagItem(stringResource(id = R.string.comment), info.comment, true)
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