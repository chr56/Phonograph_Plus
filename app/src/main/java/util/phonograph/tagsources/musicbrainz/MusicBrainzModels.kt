/*
 *  Copyright (c) 2022~2023 chr_56
 */

package util.phonograph.tagsources.musicbrainz

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed interface MusicBrainzModel

@Keep
@Serializable
data class MusicBrainzArtist(
    val id: String,
    val name: String,
    val type: String? = null,
    val gender: String? = null,
    val country: String? = null,
    val area: MusicBrainzArea? = null,
    @SerialName("begin-area")
    val beginArea: MusicBrainzArea? = null,
    @SerialName("life-span")
    val lifeSpan: LifeSpan? = null,
    val isnis: List<String>? = emptyList(),
    val tags: List<MusicBrainzTag>? = emptyList(),
    val relations: List<Relation>? = emptyList(),
    val aliases: List<MusicBrainzAlias>? = emptyList(),
    val score: Int? = null,
) : MusicBrainzModel {

    @Keep
    @Serializable
    data class LifeSpan(
        val begin: String? = null,
        val end: String? = null,
        val ended: Boolean? = null,
    )

    @Keep
    @Serializable
    data class Relation(
        val type: String?,
        val url: MusicBrainzUrl?,
    )

}

@Keep
@Serializable
data class MusicBrainzRelease(
    val id: String,
    val title: String,
    @SerialName("artist-credit")
    val artistCredit: List<MusicBrainzArtistCredit>? = emptyList(),
    @SerialName("release-group")
    val releaseGroup: MusicBrainzReleaseGroup? = null,
    val date: String? = null,
    val country: String? = null,
    val status: String? = null,
    @SerialName("release-events")
    val releaseEvents: List<MusicBrainzReleaseEvent>? = emptyList(),
    @SerialName("track-count")
    val trackCount: Int = 0,
    val media: List<Media>? = null,
    @SerialName("label-info")
    val labelInfo: List<LabelInfo>? = null,
    val barcode: String? = null,
    val asin: String? = null,
    @SerialName("text-representation")
    val textRepresentation: TextRepresentation? = null,
    val disambiguation: String? = null,
    val score: Int = 0,
    val count: Int = 0,
    val tags: List<MusicBrainzTag>? = null,
) : MusicBrainzModel {


    @Keep
    @Serializable
    data class LabelInfo(
        @SerialName("catalog-number")
        val catalogNumber: String? = null,
        val label: Label,
    ) {
        @Keep
        @Serializable
        data class Label(
            val id: String,
            val name: String,
        )
    }

    @Keep
    @Serializable
    data class Media(
        val format: String? = null,
        @SerialName("disc-count")
        val discCount: Int = 1,
        @SerialName("track-count")
        val trackCount: Int = 0,
        val tracks: List<MusicBrainzTrack>? = emptyList(),
    )

    @Keep
    @Serializable
    data class TextRepresentation(
        val language: String? = null,
        val script: String? = null,
    )
}

@Keep
@Serializable
data class MusicBrainzReleaseGroup(
    val id: String,
    val title: String?,
    @SerialName("artist-credit")
    val artistCredit: List<MusicBrainzArtistCredit>? = listOf(),
    val aliases: List<MusicBrainzAlias>? = listOf(),
    val count: Int = 0,
    val releases: List<MusicBrainzRelease>? = listOf(),
    @SerialName("primary-type")
    val primaryType: String? = null,
    @SerialName("primary-type-id")
    val primaryTypeId: String? = null,
    @SerialName("secondary-types")
    val secondaryTypes: List<String>? = listOf(),
    @SerialName("secondary-type-ids")
    val secondaryTypeIds: List<String>? = listOf(),
    @SerialName("first-release-date")
    val firstReleaseDate: String? = null,
    val disambiguation: String? = null,
    val tags: List<MusicBrainzTag>? = emptyList(),
) : MusicBrainzModel

@Keep
@Serializable
data class MusicBrainzRecording(
    val id: String,
    //val score: Int,
    val title: String,
    val length: Int? = null,
    @SerialName("artist-credit")
    val artistCredit: List<MusicBrainzArtistCredit> = listOf(),
    @SerialName("first-release-date")
    val firstReleaseDate: String? = null,
    val releases: List<MusicBrainzRelease>? = listOf(),
    val disambiguation: String? = null,
    val tags: List<MusicBrainzTag>? = listOf(),
) : MusicBrainzModel


@Keep
@Serializable
class MusicBrainzTrack(
    val id: String,
    val title: String,
    val recording: MusicBrainzRecording,
    val length: Int = 0,
    val position: Int = 0,
    @SerialName("artist-credit")
    val artistCredit: List<MusicBrainzArtistCredit>? = listOf(),
    val number: String = "",
) : MusicBrainzModel

@Keep
@Serializable
data class MusicBrainzArea(
    val id: String,
    val name: String,
    @SerialName("iso-3166-1-codes")
    val iso31661Codes: List<String> = emptyList(),
    val type: String? = null,
)

@Keep
@Serializable
data class MusicBrainzTag(
    val name: String = "",
    val count: Int,
)

@Keep
@Serializable
data class MusicBrainzAlias(
    val name: String,
    val type: String?,
    val locale: String?,
)

@Keep
@Serializable
data class MusicBrainzReleaseEvent(
    val date: String = "",
    val area: MusicBrainzArea? = null,
)

@Keep
@Serializable
data class MusicBrainzUrl(
    val id: String?,
    val resource: String?,
)


@Keep
@Serializable
data class MusicBrainzArtistCredit(
    val name: String,
    val artist: Artist,
    val joinphrase: String? = null,
) {
    @Keep
    @Serializable
    data class Artist(
        val id: String,
        val name: String,
        val type: String = NA,
        val disambiguation: String? = "",
    )
}

sealed interface MusicBrainzSearchResult

@Keep
@Serializable
data class MusicBrainzSearchResultArtists(
    val created: String,
    val count: Int,
    val offset: Int,
    val artists: List<MusicBrainzArtist>? = emptyList(),
) : MusicBrainzSearchResult


@Keep
@Serializable
data class MusicBrainzSearchResultReleases(
    val created: String,
    val count: Int,
    val offset: Int,
    val releases: List<MusicBrainzRelease>? = emptyList(),
) : MusicBrainzSearchResult

@Keep
@Serializable
data class MusicBrainzSearchResultReleasesGroup(
    val created: String,
    val count: Int,
    val offset: Int,
    @SerialName("release-groups")
    val releasesGroup: List<MusicBrainzReleaseGroup>? = emptyList(),
) : MusicBrainzSearchResult


@Keep
@Serializable
data class MusicBrainzSearchResultRecording(
    val created: String,
    val count: Int,
    val offset: Int,
    val recordings: List<MusicBrainzRecording>? = emptyList(),
) : MusicBrainzSearchResult


private const val NA = "N/A"