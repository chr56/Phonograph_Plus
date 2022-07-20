/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.glide.audiocover

import android.media.MediaMetadataRetriever
import android.util.Log
import com.bumptech.glide.load.data.DataFetcher
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.io.InputStream
import player.phonograph.BuildConfig

class AudioFileCoverFetchLogics(val model: AudioFileCover) {
    private var stream: InputStream? = null

    fun fetch(callback: DataFetcher.DataCallback<in InputStream>?): InputStream? {
        val retriever = MediaMetadataRetriever()

        val picture: ByteArray? =
            try {
                retriever.setDataSource(model.filePath)
                retriever.embeddedPicture
            } catch (ignored: Exception) {
                null
            }

        if (picture != null) {
            stream = ByteArrayInputStream(picture)
            callback?.onDataReady(stream)
            return stream
        } else {
            if (BuildConfig.DEBUG) Log.v(
                AudioFileCoverFetcher.TAG,
                "No cover for $model in MediaStore"
            )
        }
        // use fallback
        try {
            stream = AudioFileCoverUtils.fallback(model.filePath)
            callback?.onDataReady(stream)
            return stream
        } catch (e: FileNotFoundException) {
            if (BuildConfig.DEBUG) Log.v(
                AudioFileCoverFetcher.TAG,
                "No cover for" + model + "in File"
            )
        }
        // so onLoadFailed
        callback?.onLoadFailed(Exception("No Available Cover Picture For $model"))
        return null
    }
    fun cleanup() {
        // already cleaned up in loadData and ByteArrayInputStream will be GC'd
        runCatching {
            stream?.close()
        }
    }
}
