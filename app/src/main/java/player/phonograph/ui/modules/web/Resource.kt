/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.ui.modules.web

import mms.lastfm.LastFmAction
import mms.musicbrainz.MusicBrainzAction
import player.phonograph.R
import mms.lastfm.LastFmAction.Target.Album as LastFmAlbum
import mms.lastfm.LastFmAction.Target.Artist as LastFmArtist
import mms.lastfm.LastFmAction.Target.Track as LastFmTrack
import mms.musicbrainz.MusicBrainzAction.Target.Artist as MusicbrainzArtist
import mms.musicbrainz.MusicBrainzAction.Target.Recording as MusicbrainzRecording
import mms.musicbrainz.MusicBrainzAction.Target.Release as MusicbrainzRelease
import mms.musicbrainz.MusicBrainzAction.Target.ReleaseGroup as MusicbrainzReleaseGroup

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