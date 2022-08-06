/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.coil.audiofile

import player.phonograph.model.Song

class AudioFile private constructor(val songId: Long, val path: String, val albumId: Long) {

    companion object {
        fun from(song: Song): AudioFile =
            AudioFile(song.id, song.data, song.albumId)
    }

    override fun toString(): String = "AudioFile{songId:$songId,path:$path}"

    override fun equals(other: Any?): Boolean {
        if (other == null) return false

        val o = (other as? AudioFile) ?: return false
        return o.songId == songId && o.path == path && o.albumId == albumId
    }

    override fun hashCode(): Int {
        var result = songId.hashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + albumId.hashCode()
        return result
    }
}
