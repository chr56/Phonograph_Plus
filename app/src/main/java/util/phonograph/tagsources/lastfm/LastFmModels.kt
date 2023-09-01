/*
 *  Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.tagsources.lastfm

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
class LastFmAlbumResponse(val album: LastFmAlbum)

@Keep
@Serializable
class LastFmArtistResponse(val artist: LastFmArtist)

@Keep
@Serializable
class LastFmTrackResponse(val track: LastFmTrack)

@Keep
@Serializable
class LastFmImage(
    @SerialName("#text")
    val text: String,
    val size: String?,
)

@Keep
@Serializable
class LastFmWikiData(
    val published: String? = null,
    val summary: String? = null,
    val content: String? = null,
    // val links: Links? = null,
)

@Keep
@Serializable
data class Links(
    val link: Link? = null,
) {
    @Keep
    @Serializable
    data class Link(
        @SerialName("#text")
        val text: String? = null,
        val rel: String? = null,
        val href: String? = null,
    )
}


@Keep
@Serializable
class LastFmAlbum(
    val name: String,
    val artist: String? = null,
    val mbid: String? = null,
    val url: String,
    val image: List<LastFmImage> = emptyList(),
    val wiki: LastFmWikiData? = null,
    // val tags: Tags? = null,
    // val playcount: Long = 0,
    // val listeners: Long = 0,
    // val tracks: Tracks? = null,
) {
    @Keep
    @Serializable
    data class Tracks(
        val track: List<Track?>? = null,
    ) {
        @Keep
        @Serializable
        data class Track(
            // val streamable: Streamable? = null,
            val duration: Int = 0,
            val url: String? = null,
            val name: String? = null,
            // @SerialName("@attr")
            // val attr: Attr? = null,
        ) {
            @Keep
            @Serializable
            data class Streamable(
                val fulltrack: String? = null,
                @SerialName("#text")
                val text: String? = null,
            )
            @Keep
            @Serializable
            data class Attr(
                val rank: Int = -1,
            )
        }

    }
}

@Keep
@Serializable
class LastFmArtist(
    val name: String,
    val mbid: String? = null,
    val url: String,
    val image: List<LastFmImage> = emptyList(),
    val bio: LastFmWikiData? = null,
    // val streamable: String? = "",
    // val stats: Stats? = Stats(),
    // val tags: Tags? = Tags(),
) {
    @Keep
    @Serializable
    data class Stats(
        val listeners: Long? = null,
        val playcount: Long? = null,
    )

}

@Keep
@Serializable
class LastFmTrack(
    val name: String,
    val mbid: String? = null,
    val url: String,
    // val listeners: Long,
    // val playcount: Long,
    val duration: Long = 0,
    val artist: LastFmArtist? = null,
    val album: LastFmAlbum? = null,
    val wiki: LastFmWikiData? = null,
    val toptags: Tags? = null,
)

@Keep
@Serializable
data class Tags(
    val tag: List<Tag?>? = null,
) {
    @Keep
    @Serializable
    data class Tag(
        val name: String? = null,
        val url: String? = null,
    )
}

