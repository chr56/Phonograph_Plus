package util.phonograph.lastfm.rest.model

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
class LastFmArtist(var artist: Artist)

@Keep
@Serializable
class Artist(
    var url: String,
    var image: List<LastFmImage> = ArrayList(),
    var bio: Bio? = null,
)

@Keep
@Serializable
class Bio(var content: String = "")