/*
 *  Copyright (c) 2022~2025 chr_56
 */

package player.phonograph.model.repo

const val PROVIDER_MEDIASTORE_DIRECT = "mediastore"
const val PROVIDER_MEDIASTORE_PARSED = "mediastore_parsed"

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