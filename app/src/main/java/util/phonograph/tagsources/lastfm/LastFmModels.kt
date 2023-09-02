/*
 *  Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.tagsources.lastfm

import androidx.annotation.Keep
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.serializer

@Keep
@Serializable
class LastFmAlbumResponse(val album: LastFmAlbum)

@Keep
@Serializable
class LastFmArtistResponse(val artist: LastFmArtist)

@Keep
@Serializable
class LastFmTrackResponse(val track: LastFmTrack)

@Keep
@Serializable
class LastFmImage(
    @SerialName("#text")
    val text: String,
    val size: String?,
)

@Keep
@Serializable
class LastFmWikiData(
    val published: String? = null,
    val summary: String? = null,
    val content: String? = null,
    // val links: Links? = null,
)

@Keep
@Serializable
data class Links(
    val link: Link? = null,
) {
    @Keep
    @Serializable
    data class Link(
        @SerialName("#text")
        val text: String? = null,
        val rel: String? = null,
        val href: String? = null,
    )
}


@Keep
@Serializable
class LastFmAlbum(
    val name: String,
    val artist: String? = null,
    val mbid: String? = null,
    val url: String,
    val image: List<LastFmImage> = emptyList(),
    val wiki: LastFmWikiData? = null,
    val tags: Tags? = Tags(),
    // val playcount: Long = 0,
    // val listeners: Long = 0,
    val tracks: Tracks? = null,
) {
    @Keep
    @Serializable(with = Tracks.Serializer::class)
    data class Tracks(
        val track: List<Track>? = null,
    ) {
        @Keep
        @Serializable
        data class Track(
            // val streamable: Streamable? = null,
            val duration: Int = 0,
            val url: String = "",
            val name: String = "",
            // @SerialName("@attr")
            // val attr: Attr? = null,
        ) {
            @Keep
            @Serializable
            data class Streamable(
                val fulltrack: String? = null,
                @SerialName("#text")
                val text: String? = null,
            )
            @Keep
            @Serializable
            data class Attr(
                val rank: Int = -1,
            )
        }

        class Serializer : KSerializer<Tracks> {
            override val descriptor: SerialDescriptor
                get() = buildClassSerialDescriptor("LastFmAlbumTrack")

            override fun deserialize(decoder: Decoder): Tracks {
                decoder as JsonDecoder
                val jsonElement = decoder.decodeJsonElement()
                return when (val content = (jsonElement as? JsonObject)?.get("track")) {
                    is JsonObject -> {
                        // single track
                        val track = decoder.json.decodeFromJsonElement<Track>(content)
                        Tracks(listOf(track))
                    }

                    is JsonArray  -> {
                        // multiple track
                        val json = decoder.json
                        val tracks = content.map { item ->
                            json.decodeFromJsonElement<Track>(item)
                        }
                        Tracks(tracks)
                    }

                    else          -> Tracks(null)
                }
            }

            override fun serialize(encoder: Encoder, value: Tracks) {
                encoder as JsonDecoder
                val json = encoder.json
                val tracks = value.track
                if (tracks != null) {
                    val elements = tracks.map { track ->
                        json.encodeToJsonElement(track)
                    }
                    encoder.encodeSerializableValue(json.serializersModule.serializer(), elements)
                } else {
                    encoder.encodeSerializableValue(json.serializersModule.serializer(), JsonArray(emptyList()))
                }
            }

        }
    }
}

@Keep
@Serializable
class LastFmArtist(
    val name: String,
    val mbid: String? = null,
    val url: String,
    val image: List<LastFmImage> = emptyList(),
    val bio: LastFmWikiData? = null,
    // val streamable: String? = "",
    // val stats: Stats? = Stats(),
    val tags: Tags? = Tags(),
) {
    @Keep
    @Serializable
    data class Stats(
        val listeners: Long? = null,
        val playcount: Long? = null,
    )

}

@Keep
@Serializable
class LastFmTrack(
    val name: String,
    val mbid: String? = null,
    val url: String,
    // val listeners: Long,
    // val playcount: Long,
    val duration: Long = 0,
    val artist: LastFmArtist? = null,
    val album: LastFmAlbum? = null,
    val wiki: LastFmWikiData? = null,
    val toptags: Tags? = Tags(),
)

/**
 * notices that this might be empty string instead of json object containing json array naming `tag`
 */
@Keep
@Serializable(with = Tags.Serializer::class)
data class Tags(
    val tag: List<Tag> = emptyList(),
) {
    @Keep
    @Serializable
    data class Tag(
        val name: String,
        val url: String? = null,
    )

    class Serializer : KSerializer<Tags> {

        override fun deserialize(decoder: Decoder): Tags {
            decoder as JsonDecoder
            val json = decoder.json
            return try {
                when (val jsonElement = decoder.decodeJsonElement()) {
                    is JsonObject -> {
                        val tags = (jsonElement["tag"] as? JsonArray)?.map { tag ->
                            json.decodeFromJsonElement<Tag>(tag)
                        }
                        Tags(tags ?: emptyList())
                    }

                    else          -> Tags()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Tags()
            }
        }

        override fun serialize(encoder: Encoder, value: Tags) {
            encoder as JsonDecoder
            val json = encoder.json
            val tags = value.tag
            val elements = tags.map { tag ->
                json.encodeToJsonElement(tag)
            }
            encoder.encodeSerializableValue(json.serializersModule.serializer(), elements)
        }

        override val descriptor: SerialDescriptor
            get() = buildClassSerialDescriptor("LastFmTag")
    }
}

