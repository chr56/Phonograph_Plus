/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.coil.artist

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever
import android.util.Log
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import coil.size.Size
import lib.phonograph.BuildConfig.DEBUG
import player.phonograph.coil.*
import player.phonograph.coil.readFromMediaStore
import player.phonograph.coil.retrieveFromJAudioTagger
import player.phonograph.coil.retrieveFromMediaMetadataRetriever
import player.phonograph.util.module.IgnoreMediaStorePreference

class ArtistImageFetcher(val data: ArtistImage, val context: Context, val size: Size) : Fetcher {

    class Factory : Fetcher.Factory<ArtistImage> {
        override fun create(data: ArtistImage, options: Options, imageLoader: ImageLoader): Fetcher? {
            return ArtistImageFetcher(data, options.context, options.size)
        }
    }

    override suspend fun fetch(): FetchResult? {
        // returning bitmap
        var bitmap: Bitmap? = null

        // first check if the custom artist image exist
        val file = CustomArtistImageStore.instance(context)
            .getCustomArtistImageFile(data.artistId, data.artistName)
        if (file != null) {
            return readJEPGFile(file, "#${data.artistId}#${data.artistName}")
        }

        // then choose an AlbumCover as ArtistImage

        //
        // 0: lookup for MediaStore
        //
        if (!IgnoreMediaStorePreference.ignoreMediaStore) {
            for (cover in data.albumCovers) {
                val result = readFromMediaStore(cover.id, context, size)
                if (result != null) return result
            }
        }

        //
        // 1: From Android MediaMetadataRetriever
        //
        MediaMetadataRetriever().use { retriever ->
            for (cover in data.albumCovers) {
                bitmap = retrieveFromMediaMetadataRetriever(cover.filePath, retriever)
                if (bitmap != null) break
            }
        }
        //
        // 2: Use JAudioTagger to get embedded high resolution album art if there is any
        //
        if (bitmap == null) {
            for (cover in data.albumCovers) {
                bitmap = retrieveFromJAudioTagger(cover.filePath)
                if (bitmap != null) break
            }
        }

        //
        // 3: Look for album art in external files
        //
        // todo

        //
        // Collect Result
        //
        return if (bitmap != null) {
            DrawableResult(
                BitmapDrawable(context.resources, bitmap),
                false,
                DataSource.DISK
            )
        } else {
            if (DEBUG) {
                Log.v(TAG, "No any cover for artist ${data.artistName}(${data.artistId})")
            }
            null
        }
    }

    companion object {
        private const val TAG = "ArtistImageFetcher"
    }
}
