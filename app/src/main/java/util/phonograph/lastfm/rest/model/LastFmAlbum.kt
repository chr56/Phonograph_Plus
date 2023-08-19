package util.phonograph.lastfm.rest.model

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
class LastFmAlbum(var album: Album)

@Keep
@Serializable
class Album(
    var url: String,
    var image: List<LastFmImage> = ArrayList(),
    var wiki: Wiki? = null,
)

@Keep
@Serializable
class Wiki(
    var published: String?,
    var summary: String?,
    var content: String?,
)
