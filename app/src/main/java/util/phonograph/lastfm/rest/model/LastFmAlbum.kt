package util.phonograph.lastfm.rest.model

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
class LastFmAlbum {

    var album: Album? = null

    @Keep
    @Serializable
    class Album {

        var url: String? = null

        var image: List<Image> = ArrayList()

        var wiki: Wiki? = null

        @Keep
        @Serializable
        class Wiki {
            var content: String = ""
        }

        @Keep
        @Serializable
        class Image {
            @SerialName("#text")
            var text: String = ""
            var size: String = ""
        }
    }
}
