/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.coil

import coil.ImageLoader
import player.phonograph.coil.album.AlbumImageFetcher
import player.phonograph.coil.album.AlbumImageMapper
import player.phonograph.coil.album.AlbumKeyer
import player.phonograph.coil.artist.ArtistImageFetcher
import player.phonograph.coil.artist.ArtistImageMapper
import player.phonograph.coil.artist.ArtistKeyer
import player.phonograph.coil.audiofile.AudioFileFetcher
import player.phonograph.coil.audiofile.AudioFileKeyer
import player.phonograph.coil.audiofile.AudioFileMapper
import player.phonograph.coil.lastfm.LastFmImageBundleKeyer
import player.phonograph.coil.lastfm.LastFmImageBundleMapper
import android.content.Context

fun createPhonographImageLoader(context: Context): ImageLoader {
    return ImageLoader.Builder(context)
        .allowHardware(false)
        .components {
            // song files
            add(AudioFileKeyer())
            add(AudioFileMapper())
            add(AudioFileFetcher.Factory())
            // album
            add(AlbumKeyer())
            add(AlbumImageMapper())
            add(AlbumImageFetcher.Factory())
            // artist
            add(ArtistKeyer())
            add(ArtistImageMapper())
            add(ArtistImageFetcher.Factory())
            // last.fm
            add(LastFmImageBundleKeyer())
            add(LastFmImageBundleMapper())
        }
        .crossfade(true)
        .build()
}
