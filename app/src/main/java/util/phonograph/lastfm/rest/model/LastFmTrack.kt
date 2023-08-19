/*
 *  Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.lastfm.rest.model

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
class LastFmTrack(var track: Track)

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
