/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.mechanism.metadata

import player.phonograph.model.Genre
import player.phonograph.model.Song
import player.phonograph.model.repo.DEFAULT_TAG_ABBR_FEATURES_ARTISTS
import player.phonograph.model.repo.DEFAULT_TAG_SEPARATORS_ARTISTS
import player.phonograph.model.repo.DEFAULT_TAG_SEPARATORS_GENRES
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import android.content.Context

class RelationshipResolver private constructor(
    var enableFeatureArtistsExtraction: Boolean = true,
    var artistSeparators: Array<String> = DEFAULT_TAG_SEPARATORS_ARTISTS.toTypedArray(),
    var featureArtistsAbbr: Array<String> = DEFAULT_TAG_ABBR_FEATURES_ARTISTS.toTypedArray(),
    var genresSeparators: Array<String> = DEFAULT_TAG_SEPARATORS_GENRES.toTypedArray(),
) {
    companion object {
        fun default(): RelationshipResolver = RelationshipResolver()

        suspend fun fromSettings(context: Context): RelationshipResolver {
            val artistSeparators = Setting(context)[Keys.tagSeparatorsArtists].read()
            val abbrFeatureArtists = Setting(context)[Keys.tagAbbrFeatureArtists].read()
            return RelationshipResolver(
                enableFeatureArtistsExtraction = true,
                artistSeparators = artistSeparators.toTypedArray(),
                featureArtistsAbbr = abbrFeatureArtists.toTypedArray(),
            )
        }
    }

    fun splitJointTag(source: String?, separators: Array<String>): Collection<String> {
        if (source.isNullOrEmpty()) return emptySet()
        return source.trim(Char::isWhitespace).split(*separators).map { it.trimStart() }
    }

    fun extractFeatureArtists(raw: String?): Set<String> {
        if (raw.isNullOrEmpty()) return emptySet()

        val result = raw.split(*featureArtistsAbbr, ignoreCase = true, limit = 2)
        val artist = result.getOrNull(1) ?: return emptySet()

        val names = artist.trimEnd().substringBefore(')')
        return splitJointTag(names, separators = artistSeparators).toSet()
    }

    fun split(genre: Genre) = splitJointTag(genre.name, genresSeparators)

    fun solve(song: Song): SongRelationship {
        val defaultArtists = splitJointTag(song.artistName, separators = artistSeparators).toSet()
        val albumArtists = splitJointTag(song.albumArtistName, separators = artistSeparators).toSet()
        val composerArtists = splitJointTag(song.composer, separators = artistSeparators).toSet()
        val featureArtists =
            if (enableFeatureArtistsExtraction) {
                extractFeatureArtists(song.title) + extractFeatureArtists(song.albumName)
            } else {
                emptySet()
            }

        return SongRelationship(
            song,
            song.albumName,
            song.albumId,
            defaultArtists,
            albumArtists,
            composerArtists,
            featureArtists,
        )
    }

    fun reduce(items: Collection<SongRelationship>): AccumulatedSongRelationship {
        val songs = items.map { it.song }.toSet()
        val albums = items.associate { it.albumId to it.albumName }
        val defaultArtists = items.flatMap { it.defaultArtists }.toSet()
        val albumArtists = items.flatMap { it.albumArtists }.toSet()
        val composerArtists = items.flatMap { it.composerArtists }.toSet()
        val featureArtists = items.flatMap { it.featureArtists }.toSet()

        return AccumulatedSongRelationship(
            songs = songs,
            albums = albums,
            artists = defaultArtists + albumArtists + composerArtists + featureArtists,
        )
    }


    class SongRelationship(
        val song: Song,
        val albumName: String?,
        val albumId: Long,
        val defaultArtists: Set<String>,
        val albumArtists: Set<String>,
        val composerArtists: Set<String>,
        val featureArtists: Set<String>,
    ) {
        val artists get() = defaultArtists + albumArtists + composerArtists + featureArtists
    }

    class AccumulatedSongRelationship(
        val songs: Set<Song>,
        val albums: Map<Long, String?>,
        val artists: Set<String>,
    ) {
        val total: Int = songs.size + albums.size + artists.size
    }

}


