/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.modules.tag

import mms.Source
import mms.lastfm.LastFmTrack
import mms.musicbrainz.MusicBrainzRecording
import org.jaudiotagger.tag.FieldKey
import player.phonograph.R
import player.phonograph.ui.modules.web.WebSearchTool
import androidx.compose.foundation.layout.padding
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
@Suppress("UNUSED_PARAMETER")
internal fun RequestWebSearch(
    webSearchTool: WebSearchTool,
    onSearch: (Source) -> Unit,
    onShowWikiDialog: (() -> Unit)?,
) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = !expanded }) {
        Icon(painterResource(id = R.drawable.ic_search_white_24dp), null)
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(onClick = { onSearch(Source.MusicBrainz) }
        ) {
            Text(Source.MusicBrainz.name, Modifier.padding(8.dp))
        }
        DropdownMenuItem(onClick = { onSearch(Source.LastFm) }
        ) {
            Text(Source.LastFm.name, Modifier.padding(8.dp))
        }
        if (onShowWikiDialog != null) {
            DropdownMenuItem(onClick = {
                onShowWikiDialog.invoke()
            }) {
                Text(stringResource(R.string.wiki), Modifier.padding(8.dp))
            }
        }
    }
}

internal fun importResult(viewModel: TagBrowserViewModel, item: Any) {
    when (item) {
        is LastFmTrack          -> insert(viewModel, item)
        is MusicBrainzRecording -> insert(viewModel, item)
    }
}

private fun insert(tableViewModel: TagBrowserViewModel, track: LastFmTrack) =
    with(ProcessScope(tableViewModel)) {

        link(FieldKey.MUSICBRAINZ_TRACK_ID, track.mbid)
        link(FieldKey.TITLE, track.name)
        link(FieldKey.ARTIST, track.artist?.name)
        link(FieldKey.ALBUM, track.album?.name)

        val tags = track.toptags?.tag?.map { it.name }
        link(FieldKey.COMMENT, tags)
        link(FieldKey.GENRE, tags)

    }

private fun insert(tableViewModel: TagBrowserViewModel, recording: MusicBrainzRecording) =
    with(ProcessScope(tableViewModel)) {
        link(FieldKey.MUSICBRAINZ_TRACK_ID, recording.id)
        link(FieldKey.TITLE, recording.title)

        for (artistCredit in recording.artistCredit) {
            link(FieldKey.ARTIST, artistCredit.name)
            link(FieldKey.MUSICBRAINZ_ARTISTID, artistCredit.artist?.id)
        }
        recording.releases?.forEach { release ->
            link(FieldKey.ALBUM, release.title)
            link(FieldKey.MUSICBRAINZ_RELEASEID, release.id)
            link(FieldKey.MUSICBRAINZ_RELEASE_STATUS, release.status)
            link(FieldKey.MUSICBRAINZ_RELEASE_COUNTRY, release.country)
        }

        val genre = recording.genres.map { it.name }
        val tags = recording.tags?.map { it.name }
        link(FieldKey.GENRE, genre)
        link(FieldKey.COMMENT, tags)
        link(FieldKey.COMMENT, recording.disambiguation)
        link(FieldKey.YEAR, recording.firstReleaseDate)
    }

private class ProcessScope(val tableViewModel: TagBrowserViewModel) {
    fun link(fieldKey: FieldKey, value: String?) {
        if (value != null) tableViewModel.insertPrefill(fieldKey, value)
    }

    fun link(fieldKey: FieldKey, values: List<String>?) {
        if (values != null) tableViewModel.insertPrefill(fieldKey, values)
    }
}