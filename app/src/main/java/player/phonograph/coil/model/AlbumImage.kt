/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.coil.model

import player.phonograph.repo.loader.Songs
import android.content.Context

data class AlbumImage(
    override val id: Long,
    val name: String,
) : CompositeLoaderTarget<SongImage> {

    override suspend fun items(context: Context): Iterable<SongImage> =
        Songs.album(context, id).map { SongImage.from(it) }

    override fun toString(): String =
        "AlbumImage(name=$name, id=$id,)"
}