/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.coil.audiofile

import coil.fetch.FetchResult
import coil.size.Size
import player.phonograph.coil.retriever.CacheStore
import player.phonograph.coil.retriever.ImageRetriever
import player.phonograph.util.debug
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "ImageRetriever"
internal fun retrieveAudioFile(
    retrievers: List<ImageRetriever>,
    audioFile: AudioFile,
    context: Context,
    size: Size,
    rawImage: Boolean,
): FetchResult? {

    val noImage = CacheStore.AudioFiles(context).isNoImage(audioFile)
    if (noImage) return null // skipping

    for (retriever in retrievers) {
        val cached = CacheStore.AudioFiles(context).get(audioFile, retriever.name)
        if (cached != null) {
            debug {
                Log.v(TAG, "Image was read from cache of ${retriever.name} for file $audioFile")
            }
            return cached
        }
        val noSpecificImage = CacheStore.AudioFiles(context).isNoImage(audioFile, retriever.name)
        if (noSpecificImage) continue
        val result = retriever.retrieve(audioFile.path, audioFile.albumId, context, size, rawImage)
        if (result == null) {
            debug {
                Log.v(TAG, "Image not available from ${retriever.name} for file $audioFile")
            }
            CacheStore.AudioFiles(context).markNoImage(audioFile, retriever.name)
            continue
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                CacheStore.AudioFiles(context).set(audioFile, result, retriever.name)
            }
            return result
        }
    }
    debug {
        Log.v(TAG, "No any cover for file $audioFile")
    }
    CacheStore.AudioFiles(context).markNoImage(audioFile)
    return null
}