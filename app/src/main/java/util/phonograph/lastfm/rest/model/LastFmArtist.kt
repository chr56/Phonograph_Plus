package util.phonograph.lastfm.rest.model

import kotlinx.serialization.Serializable

@Serializable
class LastFmArtist {

    var artist: Artist? = null

    @Serializable
    class Artist {

        var url: String? = null

        var image: List<LastFmImage> = ArrayList()

        var bio: Bio? = null

        @Serializable
        class Bio {
            var content: String = ""
        }
    }
}
