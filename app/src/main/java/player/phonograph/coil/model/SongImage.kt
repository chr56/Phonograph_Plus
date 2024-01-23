/*
 *  Copyright (c) 2022~2024 chr_56
 */

package player.phonograph.coil.model

import player.phonograph.model.Song

class SongImage private constructor(val songId: Long, val path: String, val albumId: Long) : LoaderTarget {

    override val id: Long get() = songId

    override fun toString(): String = "SongImage{songId:$songId,path:$path}"

    override fun equals(other: Any?): Boolean {
        if (other == null) return false

        val o = (other as? SongImage) ?: return false
        return o.songId == songId && o.path == path && o.albumId == albumId
    }

    override fun hashCode(): Int {
        var result = songId.hashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + albumId.hashCode()
        return result
    }

    companion object {
        fun from(song: Song): SongImage =
            SongImage(song.id, song.data, song.albumId)
    }
}
