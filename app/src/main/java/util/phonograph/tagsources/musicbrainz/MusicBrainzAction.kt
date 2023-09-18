/*
 *  Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.tagsources.musicbrainz

import player.phonograph.R
import util.phonograph.tagsources.Action
import androidx.annotation.StringRes
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed interface MusicBrainzAction : Action {

    enum class Target(@StringRes val displayTextRes: Int, val urlName: String) {
        ReleaseGroup(R.string.target_release_group, "release-group"),
        Release(R.string.target_release, "release"),
        Artist(R.string.target_artist, "artist"),
        Recording(R.string.target_recording, "recording"),
        ;

        fun link(mbid: String): String = "https://musicbrainz.org/${urlName}/$mbid"
    }

    @Parcelize
    data class Search(val target: Target, val query: String) : MusicBrainzAction, Parcelable
    @Parcelize
    data class View(val target: Target, val mbid: String) : MusicBrainzAction, Parcelable
}