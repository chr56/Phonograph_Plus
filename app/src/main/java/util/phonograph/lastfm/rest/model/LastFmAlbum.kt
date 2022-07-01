package util.phonograph.lastfm.rest.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class LastFmAlbum {
    @Expose
    var album: Album? = null

    class Album {

        @Expose
        var url: String? = null

        @Expose
        var image: List<Image> = ArrayList()

        @Expose
        var wiki: Wiki? = null

        inner class Wiki {
            @Expose
            var content: String = ""
        }

        class Image {
            @SerializedName("#text")
            @Expose
            var text: String = ""

            @Expose
            var size: String = ""
        }
    }
}
