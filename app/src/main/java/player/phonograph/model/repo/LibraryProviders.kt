/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.model.repo

interface MusicLibraryBackendOptions {

    val useMediaStoreSongs: Boolean
    val useMediaStoreArtists: Boolean
    val useMediaStoreAlbums: Boolean
    val useMediaStoreGenres: Boolean

    val syncBasicDatabase: Boolean
    val syncWithGenres: Boolean
}

const val PROVIDER_MEDIASTORE_DIRECT = "mediastore"
const val PROVIDER_INTERNAL_DATABASE = "internal_database"

const val SYNC_MODE_STANDARD = "standard"
const val SYNC_MODE_EXCLUDE_GENRES = "exclude_genres"

val DEFAULT_TAG_ABBR_FEATURES_ARTISTS: Set<String>
    get() = setOf(
        "feat.",
    )

val DEFAULT_TAG_SEPARATORS_ARTISTS: Set<String>
    get() = setOf(
        ";",
        " / ",
        " & ",
        "ft. ",
    )

val DEFAULT_TAG_SEPARATORS_GENRES: Set<String>
    get() = setOf(
        ";",
        ", ",
        " & ",
    )