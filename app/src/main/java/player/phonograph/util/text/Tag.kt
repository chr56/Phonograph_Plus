/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.util.text

import player.phonograph.model.Song

fun splitMultiTag(source: String): Collection<String> {
    if (source.isEmpty()) return emptySet()
    return source.trim(Char::isWhitespace).split(";", " / ", " & ", "ft. ").map { it.trimStart() }
}

fun parseArtist(raw: String?): Set<String> =
    if (!raw.isNullOrEmpty()) {
        splitMultiTag(raw).toSet()
    } else {
        emptySet()
    }

fun extractFeatureArtists(raw: String?): Set<String> {
    if (raw.isNullOrEmpty()) return emptySet()

    val result = raw.split("feat.", ignoreCase = true, limit = 2)
    val artist = result.getOrNull(1) ?: return emptySet()

    val names = artist.trimEnd().substringBefore(')')
    return parseArtist(names)
}


class SongRelationship private constructor(
    val song: Song,
    val album: String?,
    val albumId: Long,
    val defaultArtists: Set<String>,
    val albumArtists: Set<String>,
    val composerArtists: Set<String>,
) {
    val artists get() = defaultArtists + albumArtists + composerArtists

    companion object {
        fun solve(song: Song): SongRelationship {

            val standardArtists = parseArtist(song.artistName)
            val albumArtists = parseArtist(song.albumArtistName)
            val composerArtists = parseArtist(song.composer)

            val defaultArtists =
                standardArtists + extractFeatureArtists(song.title) + extractFeatureArtists(song.albumName)

            return SongRelationship(song, song.albumName, song.albumId, defaultArtists, albumArtists, composerArtists)
        }
    }
}

class AccumulatedSongRelationship private constructor(
    val songs: Set<Song>,
    val albums: Map<Long, String?>,
    val artists: Set<String>,
) {
    val total: Int = songs.size + albums.size + artists.size

    companion object {
        fun reduce(items: List<SongRelationship>): AccumulatedSongRelationship {
            val songs = items.map { it.song }.toSet()
            val albums = items.associate { it.albumId to it.album }
            val defaultArtists = items.flatMap { it.defaultArtists }.toSet()
            val albumArtists = items.flatMap { it.albumArtists }.toSet()
            val composerArtists = items.flatMap { it.composerArtists }.toSet()

            return AccumulatedSongRelationship(
                songs = songs,
                albums = albums,
                artists = defaultArtists + albumArtists + composerArtists,
            )
        }
    }
}