package util.phonograph.lastfm.rest.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class LastFmAlbum {

    var album: Album? = null

    @Serializable
    class Album {

        var url: String? = null

        var image: List<Image> = ArrayList()

        var wiki: Wiki? = null

        @Serializable
        class Wiki {
            var content: String = ""
        }

        @Serializable
        class Image {
            @SerialName("#text")
            var text: String = ""
            var size: String = ""
        }
    }
}
