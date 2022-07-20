/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.glide.artistimage

import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.MediaMetadataRetriever
import android.util.Log
import com.bumptech.glide.load.data.DataFetcher
import java.io.*
import kotlin.math.pow
import player.phonograph.BuildConfig
import player.phonograph.glide.audiocover.AudioFileCoverUtils
import player.phonograph.util.ImageUtil.resize

class ArtistImageFetchLogics(private val model: ArtistImage, private val ignoreMediaStore: Boolean) {
    private var stream: InputStream? = null

    fun fetch(callback: DataFetcher.DataCallback<in InputStream?>) {
        try {
            stream = getCover(model.albumCovers)
            callback.onDataReady(stream)
            return
        } catch (e: FileNotFoundException) {
            if (BuildConfig.DEBUG) Log.v(TAG, "No cover for " + model.artistName + " in MediaStore")
        }
        callback.onLoadFailed(Exception("No Available Photo For " + model.artistName))
    }
    fun cleanup() {
        // already cleaned up in loadData and ByteArrayInputStream will be GC'd
        runCatching {
            stream?.close()
        }
    }

    @Throws(FileNotFoundException::class)
    fun getCover(albumCovers: List<AlbumCover>): InputStream? {
        val retriever = MediaMetadataRetriever()
        val artistBitMapSize = 512

        val images: MutableMap<InputStream, Int> = HashMap()
        var result: InputStream? = null

        var streams: List<InputStream> = ArrayList()

        try {
            for (cover in albumCovers) {
                var picture: ByteArray? = null

                if (!ignoreMediaStore) {
                    retriever.setDataSource(cover.filePath)
                    picture = retriever.embeddedPicture
                }

                val stream: InputStream? =
                    if (picture != null) {
                        ByteArrayInputStream(picture)
                    } else {
                        AudioFileCoverUtils.fallback(cover.filePath)
                    }

                if (stream != null) {
                    images[stream] = cover.year
                }
            }

            val nbImages = images.size

            if (nbImages > 3) {
                streams = ArrayList(images.keys)
                var divisor = 1

                run {
                    var i = 1
                    while (i < nbImages && i.toDouble().pow(2.0) <= nbImages) {
                        divisor = i
                        ++i
                    }
                }
                divisor += 1

                var nbTiles = divisor.toDouble().pow(2.0)
                if (nbImages < nbTiles) {
                    divisor -= 1
                    nbTiles = divisor.toDouble().pow(2.0)
                }

                val resize = artistBitMapSize / divisor + 1
                val bitmap = Bitmap.createBitmap(
                    artistBitMapSize,
                    artistBitMapSize,
                    Bitmap.Config.RGB_565
                )

                val canvas = Canvas(bitmap)
                var x = 0
                var y = 0
                var i = 0

                while (i < streams.size && i < nbTiles) {
                    val bitmap1 = resize(streams[i], resize, resize)
                    canvas.drawBitmap(bitmap1, x.toFloat(), y.toFloat(), null)
                    x += resize
                    if (x >= artistBitMapSize) {
                        x = 0
                        y += resize
                    }
                    ++i
                }

                val byteArrayOutputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 0, byteArrayOutputStream)
                result = ByteArrayInputStream(byteArrayOutputStream.toByteArray())
            } else if (nbImages > 0) {
                // we return the last cover album of the artist
                var maxEntryYear: Map.Entry<InputStream, Int>? = null
                for (entry in images.entries) {
                    if (maxEntryYear == null || entry.value > maxEntryYear.value) {
                        maxEntryYear = entry
                    }
                }
                result = maxEntryYear?.key
                    ?: images.entries.iterator().next().key
            }
        } finally {
            retriever.release()
            try {
                for (stream in streams) {
                    stream.close()
                }
            } catch (e: IOException) {
                Log.v(TAG, "Error" + e.javaClass.simpleName)
            }
        }
        return result
    }

    companion object {
        private const val TAG = "ArtistImageFetcher"
    }
}
