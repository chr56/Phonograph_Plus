/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.coil.retriever

import player.phonograph.mechanism.setting.CoilImageConfig
import player.phonograph.model.coil.ImageSource
import player.phonograph.model.coil.ImageSourceConfig
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

private var sourceConfig: ImageSourceConfig? = null
private var sourceConfigJob: Job? = null

suspend fun collectSourceConfig(context: Context): ImageSourceConfig = sourceConfig ?: run {
    val imageSourceConfigFlow = CoilImageConfig.currentImageSourceConfig(context)
    if (sourceConfigJob == null) {
        sourceConfigJob = CoroutineScope(Dispatchers.IO).launch {
            imageSourceConfigFlow.collect { sourceConfig = it }
        }
    } else {
        Log.d("ImageSourceConfig", "`collectSourceConfig` is already called once!")
    }
    return withContext(Dispatchers.IO) { imageSourceConfigFlow.first() }
}