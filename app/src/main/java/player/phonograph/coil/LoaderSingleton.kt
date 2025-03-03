/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.coil

import coil.ImageLoader
import player.phonograph.coil.album.AlbumImageFetcher
import player.phonograph.coil.album.AlbumImageKeyer
import player.phonograph.coil.album.AlbumImageMapper
import player.phonograph.coil.artist.ArtistImageFetcher
import player.phonograph.coil.artist.ArtistImageKeyer
import player.phonograph.coil.artist.ArtistImageMapper
import player.phonograph.coil.audiofile.AudioFileFetcher
import player.phonograph.coil.audiofile.AudioFileKeyer
import player.phonograph.coil.audiofile.AudioFileMapper
import player.phonograph.coil.audiofile.FileEntityMapper
import player.phonograph.coil.lastfm.LastFmImageBundleKeyer
import player.phonograph.coil.lastfm.LastFmImageBundleMapper
import player.phonograph.coil.palette.PaletteInterceptor
import android.content.Context

fun createPhonographImageLoader(context: Context): ImageLoader {
    return ImageLoader.Builder(context)
        .allowHardware(false)
        .components {
            // song files
            add(AudioFileKeyer())
            add(AudioFileMapper())
            add(FileEntityMapper())
            add(AudioFileFetcher.Factory(context))
            // album
            add(AlbumImageKeyer())
            add(AlbumImageMapper())
            add(AlbumImageFetcher.Factory(context))
            // artist
            add(ArtistImageKeyer())
            add(ArtistImageMapper())
            add(ArtistImageFetcher.Factory(context))
            // last.fm
            add(LastFmImageBundleKeyer())
            add(LastFmImageBundleMapper())
            // interceptors
            add(PaletteInterceptor())
        }
        .build()
}
