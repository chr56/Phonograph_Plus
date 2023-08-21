/*
 *  Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.tagsources.lastfm

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
class LastFmAlbum(var album: Album)

@Keep
@Serializable
class LastFmArtist(var artist: Artist)

@Keep
@Serializable
class LastFmTrack(var track: Track)

@Keep
@Serializable
class LastFmImage(
    @SerialName("#text")
    var text: String,
    var size: String?,
)

@Keep
@Serializable
class Album(
    var url: String,
    var image: List<LastFmImage> = ArrayList(),
    var wiki: Wiki? = null,
)
@Keep
@Serializable
class Artist(
    var url: String,
    var image: List<LastFmImage> = ArrayList(),
    var bio: Bio? = null,
)

@Keep
@Serializable
class Track(
    var name: String,
    var mbid: String,
    var url: String,
    var listeners: Long,
    var playcount: Long,
    var artist: Artist,
    var album: Album,
    var wiki: Wiki,
)

@Keep
@Serializable
class Wiki(
    var published: String?,
    var summary: String?,
    var content: String?,
)

@Keep
@Serializable
class Bio(var content: String = "")
