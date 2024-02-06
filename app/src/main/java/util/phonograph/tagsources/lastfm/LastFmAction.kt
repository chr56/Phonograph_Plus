/*
 *  Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.tagsources.lastfm

import util.phonograph.tagsources.Action
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed interface LastFmAction : Action {

    enum class Target {
        Artist,
        Album,
        Track,
        ;
    }

    @Parcelize
    data class Search(
        val target: Target,
        val album: String = "",
        val artist: String = "",
        val track: String = "",
    ) : LastFmAction, Parcelable

    sealed class View(val language: String?) : LastFmAction {
        abstract val item: LastFmSearchResultItem

        class ViewArtist(override val item: ArtistResult.Artist, language: String? = null) : View(language)
        class ViewAlbum(override val item: AlbumResult.Album, language: String? = null) : View(language)
        class ViewTrack(override val item: TrackResult.Track, language: String? = null) : View(language)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is View) return false
            if (language != other.language) return false
            return item == other.item
        }

        override fun hashCode(): Int = 31 * item.hashCode() + (language?.hashCode() ?: 0)


    }
}