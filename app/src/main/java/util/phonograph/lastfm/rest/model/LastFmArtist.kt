package util.phonograph.lastfm.rest.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class LastFmArtist {
    @Expose
    var artist: Artist? = null

    class Artist {
        @Expose
        var image: List<Image> = ArrayList()

        @Expose
        var bio: Bio? = null

        inner class Bio {
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
