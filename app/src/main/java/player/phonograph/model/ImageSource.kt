/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.model

import player.phonograph.R
import android.content.Context

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

    /**
     * return string for translate
     */
    fun displayString(context: Context): String = context.getString(
        when (key) {
            IMAGE_SOURCE_MEDIA_STORE              -> R.string.image_source_media_store
            IMAGE_SOURCE_MEDIA_METADATA_RETRIEVER -> R.string.image_source_media_metadata_retriever
            IMAGE_SOURCE_J_AUDIO_TAGGER           -> R.string.image_source_jaudio_tagger
            IMAGE_SOURCE_EXTERNAL_FILE            -> R.string.image_source_external_file
            else                                  -> throw IllegalStateException("Unknown ImageSource: $key")
        }
    )

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