/*
 *  Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.tagsources.lastfm

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
class LastFmAlbumResponse(var album: LastFmAlbum)

@Keep
@Serializable
class LastFmArtistResponse(var artist: LastFmArtist)

@Keep
@Serializable
class LastFmTrackResponse(var track: LastFmTrack)

@Keep
@Serializable
class LastFmImage(
    @SerialName("#text")
    var text: String,
    var size: String?,
)

@Keep
@Serializable
class LastFmWikiData(
    var published: String? = null,
    var summary: String? = null,
    var content: String? = null,
    // var links: Links? = null,
)

@Keep
@Serializable
data class Links(
    var link: Link? = null,
) {
    @Keep
    @Serializable
    data class Link(
        @SerialName("#text")
        var text: String? = null,
        var rel: String? = null,
        var href: String? = null,
    )
}


@Keep
@Serializable
class LastFmAlbum(
    var name: String? = null,
    var artist: String? = null,
    var mbid: String? = null,
    var url: String,
    var image: List<LastFmImage> = emptyList(),
    var wiki: LastFmWikiData? = null,
    // var tags: Tags? = null,
    // var playcount: Long = 0,
    // var listeners: Long = 0,
    // var tracks: Tracks? = null,
) {
    @Keep
    @Serializable
    data class Tracks(
        var track: List<Track?>? = null,
    ) {
        @Keep
        @Serializable
        data class Track(
            // var streamable: Streamable? = null,
            var duration: Int = 0,
            var url: String? = null,
            var name: String? = null,
            // @SerialName("@attr")
            // var attr: Attr? = null,
        ) {
            @Keep
            @Serializable
            data class Streamable(
                var fulltrack: String? = null,
                @SerialName("#text")
                var text: String? = null,
            )
            @Keep
            @Serializable
            data class Attr(
                var rank: Int = -1,
            )
        }

    }
}

@Keep
@Serializable
class LastFmArtist(
    var name: String,
    var mbid: String? = null,
    var url: String,
    var image: List<LastFmImage> = emptyList(),
    var bio: LastFmWikiData? = null,
    // var streamable: String? = "",
    // var stats: Stats? = Stats(),
    // var tags: Tags? = Tags(),
) {
    @Keep
    @Serializable
    data class Stats(
        var listeners: Long? = null,
        var playcount: Long? = null,
    )

}

@Keep
@Serializable
class LastFmTrack(
    var name: String,
    var mbid: String? = null,
    var url: String,
    // var listeners: Long,
    // var playcount: Long,
    var duration: Long = 0,
    var artist: LastFmArtist? = null,
    var album: LastFmAlbum? = null,
    var wiki: LastFmWikiData? = null,
    var toptags: Tags? = null,
)

@Keep
@Serializable
data class Tags(
    var tag: List<Tag?>? = null,
) {
    @Keep
    @Serializable
    data class Tag(
        var name: String? = null,
        var url: String? = null,
    )
}

