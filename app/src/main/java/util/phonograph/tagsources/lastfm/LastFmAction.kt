/*
 *  Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.tagsources.lastfm

import player.phonograph.R
import util.phonograph.tagsources.Action
import androidx.annotation.StringRes
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed interface LastFmAction : Action {

    enum class Target(@StringRes val displayTextRes: Int) {
        Artist(R.string.target_artist),
        Album(R.string.target_album),
        Track(R.string.target_track),
        ;
    }

    @Parcelize
    data class Search(
        val target: Target,
        val album: String = "",
        val artist: String = "",
        val track: String = "",
    ) : LastFmAction, Parcelable

    sealed interface View : LastFmAction {
        data class ViewArtist(val item: ArtistResult.Artist) : View
        data class ViewAlbum(val item: AlbumResult.Album) : View
        data class ViewTrack(val item: TrackResult.Track) : View
    }
}