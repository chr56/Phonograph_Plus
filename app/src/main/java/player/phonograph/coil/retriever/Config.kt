/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.coil.retriever

import player.phonograph.mechanism.setting.CoilImageConfig
import player.phonograph.model.coil.ImageSource

internal val retrieverFromConfig: List<ImageRetriever>
    get() {
        val config = CoilImageConfig.currentImageSourceConfig
        return config.sources.filter { it.enabled }.map { it.imageSource.retriever() }
    }

internal fun ImageSource.retriever(): ImageRetriever = when (key) {
    ImageSource.IMAGE_SOURCE_MEDIA_STORE              -> MediaStoreRetriever()
    ImageSource.IMAGE_SOURCE_MEDIA_METADATA_RETRIEVER -> MediaMetadataRetriever()
    ImageSource.IMAGE_SOURCE_J_AUDIO_TAGGER           -> JAudioTaggerRetriever()
    ImageSource.IMAGE_SOURCE_EXTERNAL_FILE            -> ExternalFileRetriever()
    else                                  -> throw IllegalArgumentException("Unknown ImageSource: $key")
}