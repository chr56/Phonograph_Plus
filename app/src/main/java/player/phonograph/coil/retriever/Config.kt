/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.coil.retriever

import coil.request.Parameters
import player.phonograph.coil.PARAMETERS_KEY_IMAGE_SOURCE_CONFIG
import player.phonograph.model.coil.ImageSource
import player.phonograph.model.coil.ImageSourceConfig
import player.phonograph.settings.Keys
import player.phonograph.settings.Setting
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun ImageSource.retriever(): ImageRetriever = when (key) {
    ImageSource.IMAGE_SOURCE_MEDIA_STORE              -> ImageRetrievers.MediaStoreRetriever()
    ImageSource.IMAGE_SOURCE_MEDIA_METADATA_RETRIEVER -> ImageRetrievers.MediaMetadataRetriever()
    ImageSource.IMAGE_SOURCE_J_AUDIO_TAGGER           -> ImageRetrievers.JAudioTaggerRetriever()
    ImageSource.IMAGE_SOURCE_EXTERNAL_FILE            -> ImageRetrievers.ExternalFileRetriever()
    else                                              -> throw IllegalArgumentException("Unknown ImageSource: $key")
}

fun Parameters.retrievers(): List<ImageRetriever> =
    retrieversFrom(value<ImageSourceConfig>(PARAMETERS_KEY_IMAGE_SOURCE_CONFIG))

fun retrieversFrom(config: ImageSourceConfig?): List<ImageRetriever> {
    if (config == null) {
        Log.w("Coil", "No ImageSourceConfig!")
        return emptyList()
    }
    return config.sources.filter { it.enabled }.map { it.imageSource.retriever() }
}


private var sourceConfig: ImageSourceConfig? = null
private var sourceConfigJob: Job? = null

suspend fun collectSourceConfig(context: Context): ImageSourceConfig = sourceConfig ?: run {
    val imageSourceConfigFlow = Setting(context).Composites[Keys.imageSourceConfig].flow()
    if (sourceConfigJob == null) {
        sourceConfigJob = CoroutineScope(Dispatchers.IO).launch {
            imageSourceConfigFlow.collect { sourceConfig = it }
        }
    } else {
        Log.d("ImageSourceConfig", "`collectSourceConfig` is already called once!")
    }
    return withContext(Dispatchers.IO) { imageSourceConfigFlow.first() }
}

private var isCacheEnabled: Boolean? = null
private var isCacheEnabledJob: Job? = null

suspend fun collectCacheSetting(context: Context): Boolean = isCacheEnabled ?: run {
    val enableImageCacheFlow = Setting(context)[Keys.imageCache].flow
    if (isCacheEnabledJob == null) {
        isCacheEnabledJob = CoroutineScope(Dispatchers.IO).launch {
            enableImageCacheFlow.collect { isCacheEnabled = it }
        }
    } else {
        Log.d("ImageCache", "`isCacheEnabled` is already called once!")
    }
    return withContext(Dispatchers.IO) { enableImageCacheFlow.first() }
}