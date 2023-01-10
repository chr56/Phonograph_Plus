/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.coil.retriever

import player.phonograph.model.IMAGE_SOURCE_EXTERNAL_FILE
import player.phonograph.model.IMAGE_SOURCE_J_AUDIO_TAGGER
import player.phonograph.model.IMAGE_SOURCE_MEDIA_METADATA_RETRIEVER
import player.phonograph.model.IMAGE_SOURCE_MEDIA_STORE
import player.phonograph.model.ImageSource
import player.phonograph.util.preferences.CoilImageSourceConfig

internal val retrieverFromConfig: List<ImageRetriever>
    get() {
        val config = CoilImageSourceConfig.currentConfig
        return config.list.filter { it.enabled }.map { it.imageSource.retriever() }
    }

internal fun ImageSource.retriever(): ImageRetriever = when (key) {
    IMAGE_SOURCE_MEDIA_STORE              -> MediaStoreRetriever()
    IMAGE_SOURCE_MEDIA_METADATA_RETRIEVER -> MediaMetadataRetriever()
    IMAGE_SOURCE_J_AUDIO_TAGGER           -> JAudioTaggerRetriever()
    IMAGE_SOURCE_EXTERNAL_FILE            -> ExternalFileRetriever()
    else                                  -> throw IllegalArgumentException("Unknown ImageSource: $key")
}