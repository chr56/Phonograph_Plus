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
    var published: String?,
    var summary: String?,
    var content: String?,
)

@Keep
@Serializable
class LastFmAlbum(
    var url: String,
    var image: List<LastFmImage> = ArrayList(),
    var wiki: LastFmWikiData? = null,
)
@Keep
@Serializable
class LastFmArtist(
    var url: String,
    var image: List<LastFmImage> = ArrayList(),
    var bio: LastFmWikiData? = null,
)

@Keep
@Serializable
class LastFmTrack(
    var name: String,
    var mbid: String,
    var url: String,
    var listeners: Long,
    var playcount: Long,
    var artist: LastFmArtist,
    var album: LastFmAlbum,
    var wiki: LastFmWikiData,
)
