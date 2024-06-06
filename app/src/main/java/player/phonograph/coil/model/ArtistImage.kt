/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.coil.model

import player.phonograph.repo.loader.Songs
import android.content.Context

data class ArtistImage(
    override val id: Long,
    val name: String,
) : CompositeLoaderTarget<SongImage> {

    override suspend fun items(context: Context): Iterable<SongImage> =
        Songs.artist(context, id).map { SongImage.from(it) }

    override fun toString(): String =
        "ArtistImage(name=$name, id=$id,)"
}
