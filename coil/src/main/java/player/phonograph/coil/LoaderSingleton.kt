/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.coil

import android.content.Context
import coil.ImageLoader
import player.phonograph.coil.artist.ArtistImageFetcher
import player.phonograph.coil.artist.ArtistImageMapper
import player.phonograph.coil.artist.ArtistKeyer
import player.phonograph.coil.audiofile.AudioFileFetcher
import player.phonograph.coil.audiofile.AudioFileKeyer
import player.phonograph.coil.audiofile.AudioFileMapper

fun createPhonographImageLoader(context: Context): ImageLoader {
    return ImageLoader.Builder(context)
        .allowHardware(false)
        .components {
            // song files
            add(AudioFileKeyer())
            add(AudioFileMapper())
            add(AudioFileFetcher.Factory())
            // artist
            add(ArtistKeyer())
            add(ArtistImageMapper())
            add(ArtistImageFetcher.Factory())
        }
        .crossfade(true)
        .build()
}
