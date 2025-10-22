/*
 * Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.coil.retriever

import coil.request.Parameters
import player.phonograph.App
import player.phonograph.coil.PARAMETERS_KEY_IMAGE_SOURCE_CONFIG
import player.phonograph.model.coil.ImageSource
import player.phonograph.model.coil.ImageSourceConfig
import player.phonograph.settings.PreferenceKey
import player.phonograph.settings.Setting
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class SettingCollector<T>(val preferenceKey: () -> PreferenceKey<T>) {

    private var current: T? = null
    private var job: Job? = null

    suspend fun retrieve(context: Context): T = current ?: init(context)

    private suspend fun init(context: Context): T {
        val key = preferenceKey()
        val flow = Setting(context)[key].flow
        if (job == null) {
            job = coroutineScope(context).launch {
                flow.collect { current = it }
            }
        } else {
            Log.d("ImageCache", "$key is already init once before!")
        }
        return withContext(Dispatchers.IO) { flow.first() }
    }

    private fun coroutineScope(context: Context): CoroutineScope {
        val appScope = (context.applicationContext as? App)?.appScope
        return appScope ?: (scope ?: CoroutineScope(Dispatchers.IO).also {
            scope = it
        })
    }

    private var scope: CoroutineScope? = null
}

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