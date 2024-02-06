/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.web

import player.phonograph.R
import util.phonograph.tagsources.lastfm.LastFmAction
import util.phonograph.tagsources.musicbrainz.MusicBrainzAction
import util.phonograph.tagsources.lastfm.LastFmAction.Target.Album as LastFmAlbum
import util.phonograph.tagsources.lastfm.LastFmAction.Target.Artist as LastFmArtist
import util.phonograph.tagsources.lastfm.LastFmAction.Target.Track as LastFmTrack
import util.phonograph.tagsources.musicbrainz.MusicBrainzAction.Target.Artist as MusicbrainzArtist
import util.phonograph.tagsources.musicbrainz.MusicBrainzAction.Target.Recording as MusicbrainzRecording
import util.phonograph.tagsources.musicbrainz.MusicBrainzAction.Target.Release as MusicbrainzRelease
import util.phonograph.tagsources.musicbrainz.MusicBrainzAction.Target.ReleaseGroup as MusicbrainzReleaseGroup

fun LastFmAction.Target.displayTextRes(): Int = when (this) {
    LastFmArtist -> R.string.target_artist
    LastFmAlbum  -> R.string.target_album
    LastFmTrack  -> R.string.target_track
}

fun MusicBrainzAction.Target.displayTextRes(): Int = when (this) {
    MusicbrainzArtist       -> R.string.target_release_group
    MusicbrainzRecording    -> R.string.target_release
    MusicbrainzRelease      -> R.string.target_artist
    MusicbrainzReleaseGroup -> R.string.target_recording
}