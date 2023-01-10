/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.model

sealed interface ImageSource {
    /**
     * for persistence
     */
    val key: String

    object MediaStore : ImageSource {
        override val key: String get() = IMAGE_SOURCE_MEDIA_STORE
    }

    object MediaMetadataRetriever : ImageSource {
        override val key: String get() = IMAGE_SOURCE_MEDIA_METADATA_RETRIEVER
    }

    object JAudioTagger : ImageSource {
        override val key: String get() = IMAGE_SOURCE_J_AUDIO_TAGGER
    }

    object ExternalFile : ImageSource {
        override val key: String get() = IMAGE_SOURCE_EXTERNAL_FILE
    }

    companion object {
        fun fromKey(key: String): ImageSource {
            return when (key) {
                IMAGE_SOURCE_MEDIA_STORE              -> MediaStore
                IMAGE_SOURCE_MEDIA_METADATA_RETRIEVER -> MediaMetadataRetriever
                IMAGE_SOURCE_J_AUDIO_TAGGER           -> JAudioTagger
                IMAGE_SOURCE_EXTERNAL_FILE            -> ExternalFile
                else                                  -> throw IllegalArgumentException("Unknown ImageSource: $key")
            }
        }
    }
}

const val IMAGE_SOURCE_MEDIA_STORE = "MediaStore"
const val IMAGE_SOURCE_MEDIA_METADATA_RETRIEVER = "MediaMetadataRetriever"
const val IMAGE_SOURCE_J_AUDIO_TAGGER = "JAudioTagger"
const val IMAGE_SOURCE_EXTERNAL_FILE = "ExternalFile"