/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.repo

const val PROVIDER_MEDIASTORE_DIRECT = "mediastore_direct"
const val PROVIDER_MEDIASTORE_MIRROR = "mediastore_mirror"
const val PROVIDER_MEDIASTORE_PARSED = "mediastore_parsed"

val LIBRARY_PROVIDERS: List<String>
    get() = listOf(
        PROVIDER_MEDIASTORE_DIRECT,
        PROVIDER_MEDIASTORE_MIRROR,
        PROVIDER_MEDIASTORE_PARSED
    )

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