/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.ui.modules.tag.util

import mms.lastfm.LastFmTrack
import mms.musicbrainz.MusicBrainzRecording
import player.phonograph.model.metadata.ConventionalMusicMetadataKey
import player.phonograph.ui.modules.tag.TagBrowserActivityViewModel

fun importWebSearchResult(viewModel: TagBrowserActivityViewModel, item: Any) {
    when (item) {
        is LastFmTrack          -> insert(viewModel, item)
        is MusicBrainzRecording -> insert(viewModel, item)
    }
}

private fun insert(tableViewModel: TagBrowserActivityViewModel, track: LastFmTrack) =
    with(ProcessScope(tableViewModel)) {

        link(ConventionalMusicMetadataKey.MUSICBRAINZ_TRACK_ID, track.mbid)
        link(ConventionalMusicMetadataKey.TITLE, track.name)
        link(ConventionalMusicMetadataKey.ARTIST, track.artist?.name)
        link(ConventionalMusicMetadataKey.ALBUM, track.album?.name)

        val tags = track.toptags?.tag?.map { it.name }
        link(ConventionalMusicMetadataKey.COMMENT, tags)
        link(ConventionalMusicMetadataKey.GENRE, tags)

    }

private fun insert(tableViewModel: TagBrowserActivityViewModel, recording: MusicBrainzRecording) =
    with(ProcessScope(tableViewModel)) {
        link(ConventionalMusicMetadataKey.MUSICBRAINZ_TRACK_ID, recording.id)
        link(ConventionalMusicMetadataKey.TITLE, recording.title)

        for (artistCredit in recording.artistCredit) {
            link(ConventionalMusicMetadataKey.ARTIST, artistCredit.name)
            link(ConventionalMusicMetadataKey.MUSICBRAINZ_ARTISTID, artistCredit.artist?.id)
        }
        recording.releases?.forEach { release ->
            link(ConventionalMusicMetadataKey.ALBUM, release.title)
            link(ConventionalMusicMetadataKey.MUSICBRAINZ_RELEASEID, release.id)
            link(ConventionalMusicMetadataKey.MUSICBRAINZ_RELEASE_STATUS, release.status)
            link(ConventionalMusicMetadataKey.MUSICBRAINZ_RELEASE_COUNTRY, release.country)
        }

        val genre = recording.genres.map { it.name }
        val tags = recording.tags?.map { it.name }
        link(ConventionalMusicMetadataKey.GENRE, genre)
        link(ConventionalMusicMetadataKey.COMMENT, tags)
        link(ConventionalMusicMetadataKey.COMMENT, recording.disambiguation)
        link(ConventionalMusicMetadataKey.YEAR, recording.firstReleaseDate)
    }

private class ProcessScope(val tableViewModel: TagBrowserActivityViewModel) {
    fun link(fieldKey: ConventionalMusicMetadataKey, value: String?) {
        if (value != null) tableViewModel.insertPrefill(fieldKey, value)
    }

    fun link(fieldKey: ConventionalMusicMetadataKey, values: List<String>?) {
        if (values != null) tableViewModel.insertPrefill(fieldKey, values)
    }
}