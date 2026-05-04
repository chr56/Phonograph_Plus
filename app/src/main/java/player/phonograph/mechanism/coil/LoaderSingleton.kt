/*
 *  Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.mechanism.coil

import coil.ImageLoader
import player.phonograph.mechanism.coil.album.AlbumImageFetcher
import player.phonograph.mechanism.coil.album.AlbumImageKeyer
import player.phonograph.mechanism.coil.album.AlbumImageMapper
import player.phonograph.mechanism.coil.artist.ArtistImageFetcher
import player.phonograph.mechanism.coil.artist.ArtistImageKeyer
import player.phonograph.mechanism.coil.artist.ArtistImageMapper
import player.phonograph.mechanism.coil.audiofile.AudioFileFetcher
import player.phonograph.mechanism.coil.audiofile.AudioFileKeyer
import player.phonograph.mechanism.coil.audiofile.AudioFileMapper
import player.phonograph.mechanism.coil.audiofile.FileEntityMapper
import player.phonograph.mechanism.coil.cache.MemeryCacheInterceptor
import player.phonograph.mechanism.coil.lastfm.LastFmImageBundleKeyer
import player.phonograph.mechanism.coil.lastfm.LastFmImageBundleMapper
import player.phonograph.mechanism.coil.palette.PaletteInterceptor
import player.phonograph.mechanism.coil.retriever.RetrieverConfigInterceptor
import android.content.Context

fun createPhonographImageLoader(context: Context): ImageLoader {
    return ImageLoader.Builder(context)
        .allowHardware(false)
        .components {
            // song files
            add(AudioFileKeyer())
            add(AudioFileMapper())
            add(FileEntityMapper())
            add(AudioFileFetcher.Factory())
            // album
            add(AlbumImageKeyer())
            add(AlbumImageMapper())
            add(AlbumImageFetcher.Factory())
            // artist
            add(ArtistImageKeyer())
            add(ArtistImageMapper())
            add(ArtistImageFetcher.Factory())
            // last.fm
            add(LastFmImageBundleKeyer())
            add(LastFmImageBundleMapper())
            // interceptors
            add(MemeryCacheInterceptor())
            add(PaletteInterceptor())
            add(RetrieverConfigInterceptor())
        }
        .build()
}
